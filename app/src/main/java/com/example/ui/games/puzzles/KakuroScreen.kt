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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily

// Dynamic valid Kakuro puzzle generator on a 5x5 block-and-run grid
private data class KakuroBoard(
    val solution: List<List<Int>>, // 0 = blocked/clue cell, 1..9 = solution digit
    val acrossClues: Map<Pair<Int, Int>, Int>, // clue cell -> sum right
    val downClues: Map<Pair<Int, Int>, Int> // clue cell -> sum down
)

@Composable
fun KakuroScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("kakuro") }

    fun generateKakuro(): KakuroBoard {
        // 5x5 template: row 0 & col 0 are clue headers, (4,1) and (1,4) blocked for variety
        // All open runs have unique digits in 1..9!
        val sol = MutableList(5) { MutableList(5) { 0 } }
        val baseRows = (1..9).shuffled()
        // Latin 3x3 subgrid + extra cells with unique digits per run
        val rows = listOf(1, 2, 3, 4).shuffled().take(4)
        for (r in 1..4) {
            val used = mutableSetOf<Int>()
            for (c in 1..4) {
                if ((r == 1 && c == 4) || (r == 4 && c == 1)) continue
                val colUsed = (1 until r).map { sol[it][c] }.toSet()
                val pick = (1..9).shuffled().first { it !in used && it !in colUsed }
                sol[r][c] = pick
                used.add(pick)
            }
        }
        val across = mutableMapOf<Pair<Int, Int>, Int>()
        val down = mutableMapOf<Pair<Int, Int>, Int>()
        for (r in 1..4) {
            var sum = 0
            for (c in 1..4) sum += sol[r][c]
            across[r to 0] = sum
        }
        for (c in 1..4) {
            var sum = 0
            for (r in 1..4) sum += sol[r][c]
            down[0 to c] = sum
        }
        return KakuroBoard(sol.map { it.toList() }, across, down)
    }

    var puzzle by remember { mutableStateOf(generateKakuro()) }
    var entries by remember(puzzle) { mutableStateOf(List(5) { List(5) { 0 } }) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(1 to 1) }
    var recorded by remember { mutableStateOf(false) }
    var conflictShakeCount by remember { mutableIntStateOf(0) }

    // Valid if every fillable cell has 1..9, unique in its row & col run, and matches every clue sum
    val solved = remember(entries, puzzle) {
        val allFilled = (1..4).all { r -> (1..4).all { c -> puzzle.solution[r][c] == 0 || entries[r][c] in 1..9 } }
        if (!allFilled) false
        else {
            val rowsOk = (1..4).all { r ->
                val vals = (1..4).filter { c -> puzzle.solution[r][c] != 0 }.map { c -> entries[r][c] }
                vals.distinct().size == vals.size && vals.sum() == puzzle.acrossClues[r to 0]
            }
            val colsOk = (1..4).all { c ->
                val vals = (1..4).filter { r -> puzzle.solution[r][c] != 0 }.map { r -> entries[r][c] }
                vals.distinct().size == vals.size && vals.sum() == puzzle.downClues[0 to c]
            }
            rowsOk && colsOk
        }
    }

    LaunchedEffect(solved) {
        if (solved && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Solved 5×5 Kakuro", 150, 60)
        }
    }

    fun restart() {
        puzzle = generateKakuro(); selected = 1 to 1; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "Unique digits 1–9 per sum run",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).shakeOnTrigger(conflictShakeCount),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                for (r in 0..4) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0..4) {
                            val isFillable = r in 1..4 && c in 1..4 && puzzle.solution[r][c] != 0
                            val aClue = puzzle.acrossClues[r to c]
                            val dClue = puzzle.downClues[r to c]
                            val isSel = selected == (r to c)
                            val vCell = entries[r][c]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .springPop(if (isFillable && vCell != 0) vCell else null, popScale = 1.16f)
                                    .pulseEffect(isSel && !solved, maxScale = 1.06f)
                                    .background(
                                        when {
                                            !isFillable -> MaterialTheme.colorScheme.background
                                            isSel -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable(enabled = isFillable && !solved) {
                                        selected = r to c
                                        onPlayCue(SoundCue.KEY_TICK)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFillable) {
                                    val v = entries[r][c]
                                    Text(
                                        text = if (v == 0) "" else v.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                } else if (aClue != null || dClue != null) {
                                    Text(
                                        text = aClue?.let { "→$it" } ?: "↓$dClue",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..9).forEach { d ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bouncyPress(selected != null && !solved)
                        .clickable(enabled = selected != null && !solved) {
                            val (sr, sc) = selected!!
                            if (puzzle.solution[sr][sc] != 0) {
                                val next = entries.map { it.toMutableList() }
                                val newVal = if (next[sr][sc] == d) 0 else d
                                next[sr][sc] = newVal
                                val updated = next.map { it.toList() }
                                entries = updated
                                val rowDup = newVal != 0 && (1..4).any { c -> c != sc && puzzle.solution[sr][c] != 0 && updated[sr][c] == newVal }
                                val colDup = newVal != 0 && (1..4).any { r -> r != sr && puzzle.solution[r][sc] != 0 && updated[r][sc] == newVal }
                                val rowRunVals = (1..4).filter { c -> puzzle.solution[sr][c] != 0 }.map { c -> updated[sr][c] }
                                val colRunVals = (1..4).filter { r -> puzzle.solution[r][sc] != 0 }.map { r -> updated[r][sc] }
                                val rowSolved = rowRunVals.none { it == 0 } && rowRunVals.distinct().size == rowRunVals.size && rowRunVals.sum() == puzzle.acrossClues[sr to 0]
                                val colSolved = colRunVals.none { it == 0 } && colRunVals.distinct().size == colRunVals.size && colRunVals.sum() == puzzle.downClues[0 to sc]
                                if (rowDup || colDup) {
                                    conflictShakeCount++
                                    onPlayCue(SoundCue.ERROR)
                                } else if (newVal != 0 && (rowSolved || colSolved)) {
                                    onPlayCue(SoundCue.TILE_MERGE)
                                } else {
                                    onPlayCue(SoundCue.KEY_TICK)
                                }
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(d.toString(), fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (solved) {
        GameCompletionDialog(
            title = "Kakuro Solved!",
            subtitle = "All horizontal and vertical sums match with unique digits.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
