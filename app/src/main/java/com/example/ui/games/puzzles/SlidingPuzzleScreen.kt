package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.StatMetricTile
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun SlidingPuzzleScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("sliding_puzzle") }
    val dim = if (sessionConfig.ruleVariant.contains("3")) 3 else 4

    fun createSolvableBoard(): List<Int> {
        val list = ((1 until dim * dim) + 0).toMutableList()
        var idx0 = list.lastIndex
        repeat(dim * dim * 20) {
            val r = idx0 / dim; val c = idx0 % dim
            val neighbors = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                .map { (dr, dc) -> (r + dr) to (c + dc) }
                .filter { (nr, nc) -> nr in 0 until dim && nc in 0 until dim }
            val (nr, nc) = neighbors.random()
            val nIdx = nr * dim + nc
            list[idx0] = list[nIdx]
            list[nIdx] = 0
            idx0 = nIdx
        }
        return list.toList()
    }

    var tiles by remember(dim) { mutableStateOf(createSolvableBoard()) }
    var moves by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    val solved = remember(tiles) {
        (0 until dim * dim - 1).all { tiles[it] == it + 1 } && tiles.last() == 0
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
            val score = (600 - moves * 2 - elapsed).coerceAtLeast(50)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Solved ${dim}×${dim} in $moves moves", score, elapsed)
        }
    }

    fun tapTile(index: Int) {
        if (solved || tiles[index] == 0) return
        val blankIdx = tiles.indexOf(0)
        val r1 = index / dim; val c1 = index % dim
        val r2 = blankIdx / dim; val c2 = blankIdx % dim
        if (kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) == 1) {
            val next = tiles.toMutableList()
            val movedTile = next[index]
            next[blankIdx] = movedTile
            next[index] = 0
            tiles = next
            moves++
            onPlayCue(if (movedTile == blankIdx + 1) SoundCue.TILE_MERGE else SoundCue.PIECE_SLIDE)
        } else {
            errorShakeCount++
            onPlayCue(SoundCue.ERROR)
        }
    }

    fun restart() {
        tiles = createSolvableBoard(); moves = 0; elapsed = 0; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${dim}×${dim} Puzzle",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "MOVES", value = moves.toString(), modifier = Modifier.weight(1f))
            StatMetricTile(label = "TIME", value = "%02d:%02d".format(elapsed / 60, elapsed % 60), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth, maxHeight, 360.dp)
            Surface(
                modifier = Modifier.size(size).shakeOnTrigger(errorShakeCount),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until dim) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in 0 until dim) {
                                val idx = r * dim + c
                                val v = tiles[idx]
                                val targetColor = if (v == 0) MaterialTheme.colorScheme.background
                                else if (v == idx + 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                                val tileColor by animateColorAsState(targetColor, tween(180), label = "slide_col_$idx")
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .springPop(if (v > 0) v else null, popScale = 1.12f)
                                        .bouncyPress(v != 0 && !solved)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = v != 0 && !solved) { tapTile(idx) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = tileColor,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (v != 0) {
                                            Text(
                                                text = v.toString(),
                                                fontSize = if (dim == 3) 32.sp else 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = if (v == idx + 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
    }

    if (solved) {
        GameCompletionDialog(
            title = "Puzzle Aligned!",
            subtitle = "Completed in $moves moves (%02d:%02d).".format(elapsed / 60, elapsed % 60),
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
