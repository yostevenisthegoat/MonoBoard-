package com.example.ui.games.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.theme.JetBrainsMonoFontFamily

private data class SpiderState(
    val columns: List<List<PlayingCard>>,
    val stock: List<PlayingCard>,
    val completedSets: Int = 0,
    val moves: Int = 0
)

@Composable
fun SpiderSolitaireScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("spider_solitaire") }
    val twoSuits = sessionConfig.ruleVariant.contains("2")

    fun deal(): SpiderState {
        val suits = if (twoSuits) listOf(Suit.SPADES, Suit.HEARTS) else listOf(Suit.SPADES)
        val deck = (0 until 8).flatMap { setIdx ->
            val suit = suits[setIdx % suits.size]
            Rank.entries.map { r -> PlayingCard(suit, r, faceUp = false) }
        }.shuffled()
        var cursor = 0
        val cols = MutableList(10) { col ->
            val cnt = if (col < 4) 6 else 5
            val slice = deck.subList(cursor, cursor + cnt).mapIndexed { i, c -> c.copy(faceUp = i == cnt - 1) }
            cursor += cnt
            slice
        }
        return SpiderState(columns = cols, stock = deck.drop(cursor))
    }

    var state by remember(twoSuits) { mutableStateOf(deal()) }
    val undoStack = remember { mutableStateListOf<SpiderState>() }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var recorded by remember { mutableStateOf(false) }

    val won = state.completedSets == 8

    LaunchedEffect(won) {
        if (won && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Completed all 8 Spider runs", 300, 180)
        }
    }

    fun isOrderedSuitedRun(col: List<PlayingCard>, startIdx: Int): Boolean {
        for (i in startIdx until col.lastIndex) {
            val a = col[i]; val b = col[i + 1]
            if (!a.faceUp || !b.faceUp || a.suit != b.suit || a.rank.value != b.rank.value + 1) return false
        }
        return col[startIdx].faceUp
    }

    fun clearCompletedRuns(cols: List<List<PlayingCard>>): Pair<List<List<PlayingCard>>, Int> {
        var cleared = 0
        val next = cols.map { col ->
            var cur = col
            if (cur.size >= 13) {
                val tail = cur.takeLast(13)
                val fullSuit = tail.all { it.suit == tail.first().suit }
                val desc = (0 until 13).all { idx -> tail[idx].rank.value == 13 - idx }
                if (fullSuit && desc) {
                    cur = cur.dropLast(13)
                    cleared++
                }
            }
            if (cur.isNotEmpty() && !cur.last().faceUp) cur.dropLast(1) + cur.last().copy(faceUp = true) else cur
        }
        return next to cleared
    }

    fun tryMove(targetCol: Int) {
        val (srcCol, srcIdx) = selected ?: return
        if (srcCol == targetCol) { selected = null; return }
        val moving = state.columns[srcCol].drop(srcIdx)
        val destTop = state.columns[targetCol].lastOrNull()
        if (destTop == null || destTop.rank.value == moving.first().rank.value + 1) {
            undoStack.add(state)
            val rawCols = state.columns.mapIndexed { i, c ->
                when (i) {
                    srcCol -> c.take(srcIdx)
                    targetCol -> c + moving
                    else -> c
                }
            }
            val (cleaned, newlyCleared) = clearCompletedRuns(rawCols)
            state = state.copy(
                columns = cleaned,
                completedSets = state.completedSets + newlyCleared,
                moves = state.moves + 1
            )
            selected = null
            onPlayCue(if (newlyCleared > 0) SoundCue.WIN else SoundCue.MOVE)
        } else {
            selected = null
        }
    }

    fun dealFromStock() {
        if (state.stock.size < 10) return
        undoStack.add(state)
        val nextBatch = state.stock.take(10)
        val rawCols = state.columns.mapIndexed { i, col -> col + nextBatch[i].copy(faceUp = true) }
        val (cleaned, newlyCleared) = clearCompletedRuns(rawCols)
        state = state.copy(
            columns = cleaned,
            stock = state.stock.drop(10),
            completedSets = state.completedSets + newlyCleared,
            moves = state.moves + 1
        )
        selected = null
        onPlayCue(SoundCue.MOVE)
    }

    fun restart() {
        state = deal(); undoStack.clear(); selected = null; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "Sets: ${state.completedSets}/8 • Stock: ${state.stock.size}",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty(),
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty()) { state = undoStack.removeAt(undoStack.lastIndex); selected = null }
        },
        onRestart = ::restart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Completed: ${state.completedSets} / 8", style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily))
            Surface(
                modifier = Modifier.clickable(enabled = state.stock.size >= 10) { dealFromStock() },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    "Deal Row (${state.stock.size / 10})",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            state.columns.forEachIndexed { colIdx, col ->
                Box(modifier = Modifier.weight(1f).clickable(enabled = col.isEmpty() && selected != null) { tryMove(colIdx) }) {
                    if (col.isEmpty()) {
                        PlayingCardView(card = null, width = 34.dp, height = 52.dp, onClick = { tryMove(colIdx) })
                    } else {
                        col.forEachIndexed { cIdx, card ->
                            val isSel = selected?.let { it.first == colIdx && cIdx >= it.second } ?: false
                            PlayingCardView(
                                card = card,
                                selected = isSel,
                                width = 34.dp,
                                height = 52.dp,
                                modifier = Modifier.offset(y = (cIdx * 16).dp),
                                onClick = {
                                    if (!card.faceUp) return@PlayingCardView
                                    if (selected != null && selected!!.first != colIdx) {
                                        tryMove(colIdx)
                                    } else if (isOrderedSuitedRun(col, cIdx)) {
                                        selected = if (selected == (colIdx to cIdx)) null else colIdx to cIdx
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (won) {
        GameCompletionDialog(
            title = "Spider Cleared!",
            subtitle = "All 8 K-to-A suited runs completed in ${state.moves} moves.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
