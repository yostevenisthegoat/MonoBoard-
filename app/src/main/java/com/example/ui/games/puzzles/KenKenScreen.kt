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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.abs

private data class KenCage(
    val id: Int,
    val cells: List<Pair<Int, Int>>,
    val label: String,
    val op: Char,
    val target: Int
)

private data class KenPuzzle(
    val dim: Int,
    val solution: List<List<Int>>,
    val cages: List<KenCage>
)

@Composable
fun KenKenScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("kenken") }
    val dim = if (sessionConfig.ruleVariant.contains("5")) 5 else 4

    fun generatePuzzle(): KenPuzzle {
        val permRows = (0 until dim).shuffled()
        val permCols = (0 until dim).shuffled()
        val sol = List(dim) { r ->
            List(dim) { c ->
                ((permRows[r] + permCols[c]) % dim) + 1
            }
        }
        val visited = Array(dim) { BooleanArray(dim) }
        val cages = mutableListOf<KenCage>()
        var cid = 0
        for (r in 0 until dim) for (c in 0 until dim) {
            if (visited[r][c]) continue
            visited[r][c] = true
            val neighbors = listOf(r to (c + 1), (r + 1) to c).filter { (nr, nc) ->
                nr in 0 until dim && nc in 0 until dim && !visited[nr][nc]
            }
            if (neighbors.isNotEmpty() && Math.random() < 0.78) {
                val n = neighbors.random()
                visited[n.first][n.second] = true
                val a = sol[r][c]; val b = sol[n.first][n.second]
                val hi = maxOf(a, b); val lo = minOf(a, b)
                val (op, target) = when {
                    hi % lo == 0 && Math.random() < 0.35 -> '÷' to (hi / lo)
                    Math.random() < 0.4 -> '−' to (hi - lo)
                    Math.random() < 0.5 -> '×' to (a * b)
                    else -> '+' to (a + b)
                }
                cages.add(KenCage(cid++, listOf(r to c, n), "$target$op", op, target))
            } else {
                val v = sol[r][c]
                cages.add(KenCage(cid++, listOf(r to c), v.toString(), '=', v))
            }
        }
        return KenPuzzle(dim, sol, cages)
    }

    var puzzle by remember(dim) { mutableStateOf(generatePuzzle()) }
    var entries by remember(puzzle) { mutableStateOf(List(dim) { List(dim) { 0 } }) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(0 to 0) }
    var recorded by remember { mutableStateOf(false) }

    val cageByCell = remember(puzzle) {
        val m = mutableMapOf<Pair<Int, Int>, KenCage>()
        puzzle.cages.forEach { cage -> cage.cells.forEach { m[it] = cage } }
        m
    }
    val labelCells = remember(puzzle) {
        puzzle.cages.associate { cage ->
            val top = cage.cells.minWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            top to cage.label
        }
    }

    val solved = remember(entries, puzzle) {
        val latinOk = (0 until dim).all { r ->
            entries[r].toSet() == (1..dim).toSet()
        } && (0 until dim).all { c ->
            (0 until dim).map { r -> entries[r][c] }.toSet() == (1..dim).toSet()
        }
        if (!latinOk) false
        else puzzle.cages.all { cage ->
            val vals = cage.cells.map { (r, c) -> entries[r][c] }
            when (cage.op) {
                '=' -> vals[0] == cage.target
                '+' -> vals.sum() == cage.target
                '×' -> vals.fold(1) { acc, v -> acc * v } == cage.target
                '−' -> abs(vals[0] - vals[1]) == cage.target
                '÷' -> maxOf(vals[0], vals[1]) / minOf(vals[0], vals[1]) == cage.target && maxOf(vals[0], vals[1]) % minOf(vals[0], vals[1]) == 0
                else -> false
            }
        }
    }

    LaunchedEffect(solved) {
        if (solved && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Solved ${dim}×${dim} KenKen", dim * 35, 70)
        }
    }

    fun restart() {
        puzzle = generatePuzzle(); selected = 0 to 0; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${dim}×${dim} Arithmetic Grid",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                for (r in 0 until dim) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until dim) {
                            val cage = cageByCell[r to c]
                            val isSel = selected == (r to c)
                            val label = labelCells[r to c]
                            val bg = when {
                                isSel -> MaterialTheme.colorScheme.primary
                                (cage?.id ?: 0) % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.background
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .background(bg, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !solved) { selected = r to c },
                                contentAlignment = Alignment.Center
                            ) {
                                if (label != null) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    )
                                }
                                val v = entries[r][c]
                                if (v != 0) {
                                    Text(
                                        text = v.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..dim).forEach { d ->
                Surface(
                    modifier = Modifier.weight(1f).height(52.dp).clickable(enabled = selected != null && !solved) {
                        val (sr, sc) = selected!!
                        val next = entries.map { it.toMutableList() }
                        next[sr][sc] = if (next[sr][sc] == d) 0 else d
                        entries = next.map { it.toList() }
                        onPlayCue(SoundCue.MOVE)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(d.toString(), fontSize = 20.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (solved) {
        GameCompletionDialog(
            title = "KenKen Solved!",
            subtitle = "All arithmetic cages and Latin rows/columns verified.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
