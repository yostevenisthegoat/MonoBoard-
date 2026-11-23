package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.bouncyPress
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun NonogramScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("nonogram") }
    val dim = when {
        sessionConfig.ruleVariant.contains("10") -> 10
        sessionConfig.ruleVariant.contains("8") -> 8
        else -> 5
    }

    fun generateTarget(): List<List<Boolean>> {
        return List(dim) { List(dim) { Math.random() < 0.56 } }.let { g ->
            // Ensure no all-empty puzzle
            if (g.flatten().none { it }) List(dim) { r -> List(dim) { c -> (r + c) % 2 == 0 } } else g
        }
    }

    fun cluesForLine(line: List<Boolean>): List<Int> {
        val runs = mutableListOf<Int>()
        var run = 0
        line.forEach { filled ->
            if (filled) run++ else if (run > 0) { runs.add(run); run = 0 }
        }
        if (run > 0) runs.add(run)
        return if (runs.isEmpty()) listOf(0) else runs
    }

    var target by remember(dim) { mutableStateOf(generateTarget()) }
    // 0=blank, 1=filled, 2=cross-marked
    var grid by remember(target) { mutableStateOf(List(dim) { List(dim) { 0 } }) }
    var crossMode by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val rowClues = remember(target) { target.map { cluesForLine(it) } }
    val colClues = remember(target) { (0 until dim).map { c -> cluesForLine((0 until dim).map { r -> target[r][c] }) } }

    val solved = remember(grid, target) {
        (0 until dim).all { r ->
            (0 until dim).all { c -> (grid[r][c] == 1) == target[r][c] }
        }
    }

    LaunchedEffect(solved) {
        while (!solved) {
            delay(1000L)
            elapsed++
        }
    }

    LaunchedEffect(solved) {
        if (solved && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Solved ${dim}×${dim} Nonogram", dim * 25, elapsed)
        }
    }

    fun restart() {
        target = generateTarget(); elapsed = 0; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${dim}×${dim} Picross",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%02d:%02d".format(elapsed / 60, elapsed % 60),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
            )
            OutlinedButton(
                onClick = {
                    crossMode = !crossMode
                    onPlayCue(SoundCue.KEY_TICK)
                },
                modifier = Modifier.bouncyPress(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (crossMode) "Mode: Mark ×" else "Mode: Fill ■")
            }
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
                // Column clues row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.width(46.dp))
                    for (c in 0 until dim) {
                        Column(
                            modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            colClues[c].forEach { num ->
                                Text(
                                    text = num.toString(),
                                    fontSize = if (dim <= 5) 12.sp else 9.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Rows with row clues + cells
                for (r in 0 until dim) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.width(46.dp).padding(end = 6.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = rowClues[r].joinToString(" "),
                                fontSize = if (dim <= 5) 12.sp else 9.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        for (c in 0 until dim) {
                            val cell = grid[r][c]
                            val cellBg by animateColorAsState(
                                targetValue = when (cell) {
                                    1 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                animationSpec = tween(140),
                                label = "nono_cell_${r}_$c"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .springPop(cell, popScale = 1.15f)
                                    .bouncyPress(!solved)
                                    .background(cellBg, RoundedCornerShape(3.dp))
                                    .border(0.6.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                                    .clickable(enabled = !solved) {
                                        val next = grid.map { it.toMutableList() }
                                        val targetVal = if (crossMode) 2 else 1
                                        val newVal = if (next[r][c] == targetVal) 0 else targetVal
                                        next[r][c] = newVal
                                        val updated = next.map { it.toList() }
                                        grid = updated
                                        val rowSolved = (0 until dim).all { col -> (updated[r][col] == 1) == target[r][col] }
                                        val colSolved = (0 until dim).all { row -> (updated[row][c] == 1) == target[row][c] }
                                        onPlayCue(
                                            when {
                                                newVal == 1 && (rowSolved || colSolved) -> SoundCue.TILE_MERGE
                                                newVal == 1 -> SoundCue.WOOD_TAP
                                                else -> SoundCue.KEY_TICK
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell == 2) {
                                    Text("×", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (solved) {
        GameCompletionDialog(
            title = "Nonogram Complete!",
            subtitle = "Solved in %02d:%02d.".format(elapsed / 60, elapsed % 60),
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
