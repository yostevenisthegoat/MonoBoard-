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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.StatMetricTile
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun NumberMatchScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("number_match") }
    val cols = 9

    fun initialCells(): List<Int> = List(27) { (1..9).random() } // 0 = cleared

    var cells by remember { mutableStateOf(initialCells()) }
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var addUsesLeft by remember { mutableIntStateOf(4) }
    var recorded by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    fun canConnect(i1: Int, i2: Int): Boolean {
        if (i1 == i2) return false
        val v1 = cells[i1]; val v2 = cells[i2]
        if (v1 == 0 || v2 == 0) return false
        if (v1 != v2 && v1 + v2 != 10) return false

        val lo = minOf(i1, i2); val hi = maxOf(i1, i2)
        // Linear reading-order adjacency skipping cleared cells
        if ((lo + 1 until hi).all { cells[it] == 0 }) return true

        val r1 = i1 / cols; val c1 = i1 % cols
        val r2 = i2 / cols; val c2 = i2 % cols
        val dr = r2 - r1; val dc = c2 - c1
        val isAligned = dr == 0 || dc == 0 || kotlin.math.abs(dr) == kotlin.math.abs(dc)
        if (!isAligned) return false
        val stepR = dr.coerceIn(-1, 1)
        val stepC = dc.coerceIn(-1, 1)
        var cr = r1 + stepR; var cc = c1 + stepC
        while (cr != r2 || cc != c2) {
            if (cells[cr * cols + cc] != 0) return false
            cr += stepR; cc += stepC
        }
        return true
    }

    val cleared = cells.isNotEmpty() && cells.all { it == 0 }

    LaunchedEffect(cleared) {
        if (cleared && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Cleared board • Score $score", score, 60)
        }
    }

    fun restart() {
        cells = initialCells(); selectedIdx = null; score = 0; addUsesLeft = 4; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "Match equal digits or pairs summing to 10",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "SCORE", value = score.toString(), modifier = Modifier.weight(1f))
            StatMetricTile(label = "DIGITS LEFT", value = cells.count { it != 0 }.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).shakeOnTrigger(errorShakeCount),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(cells) { idx, v ->
                    val isSel = selectedIdx == idx
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .springPop(if (v != 0) v else null, popScale = 1.16f)
                            .pulseEffect(isSel, maxScale = 1.12f)
                            .bouncyPress(v != 0 && !cleared)
                            .background(
                                when {
                                    v == 0 -> MaterialTheme.colorScheme.background
                                    isSel -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                            .clickable(enabled = v != 0 && !cleared) {
                                val prev = selectedIdx
                                if (prev == null) {
                                    selectedIdx = idx
                                    onPlayCue(SoundCue.KEY_TICK)
                                } else if (prev == idx) {
                                    selectedIdx = null
                                    onPlayCue(SoundCue.KEY_TICK)
                                } else if (canConnect(prev, idx)) {
                                    val next = cells.toMutableList()
                                    next[prev] = 0
                                    next[idx] = 0
                                    cells = next
                                    selectedIdx = null
                                    score += 15
                                    onPlayCue(SoundCue.TILE_MERGE)
                                } else {
                                    selectedIdx = idx
                                    errorShakeCount++
                                    onPlayCue(SoundCue.ERROR)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (v != 0) {
                            Text(
                                text = v.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val canAdd = addUsesLeft > 0 && !cleared
        Button(
            onClick = {
                val rem = cells.filter { it != 0 }
                if (rem.isNotEmpty() && addUsesLeft > 0) {
                    cells = cells + rem
                    addUsesLeft--
                    onPlayCue(SoundCue.BOUNCE)
                }
            },
            enabled = canAdd,
            modifier = Modifier.fillMaxWidth().height(48.dp).bouncyPress(canAdd)
        ) {
            Text("Add Remaining Digits ($addUsesLeft left)")
        }
    }

    if (cleared) {
        GameCompletionDialog(
            title = "Board Cleared!",
            subtitle = "Final score: $score",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
