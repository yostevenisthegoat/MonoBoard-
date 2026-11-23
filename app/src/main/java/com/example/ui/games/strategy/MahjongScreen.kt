package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
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
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.StatMetricTile

private data class MahjongTile(
    val id: Int,
    val symbol: String,
    val row: Int,
    val col: Int,
    val layer: Int,
    val removed: Boolean = false
)

private val mahjongSymbols = listOf(
    "一", "二", "三", "四", "五", "六", "七", "八", "九",
    "東", "南", "西", "北", "中", "發", "白", "竹", "梅"
)

@Composable
fun MahjongScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("mahjong") }

    fun buildLayout(): List<MahjongTile> {
        val coords = mutableListOf<Triple<Int, Int, Int>>()
        // Layer 0: 6x6 = 36
        for (r in 0..5) for (c in 0..5) coords.add(Triple(r, c, 0))
        // Layer 1: 4x4 = 16
        for (r in 1..4) for (c in 1..4) coords.add(Triple(r, c, 1))
        // Layer 2: 2x2 = 4 (Total = 56 tiles = 28 pairs)
        for (r in 2..3) for (c in 2..3) coords.add(Triple(r, c, 2))

        val pairSymbols = (0 until 28).flatMap { i ->
            val sym = mahjongSymbols[i % mahjongSymbols.size]
            listOf(sym, sym)
        }.shuffled()

        return coords.mapIndexed { idx, (r, c, l) ->
            MahjongTile(idx, pairSymbols[idx], r, c, l)
        }
    }

    var tiles by remember { mutableStateOf(buildLayout()) }
    val undoStack = remember { mutableStateListOf<List<MahjongTile>>() }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var matches by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    fun isFree(t: MahjongTile, all: List<MahjongTile>): Boolean {
        if (t.removed) return false
        val active = all.filter { !it.removed }
        val covered = active.any { it.layer > t.layer && it.row == t.row && it.col == t.col }
        if (covered) return false
        val leftBlocked = active.any { it.layer == t.layer && it.row == t.row && it.col == t.col - 1 }
        val rightBlocked = active.any { it.layer == t.layer && it.row == t.row && it.col == t.col + 1 }
        return !leftBlocked || !rightBlocked
    }

    val topVisibleGrid = remember(tiles) {
        val map = mutableMapOf<Pair<Int, Int>, MahjongTile>()
        tiles.filter { !it.removed }.sortedBy { it.layer }.forEach { map[it.row to it.col] = it }
        map
    }

    val remaining = tiles.count { !it.removed }
    val won = remaining == 0

    LaunchedEffect(won) {
        if (won && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Cleared all 56 Mahjong tiles", 280, 90)
        }
    }

    fun restart() {
        tiles = buildLayout(); undoStack.clear(); selectedId = null; matches = 0; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "3-Layer Pyramid • $remaining Tiles Left",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty(),
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty()) {
                tiles = undoStack.removeAt(undoStack.lastIndex)
                selectedId = null
                matches = (matches - 1).coerceAtLeast(0)
            }
        },
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "REMAINING", value = remaining.toString(), modifier = Modifier.weight(1f))
            StatMetricTile(label = "PAIRS CLEARED", value = matches.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            for (r in 0..5) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..5) {
                        val tile = topVisibleGrid[r to c]
                        if (tile == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.85f))
                        } else {
                            val free = isFree(tile, tiles)
                            val isSel = selectedId == tile.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = free) {
                                        val prevId = selectedId
                                        if (prevId == null) {
                                            selectedId = tile.id
                                        } else if (prevId == tile.id) {
                                            selectedId = null
                                        } else {
                                            val prevTile = tiles.first { it.id == prevId }
                                            if (prevTile.symbol == tile.symbol) {
                                                undoStack.add(tiles)
                                                tiles = tiles.map {
                                                    if (it.id == prevId || it.id == tile.id) it.copy(removed = true) else it
                                                }
                                                selectedId = null
                                                matches++
                                                onPlayCue(SoundCue.TURN_PASS)
                                            } else {
                                                selectedId = tile.id
                                                onPlayCue(SoundCue.ERROR)
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSel -> MaterialTheme.colorScheme.primary
                                    free -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(if (free) 1.5.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = tile.symbol,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSel -> MaterialTheme.colorScheme.onPrimary
                                            free -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        }
                                    )
                                    if (tile.layer > 0) {
                                        Text(
                                            text = "L${tile.layer + 1}",
                                            fontSize = 8.sp,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)
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

    if (won) {
        GameCompletionDialog(
            title = "Mahjong Cleared!",
            subtitle = "All 28 pairs matched.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
