package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

private val wordSearchPacks = listOf(
    listOf("CHESS", "KNIGHT", "BISHOP", "ROOK", "CASTLE", "PAWN", "QUEEN", "BOARD"),
    listOf("PUZZLE", "LOGIC", "SUDOKU", "CIPHER", "GRID", "TILE", "SOLVE", "MATRIX"),
    listOf("PLANET", "ORBIT", "GALAXY", "QUASAR", "COMET", "STAR", "NEBULA", "MOON")
)

private data class WordSearchPuzzle(
    val grid: List<List<Char>>,
    val words: Map<String, List<Pair<Int, Int>>>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("word_search") }
    val size = 10

    fun buildPuzzle(): WordSearchPuzzle {
        val pack = wordSearchPacks.random().shuffled()
        val grid = MutableList(size) { MutableList(size) { '.' } }
        val placed = mutableMapOf<String, List<Pair<Int, Int>>>()
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0)
        for (word in pack) {
            for (attempt in 0 until 80) {
                val (dr, dc) = dirs.random()
                val sr = (0 until size).random()
                val sc = (0 until size).random()
                val cells = word.indices.map { k -> (sr + k * dr) to (sc + k * dc) }
                if (cells.all { (r, c) -> r in 0 until size && c in 0 until size } &&
                    cells.withIndex().all { (i, pos) -> grid[pos.first][pos.second] == '.' || grid[pos.first][pos.second] == word[i] }
                ) {
                    cells.forEachIndexed { i, (r, c) -> grid[r][c] = word[i] }
                    placed[word] = cells
                    break
                }
            }
        }
        val alphabet = ('A'..'Z').toList()
        for (r in 0 until size) for (c in 0 until size) {
            if (grid[r][c] == '.') grid[r][c] = alphabet.random()
        }
        return WordSearchPuzzle(grid.map { it.toList() }, placed)
    }

    var puzzle by remember { mutableStateOf(buildPuzzle()) }
    var startCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var foundWords by remember { mutableStateOf(emptySet<String>()) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    val foundCells = remember(foundWords, puzzle) {
        foundWords.flatMap { puzzle.words[it].orEmpty() }.toSet()
    }
    val allFound = puzzle.words.isNotEmpty() && foundWords.size == puzzle.words.size

    LaunchedEffect(allFound) {
        while (!allFound) {
            delay(1000L)
            elapsed++
        }
    }

    LaunchedEffect(allFound) {
        if (allFound && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Found all ${puzzle.words.size} words", puzzle.words.size * 30, elapsed)
        }
    }

    fun restart() {
        puzzle = buildPuzzle(); startCell = null; foundWords = emptySet(); elapsed = 0; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "Tap first & last letter of each word",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        MonoCard(elevated = true) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                puzzle.words.keys.forEach { w ->
                    val found = w in foundWords
                    Text(
                        text = w,
                        modifier = Modifier.springPop(found, popScale = 1.14f),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            textDecoration = if (found) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (found) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).shakeOnTrigger(errorShakeCount),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                for (r in 0 until size) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until size) {
                            val pos = r to c
                            val isStart = startCell == pos
                            val isFound = pos in foundCells
                            val cellBg by animateColorAsState(
                                targetValue = when {
                                    isStart -> MaterialTheme.colorScheme.primary
                                    isFound -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                animationSpec = tween(160),
                                label = "ws_cell_${r}_$c"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.5.dp)
                                    .springPop(isFound, popScale = 1.20f)
                                    .pulseEffect(isStart, maxScale = 1.12f)
                                    .bouncyPress(!allFound)
                                    .background(cellBg, RoundedCornerShape(6.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable(enabled = !allFound) {
                                        val s = startCell
                                        if (s == null) {
                                            startCell = pos
                                            onPlayCue(SoundCue.KEY_TICK)
                                        } else {
                                            val match = puzzle.words.entries.firstOrNull { (w, cells) ->
                                                w !in foundWords &&
                                                    ((cells.first() == s && cells.last() == pos) || (cells.first() == pos && cells.last() == s))
                                            }
                                            if (match != null) {
                                                foundWords = foundWords + match.key
                                                onPlayCue(SoundCue.TILE_MERGE)
                                            } else {
                                                errorShakeCount++
                                                onPlayCue(SoundCue.ERROR)
                                            }
                                            startCell = null
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = puzzle.grid[r][c].toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = if (isStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (allFound) {
        GameCompletionDialog(
            title = "All Words Located!",
            subtitle = "Cleared in %02d:%02d.".format(elapsed / 60, elapsed % 60),
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
