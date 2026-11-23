package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.AiDifficulty
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.springPop
import kotlinx.coroutines.delay

@Composable
fun TicTacToeScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("tic_tac_toe") }
    val board = remember { mutableStateListOf<String>().apply { repeat(9) { add("") } } }
    var activeSeat by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    var scores = remember { mutableStateListOf(0, 0) }
    var winLine by remember { mutableStateOf<List<Int>>(emptyList()) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val lines = remember {
        listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
    }

    fun checkWinner(b: List<String>): Pair<String?, List<Int>> {
        for (line in lines) {
            val a = b[line[0]]
            if (a.isNotEmpty() && a == b[line[1]] && a == b[line[2]]) {
                return a to line
            }
        }
        return null to emptyList()
    }

    fun minimax(b: MutableList<String>, isAiMax: Boolean, depth: Int): Int {
        val (w, _) = checkWinner(b)
        if (w == "O") return 10 - depth
        if (w == "X") return depth - 10
        if (b.none { it.isEmpty() }) return 0
        var best = if (isAiMax) Int.MIN_VALUE else Int.MAX_VALUE
        for (i in 0..8) {
            if (b[i].isEmpty()) {
                b[i] = if (isAiMax) "O" else "X"
                val sc = minimax(b, !isAiMax, depth + 1)
                b[i] = ""
                best = if (isAiMax) maxOf(best, sc) else minOf(best, sc)
            }
        }
        return best
    }

    fun playMove(idx: Int) {
        if (board[idx].isNotEmpty() || winnerSeat != null || isDraw) return
        val symbol = if (activeSeat == 0) "X" else "O"
        board[idx] = symbol
        val (w, line) = checkWinner(board)
        if (w != null) {
            winLine = line
            winnerSeat = activeSeat
            scores[activeSeat]++
            onPlayCue(SoundCue.TILE_MERGE)
        } else if (board.none { it.isEmpty() }) {
            isDraw = true
            onPlayCue(SoundCue.WOOD_TAP)
        } else {
            onPlayCue(SoundCue.WOOD_TAP)
            activeSeat = 1 - activeSeat
        }
    }

    LaunchedEffect(winnerSeat, isDraw) {
        while (winnerSeat == null && !isDraw) {
            delay(1000L)
            elapsed++
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && winnerSeat == null && !isDraw
    LaunchedEffect(activeSeat, isAiTurn) {
        if (isAiTurn) {
            delay(280L)
            val empties = (0..8).filter { board[it].isEmpty() }
            if (empties.isNotEmpty()) {
                val move = when (sessionConfig.aiDifficulty) {
                    AiDifficulty.EASY -> empties.random()
                    AiDifficulty.MEDIUM -> if (Math.random() < 0.5) empties.random() else {
                        val copy = board.toMutableList()
                        empties.maxBy { i ->
                            copy[i] = "O"
                            val sc = minimax(copy, false, 0)
                            copy[i] = ""
                            sc
                        }
                    }
                    else -> {
                        val copy = board.toMutableList()
                        empties.maxBy { i ->
                            copy[i] = "O"
                            val sc = minimax(copy, false, 0)
                            copy[i] = ""
                            sc
                        }
                    }
                }
                playMove(move)
            }
        }
    }

    LaunchedEffect(winnerSeat, isDraw) {
        if ((winnerSeat != null || isDraw) && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            if (isDraw) {
                onRecordOutcome(MatchOutcome.DRAW, "Draw", "Tic-Tac-Toe Draw", 50, elapsed)
            } else {
                val p = sessionConfig.players[winnerSeat!!]
                val outcome = if (p.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
                onRecordOutcome(outcome, p.name, "${p.name} aligned three in a row", 100, elapsed)
            }
        }
    }

    fun restart() {
        for (i in 0..8) board[i] = ""
        winLine = emptyList()
        winnerSeat = null
        isDraw = false
        activeSeat = sessionConfig.startingPlayerIndex
        elapsed = 0
        recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = if (sessionConfig.playMode == PlayMode.VS_AI) "vs ${sessionConfig.aiDifficulty.label} AI" else "Pass & Play",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = scores.toList()
        )
        Spacer(Modifier.height(24.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight, 360.dp)
            Box(modifier = Modifier.size(size)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (r in 0..2) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (c in 0..2) {
                                val idx = r * 3 + c
                                val isWinning = idx in winLine
                                val cellSymbol = board[idx]
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .bouncyPress(enabled = !isAiTurn && cellSymbol.isEmpty() && winnerSeat == null && !isDraw)
                                        .pulseEffect(active = isWinning, maxScale = 1.06f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = !isAiTurn && cellSymbol.isEmpty() && winnerSeat == null && !isDraw) {
                                            playMove(idx)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isWinning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, if (isWinning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cellSymbol,
                                            fontSize = 46.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWinning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.springPop(cellSymbol.ifEmpty { null }, popScale = 1.32f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (winLine.size == 3) {
                    val strikeProgress = remember(winLine) { Animatable(0f) }
                    val strikeColor = MaterialTheme.colorScheme.onSurface
                    LaunchedEffect(winLine) {
                        strikeProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val a = winLine.first()
                        val b = winLine.last()
                        val start = Offset(((a % 3) + 0.5f) * (this.size.width / 3f), ((a / 3) + 0.5f) * (this.size.height / 3f))
                        val end = Offset(((b % 3) + 0.5f) * (this.size.width / 3f), ((b / 3) + 0.5f) * (this.size.height / 3f))
                        val cur = Offset(
                            start.x + (end.x - start.x) * strikeProgress.value,
                            start.y + (end.y - start.y) * strikeProgress.value
                        )
                        drawLine(
                            color = strikeColor.copy(alpha = 0.7f),
                            start = start,
                            end = cur,
                            strokeWidth = 7.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }

    if (winnerSeat != null || isDraw) {
        GameCompletionDialog(
            title = if (isDraw) "Match Draw" else "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = if (isDraw) "All 9 squares filled with no line." else "Three in a row!",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
