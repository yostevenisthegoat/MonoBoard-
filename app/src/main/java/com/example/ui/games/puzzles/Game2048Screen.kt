package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.StatMetricTile
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class Dir2048 { UP, DOWN, LEFT, RIGHT }

@Composable
fun Game2048Screen(
    bestRecordedScore: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("game_2048") }

    fun spawnTile(b: List<List<Int>>): List<List<Int>> {
        val empties = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) for (c in 0..3) if (b[r][c] == 0) empties.add(r to c)
        if (empties.isEmpty()) return b
        val (r, c) = empties.random()
        val next = b.map { it.toMutableList() }
        next[r][c] = if (Math.random() < 0.9) 2 else 4
        return next.map { it.toList() }
    }

    fun initialBoard(): List<List<Int>> = spawnTile(spawnTile(List(4) { List(4) { 0 } }))

    var board by remember { mutableStateOf(initialBoard()) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember(bestRecordedScore) { mutableIntStateOf(bestRecordedScore) }
    val undoStack = remember { mutableStateListOf<Pair<List<List<Int>>, Int>>() }
    var wonAcknowledged by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(1000L)
            elapsed++
        }
    }

    fun compressLine(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }.toMutableList()
        val merged = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val v = nonZero[i] * 2
                merged.add(v)
                gained += v
                i += 2
            } else {
                merged.add(nonZero[i])
                i++
            }
        }
        while (merged.size < 4) merged.add(0)
        return merged to gained
    }

    fun hasMoves(b: List<List<Int>>): Boolean {
        for (r in 0..3) for (c in 0..3) {
            if (b[r][c] == 0) return true
            if (r < 3 && b[r][c] == b[r + 1][c]) return true
            if (c < 3 && b[r][c] == b[r][c + 1]) return true
        }
        return false
    }

    fun slide(dir: Dir2048) {
        if (gameOver) return
        val next = MutableList(4) { MutableList(4) { 0 } }
        var gained = 0
        when (dir) {
            Dir2048.LEFT -> for (r in 0..3) {
                val (line, g) = compressLine(board[r])
                next[r] = line.toMutableList(); gained += g
            }
            Dir2048.RIGHT -> for (r in 0..3) {
                val (line, g) = compressLine(board[r].reversed())
                next[r] = line.reversed().toMutableList(); gained += g
            }
            Dir2048.UP -> for (c in 0..3) {
                val col = (0..3).map { r -> board[r][c] }
                val (line, g) = compressLine(col)
                for (r in 0..3) next[r][c] = line[r]; gained += g
            }
            Dir2048.DOWN -> for (c in 0..3) {
                val col = (3 downTo 0).map { r -> board[r][c] }
                val (line, g) = compressLine(col)
                val rev = line.reversed()
                for (r in 0..3) next[r][c] = rev[r]; gained += g
            }
        }
        val candidate = next.map { it.toList() }
        if (candidate != board) {
            undoStack.add(board to score)
            board = spawnTile(candidate)
            score += gained
            bestScore = maxOf(bestScore, score)
            onPlayCue(if (gained > 0) SoundCue.TURN_PASS else SoundCue.MOVE)
            if (!hasMoves(board)) {
                gameOver = true
            }
        }
    }

    LaunchedEffect(gameOver) {
        if (gameOver && !recorded) {
            recorded = true
            val maxTile = board.flatten().maxOrNull() ?: 0
            val outcome = if (maxTile >= 2048) MatchOutcome.WIN else MatchOutcome.COMPLETE
            onRecordOutcome(outcome, "Player 1", "Score $score • Max tile $maxTile", score, elapsed)
        }
    }

    fun restart() {
        board = initialBoard()
        score = 0
        undoStack.clear()
        wonAcknowledged = false
        gameOver = false
        elapsed = 0
        recorded = false
    }

    val reached2048 = !wonAcknowledged && board.flatten().any { it >= 2048 }

    GameScaffold(
        game = game,
        subtitle = "Swipe or use arrows",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty(),
        onBack = {
            if (!recorded && score > 0) {
                val maxTile = board.flatten().maxOrNull() ?: 0
                onRecordOutcome(MatchOutcome.COMPLETE, "Player 1", "Score $score • Max tile $maxTile", score, elapsed)
            }
            onBack()
        },
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty()) {
                val prev = undoStack.removeAt(undoStack.lastIndex)
                board = prev.first
                score = prev.second
                gameOver = false
            }
        },
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "SCORE", value = score.toString(), modifier = Modifier.weight(1f))
            StatMetricTile(label = "BEST", value = bestScore.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        var dragX = 0f
        var dragY = 0f
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragX = 0f; dragY = 0f },
                        onDrag = { _, dragAmount -> dragX += dragAmount.x; dragY += dragAmount.y },
                        onDragEnd = {
                            if (abs(dragX) > abs(dragY) && abs(dragX) > 36f) {
                                slide(if (dragX > 0) Dir2048.RIGHT else Dir2048.LEFT)
                            } else if (abs(dragY) > 36f) {
                                slide(if (dragY > 0) Dir2048.DOWN else Dir2048.UP)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight, 360.dp)
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0..3) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0..3) {
                                val v = board[r][c]
                                val (bg, fg) = tileColors2048(v)
                                Surface(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (v > 0) {
                                            Text(
                                                text = v.toString(),
                                                fontSize = when {
                                                    v >= 1024 -> 20.sp
                                                    v >= 128 -> 24.sp
                                                    else -> 28.sp
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = fg
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

        // Accessible Directional Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { slide(Dir2048.LEFT) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Slide Left")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { slide(Dir2048.UP) }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Slide Up")
                }
                IconButton(onClick = { slide(Dir2048.DOWN) }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Slide Down")
                }
            }
            IconButton(onClick = { slide(Dir2048.RIGHT) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Slide Right")
            }
        }
    }

    if (reached2048) {
        GameCompletionDialog(
            title = "2048 Reached!",
            subtitle = "You forged the 2048 tile with score $score. Keep pushing or start fresh.",
            onPlayAgain = { wonAcknowledged = true },
            onExit = onBack
        )
    } else if (gameOver) {
        GameCompletionDialog(
            title = "No Moves Left",
            subtitle = "Final score: $score",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}

private fun tileColors2048(value: Int): Pair<Color, Color> = when (value) {
    0 -> Color(0xFF141414) to Color.Transparent
    2 -> Color(0xFF232323) to Color(0xFFE0E0E0)
    4 -> Color(0xFF2E2E2E) to Color(0xFFFFFFFF)
    8 -> Color(0xFF424242) to Color(0xFFFFFFFF)
    16 -> Color(0xFF5A5A5A) to Color(0xFFFFFFFF)
    32 -> Color(0xFF757575) to Color(0xFFFFFFFF)
    64 -> Color(0xFF9E9E9E) to Color(0xFF080808)
    128 -> Color(0xFFBDBDBD) to Color(0xFF080808)
    256 -> Color(0xFFD6D6D6) to Color(0xFF080808)
    512 -> Color(0xFFEAEAEA) to Color(0xFF080808)
    else -> Color(0xFFFFFFFF) to Color(0xFF080808)
}
