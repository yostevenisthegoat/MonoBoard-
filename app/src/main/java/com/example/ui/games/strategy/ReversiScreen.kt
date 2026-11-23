package com.example.ui.games.strategy

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AppSettings
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.AiDifficulty
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.pulseEffect
import com.example.ui.components.springPop
import kotlinx.coroutines.delay

@Composable
fun ReversiScreen(
    sessionConfig: GameSessionConfig,
    settings: AppSettings,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("reversi") }

    fun initialBoard(): List<List<Int>> {
        val b = MutableList(8) { MutableList(8) { 0 } } // 1=Dark(P1), 2=Light(P2)
        b[3][3] = 2; b[3][4] = 1
        b[4][3] = 1; b[4][4] = 2
        return b.map { it.toList() }
    }

    var board by remember { mutableStateOf(initialBoard()) }
    var activeSeat by remember { mutableIntStateOf(0) } // 0=1, 1=2
    val undoStack = remember { mutableStateListOf<Pair<List<List<Int>>, Int>>() }
    var statusText by remember { mutableStateOf("Tap a highlighted square to flip discs.") }
    var gameOver by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val dirs = remember {
        listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    }

    fun flipsForMove(b: List<List<Int>>, r: Int, c: Int, player: Int): List<Pair<Int, Int>> {
        if (b[r][c] != 0) return emptyList()
        val opp = if (player == 1) 2 else 1
        val total = mutableListOf<Pair<Int, Int>>()
        for ((dr, dc) in dirs) {
            val line = mutableListOf<Pair<Int, Int>>()
            var nr = r + dr; var nc = c + dc
            while (nr in 0..7 && nc in 0..7 && b[nr][nc] == opp) {
                line.add(nr to nc)
                nr += dr; nc += dc
            }
            if (line.isNotEmpty() && nr in 0..7 && nc in 0..7 && b[nr][nc] == player) {
                total.addAll(line)
            }
        }
        return total
    }

    fun validMoves(b: List<List<Int>>, player: Int): Map<Pair<Int, Int>, List<Pair<Int, Int>>> {
        val map = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>>()
        for (r in 0..7) for (c in 0..7) {
            val f = flipsForMove(b, r, c, player)
            if (f.isNotEmpty()) map[r to c] = f
        }
        return map
    }

    val currentValid = remember(board, activeSeat) { validMoves(board, activeSeat + 1) }

    fun applyMove(r: Int, c: Int) {
        val flips = currentValid[r to c] ?: return
        undoStack.add(board to activeSeat)
        val next = board.map { it.toMutableList() }
        val p = activeSeat + 1
        next[r][c] = p
        flips.forEach { (fr, fc) -> next[fr][fc] = p }
        val updated = next.map { it.toList() }
        board = updated
        onPlayCue(if (flips.size >= 3) SoundCue.CAPTURE else SoundCue.WOOD_TAP)

        val opp = 1 - activeSeat
        val oppMoves = validMoves(updated, opp + 1)
        if (oppMoves.isNotEmpty()) {
            activeSeat = opp
            statusText = "${sessionConfig.players[activeSeat].name}'s turn"
        } else if (validMoves(updated, activeSeat + 1).isNotEmpty()) {
            statusText = "${sessionConfig.players[opp].name} has no moves — turn passes!"
        } else {
            gameOver = true
        }
    }

    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(1000L)
            elapsed++
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && !gameOver
    LaunchedEffect(board, activeSeat, isAiTurn) {
        if (isAiTurn && currentValid.isNotEmpty()) {
            delay(320L)
            val weights = arrayOf(
                intArrayOf(100, -20, 10, 5, 5, 10, -20, 100),
                intArrayOf(-20, -40, -2, -2, -2, -2, -40, -20),
                intArrayOf(10, -2, 5, 1, 1, 5, -2, 10),
                intArrayOf(5, -2, 1, 1, 1, 1, -2, 5)
            )
            val pick = if (sessionConfig.aiDifficulty == AiDifficulty.EASY) {
                currentValid.keys.random()
            } else {
                currentValid.entries.maxBy { (pos, flips) ->
                    val wr = if (pos.first < 4) pos.first else 7 - pos.first
                    val wc = if (pos.second < 4) pos.second else 7 - pos.second
                    weights[wr][wc] + flips.size * 3
                }.key
            }
            applyMove(pick.first, pick.second)
        }
    }

    val darkCount = board.sumOf { row -> row.count { it == 1 } }
    val lightCount = board.sumOf { row -> row.count { it == 2 } }

    LaunchedEffect(gameOver) {
        if (gameOver && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val p1 = sessionConfig.players[0]
            val p2 = sessionConfig.players[1]
            when {
                darkCount > lightCount -> onRecordOutcome(MatchOutcome.WIN, p1.name, "Won $darkCount–$lightCount", darkCount * 10, elapsed)
                lightCount > darkCount -> onRecordOutcome(if (p2.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, p2.name, "Won $lightCount–$darkCount", lightCount * 10, elapsed)
                else -> onRecordOutcome(MatchOutcome.DRAW, "Draw", "Tied $darkCount–$lightCount", darkCount * 10, elapsed)
            }
        }
    }

    fun restart() {
        board = initialBoard()
        activeSeat = 0
        undoStack.clear()
        gameOver = false
        elapsed = 0
        recorded = false
        statusText = "Tap a highlighted square to flip discs."
    }

    GameScaffold(
        game = game,
        subtitle = if (sessionConfig.playMode == PlayMode.VS_AI) "vs ${sessionConfig.aiDifficulty.label} AI" else "Pass & Play",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty() && !isAiTurn,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty() && !isAiTurn) {
                val prev = undoStack.removeAt(undoStack.lastIndex)
                board = prev.first
                activeSeat = prev.second
                gameOver = false
                onPlayCue(SoundCue.KEY_TICK)
            }
        },
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = listOf(darkCount, lightCount),
            statusMessage = statusText
        )
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(10.dp),
                color = settings.boardTheme.boardFrame,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    for (r in 0..7) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..7) {
                                val cell = board[r][c]
                                val isValid = (r to c) in currentValid
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(if ((r + c) % 2 == 0) settings.boardTheme.darkSquare else settings.boardTheme.darkSquare.copy(alpha = 0.88f))
                                        .border(0.5.dp, Color.Black.copy(alpha = 0.4f))
                                        .clickable(enabled = isValid && !isAiTurn && !gameOver) {
                                            applyMove(r, c)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cell != 0) {
                                        val density = LocalDensity.current.density
                                        val flipRot by animateFloatAsState(
                                            targetValue = if (cell == 1) 0f else 180f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "reversi_flip_${r}_$c"
                                        )
                                        val faceDark = flipRot <= 90f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.76f)
                                                .springPop(cell, popScale = 1.15f)
                                                .graphicsLayer {
                                                    rotationY = flipRot
                                                    cameraDistance = 10f * density
                                                }
                                                .clip(CircleShape)
                                                .background(if (faceDark) Color(0xFF141414) else Color(0xFFF5F5F5))
                                                .border(1.5.dp, Color.Gray, CircleShape)
                                        )
                                    } else if (isValid && settings.showLegalMoves) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .pulseEffect(active = !isAiTurn && !gameOver, maxScale = 1.24f)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.55f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (gameOver) {
        val title = when {
            darkCount > lightCount -> "${sessionConfig.players[0].name} Wins!"
            lightCount > darkCount -> "${sessionConfig.players[1].name} Wins!"
            else -> "Draw!"
        }
        GameCompletionDialog(
            title = title,
            subtitle = "Final discs: $darkCount – $lightCount",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
