package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.StatMetricTile
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.springPop

@Composable
fun JigsawScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("jigsaw") }
    val dim = if (sessionConfig.ruleVariant.contains("5")) 5 else 4
    val total = dim * dim

    fun shuffledTiles(): List<Int> {
        var arr = (0 until total).shuffled()
        if (arr == (0 until total).toList()) arr = arr.reversed()
        return arr
    }

    var tiles by remember(dim) { mutableStateOf(shuffledTiles()) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var swaps by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val solved = remember(tiles) { tiles == (0 until total).toList() }

    LaunchedEffect(solved) {
        if (solved && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Assembled ${dim}×${dim} geometric composition in $swaps swaps", (300 - swaps * 5).coerceAtLeast(60), 45)
        }
    }

    fun restart() {
        tiles = shuffledTiles(); selectedSlot = null; swaps = 0; recorded = false
    }

    val lockedCount = tiles.indices.count { tiles[it] == it }

    GameScaffold(
        game = game,
        subtitle = "${dim}×${dim} Geometric Composition",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "LOCKED", value = "$lockedCount / $total", modifier = Modifier.weight(1f))
            StatMetricTile(label = "SWAPS", value = swaps.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth, maxHeight, 360.dp)
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (r in 0 until dim) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (c in 0 until dim) {
                                val slot = r * dim + c
                                val pieceId = tiles[slot]
                                val pr = pieceId / dim
                                val pc = pieceId % dim
                                val isLocked = pieceId == slot
                                val isSel = selectedSlot == slot

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .springPop(pieceId to isLocked, popScale = 1.14f)
                                        .pulseEffect(isSel, maxScale = 1.07f)
                                        .bouncyPress(!solved && !isLocked)
                                        .border(
                                            width = if (isSel) 2.5.dp else if (isLocked) 1.5.dp else 0.7.dp,
                                            color = if (isSel) Color.White else if (isLocked) Color(0xFF81C784) else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable(enabled = !solved && !isLocked) {
                                            val prev = selectedSlot
                                            if (prev == null) {
                                                selectedSlot = slot
                                                onPlayCue(SoundCue.KEY_TICK)
                                            } else if (prev == slot) {
                                                selectedSlot = null
                                                onPlayCue(SoundCue.KEY_TICK)
                                            } else {
                                                val next = tiles.toMutableList()
                                                val tmp = next[prev]
                                                next[prev] = next[slot]
                                                next[slot] = tmp
                                                tiles = next
                                                selectedSlot = null
                                                swaps++
                                                onPlayCue(if (next[prev] == prev || next[slot] == slot) SoundCue.TILE_MERGE else SoundCue.PIECE_SLIDE)
                                            }
                                        }
                                ) {
                                    // Draw proportional slice of concentric Bauhaus monochrome artwork
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = this.size.width; val h = this.size.height
                                        val shade = 0.14f + (pr + pc) * 0.08f
                                        drawRect(Color(shade, shade, shade), Offset.Zero, Size(w, h))
                                        // Concentric ring coordinate relative to piece's true position
                                        val cx = (dim / 2f - pc) * w
                                        val cy = (dim / 2f - pr) * h
                                        drawCircle(
                                            color = Color(0xFFEAEAEA),
                                            radius = w * dim * 0.36f,
                                            center = Offset(cx, cy),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.14f)
                                        )
                                        drawCircle(
                                            color = Color(0xFF707070),
                                            radius = w * dim * 0.2f,
                                            center = Offset(cx, cy)
                                        )
                                    }
                                    if (!isLocked) {
                                        Text(
                                            text = "${pieceId + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.55f),
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
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

    if (solved) {
        GameCompletionDialog(
            title = "Composition Locked!",
            subtitle = "Assembled in $swaps swaps.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
