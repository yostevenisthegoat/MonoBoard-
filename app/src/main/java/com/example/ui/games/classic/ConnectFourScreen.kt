package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.AiDifficulty
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ConnectFourScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("connect_four") }
    val rows = 6
    val cols = 7

    var grid by remember { mutableStateOf(List(rows) { List(cols) { 0 } }) } // 0=empty, 1=P1, 2=P2
    val undoStack = remember { mutableStateListOf<Pair<List<List<Int>>, Int>>() }
    var activeSeat by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    var winCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    fun findWinCells(b: List<List<Int>>): Pair<Int, Set<Pair<Int, Int>>> {
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val player = b[r][c]
                if (player == 0) continue
                for ((dr, dc) in dirs) {
                    val cells = (0..3).map { k -> (r + k * dr) to (c + k * dc) }
                    if (cells.all { (nr, nc) -> nr in 0 until rows && nc in 0 until cols && b[nr][nc] == player }) {
                        return player to cells.toSet()
                    }
                }
            }
        }
        return 0 to emptySet()
    }

    fun dropRow(b: List<List<Int>>, col: Int): Int {
        for (r in rows - 1 downTo 0) {
            if (b[r][col] == 0) return r
        }
        return -1
    }

    fun evaluateWindow(b: List<List<Int>>): Int {
        var score = 0
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                for ((dr, dc) in dirs) {
                    if (r + 3 * dr !in 0 until rows || c + 3 * dc !in 0 until cols) continue
                    var ai = 0; var hu = 0
                    for (k in 0..3) {
                        when (b[r + k * dr][c + k * dc]) {
                            2 -> ai++
                            1 -> hu++
                        }
                    }
                    if (ai > 0 && hu == 0) score += when (ai) { 4 -> 10000; 3 -> 50; 2 -> 8; else -> 1 }
                    if (hu > 0 && ai == 0) score -= when (hu) { 4 -> 10000; 3 -> 60; 2 -> 8; else -> 1 }
                }
            }
        }
        return score
    }

    fun minimax(b: List<List<Int>>, depth: Int, alphaIn: Int, betaIn: Int, maximizing: Boolean): Int {
        val (w, _) = findWinCells(b)
        if (w == 2) return 100_000 + depth
        if (w == 1) return -100_000 - depth
        val validCols = (0 until cols).filter { dropRow(b, it) != -1 }
        if (validCols.isEmpty()) return 0
        if (depth == 0) return evaluateWindow(b)
        var alpha = alphaIn; var beta = betaIn
        return if (maximizing) {
            var best = Int.MIN_VALUE
            for (col in validCols) {
                val r = dropRow(b, col)
                val next = b.map { it.toMutableList() }.apply { this[r][col] = 2 }
                best = maxOf(best, minimax(next, depth - 1, alpha, beta, false))
                alpha = maxOf(alpha, best)
                if (beta <= alpha) break
            }
            best
        } else {
            var best = Int.MAX_VALUE
            for (col in validCols) {
                val r = dropRow(b, col)
                val next = b.map { it.toMutableList() }.apply { this[r][col] = 1 }
                best = minOf(best, minimax(next, depth - 1, alpha, beta, true))
                beta = minOf(beta, best)
                if (beta <= alpha) break
            }
            best
        }
    }

    fun dropPiece(col: Int) {
        if (winnerSeat != null || isDraw) return
        val r = dropRow(grid, col)
        if (r == -1) return
        undoStack.add(grid to activeSeat)
        val next = grid.map { it.toMutableList() }
        next[r][col] = activeSeat + 1
        grid = next.map { it.toList() }
        onPlayCue(SoundCue.MOVE)

        val (w, cells) = findWinCells(grid)
        if (w != 0) {
            winCells = cells
            winnerSeat = w - 1
        } else if ((0 until cols).all { dropRow(grid, it) == -1 }) {
            isDraw = true
        } else {
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
            val validCols = (0 until cols).filter { dropRow(grid, it) != -1 }
            if (validCols.isNotEmpty()) {
                val chosen = withContext(Dispatchers.Default) {
                    if (sessionConfig.aiDifficulty == AiDifficulty.EASY) {
                        validCols.random()
                    } else {
                        val depth = if (sessionConfig.aiDifficulty == AiDifficulty.MEDIUM) 2 else 4
                        validCols.sortedBy { kotlin.math.abs(it - 3) }.maxBy { col ->
                            val r = dropRow(grid, col)
                            val next = grid.map { it.toMutableList() }.apply { this[r][col] = 2 }
                            minimax(next, depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, false)
                        }
                    }
                }
                dropPiece(chosen)
            }
        }
    }

    LaunchedEffect(winnerSeat, isDraw) {
        if ((winnerSeat != null || isDraw) && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            if (isDraw) {
                onRecordOutcome(MatchOutcome.DRAW, "Draw", "Connect Four Draw", 50, elapsed)
            } else {
                val p = sessionConfig.players[winnerSeat!!]
                val outcome = if (p.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
                onRecordOutcome(outcome, p.name, "${p.name} connected four!", 100, elapsed)
            }
        }
    }

    fun restart() {
        grid = List(rows) { List(cols) { 0 } }
        undoStack.clear()
        winCells = emptySet()
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
        canUndo = undoStack.isNotEmpty() && !isAiTurn,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty() && !isAiTurn) {
                val prev = undoStack.removeAt(undoStack.lastIndex)
                grid = prev.first
                activeSeat = prev.second
                winCells = emptySet()
                winnerSeat = null
                isDraw = false
                recorded = false
            }
        },
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat
        )
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardW = minOf(maxWidth, maxHeight * (7f / 6f))
            Surface(
                modifier = Modifier
                    .size(width = boardW, height = boardW * (6f / 7f)),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until cols) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isAiTurn && winnerSeat == null && !isDraw) {
                                    dropPiece(c)
                                },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (r in 0 until rows) {
                                val cell = grid[r][c]
                                val isWinCell = (r to c) in winCells
                                val discColor = when (cell) {
                                    1 -> Color(0xFFF5F5F5)
                                    2 -> Color(0xFF555555)
                                    else -> MaterialTheme.colorScheme.background
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(discColor)
                                        .border(
                                            width = if (isWinCell) 3.dp else 1.dp,
                                            color = if (isWinCell) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (winnerSeat != null || isDraw) {
        GameCompletionDialog(
            title = if (isDraw) "Board Full — Draw" else "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = if (isDraw) "No more columns available." else "Four discs connected!",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
