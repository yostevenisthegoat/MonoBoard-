package com.example.ui.games.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.shakeOnTrigger

private data class FreeCellState(
    val freeCells: List<PlayingCard?>,
    val foundations: List<List<PlayingCard>>,
    val cascades: List<List<PlayingCard>>,
    val moves: Int = 0
)

private sealed class FcSel {
    data class Free(val idx: Int) : FcSel()
    data class Cascade(val col: Int, val cardIdx: Int) : FcSel()
}

@Composable
fun FreeCellScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("freecell") }

    fun deal(): FreeCellState {
        val deck = standardDeck(faceUp = true).shuffled()
        val cascades = List(8) { mutableListOf<PlayingCard>() }
        deck.forEachIndexed { idx, card -> cascades[idx % 8].add(card) }
        return FreeCellState(
            freeCells = List(4) { null },
            foundations = List(4) { emptyList() },
            cascades = cascades.map { it.toList() }
        )
    }

    var state by remember { mutableStateOf(deal()) }
    val undoStack = remember { mutableStateListOf<FreeCellState>() }
    var selection by remember { mutableStateOf<FcSel?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val won = state.foundations.sumOf { it.size } == 52

    LaunchedEffect(won) {
        if (won && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Cleared FreeCell in ${state.moves} moves", (350 - state.moves).coerceAtLeast(100), 120)
        }
    }

    fun canFoundation(c: PlayingCard, f: List<PlayingCard>): Boolean {
        val top = f.lastOrNull() ?: return c.rank == Rank.ACE
        return c.suit == top.suit && c.rank.value == top.rank.value + 1
    }

    fun canCascade(c: PlayingCard, col: List<PlayingCard>): Boolean {
        val top = col.lastOrNull() ?: return true
        return c.suit.isRed != top.suit.isRed && c.rank.value == top.rank.value - 1
    }

    fun isAlternatingRun(col: List<PlayingCard>, startIdx: Int): Boolean {
        for (i in startIdx until col.lastIndex) {
            val a = col[i]; val b = col[i + 1]
            if (a.suit.isRed == b.suit.isRed || a.rank.value != b.rank.value + 1) return false
        }
        return true
    }

    fun moveToFoundation(fIdx: Int): Boolean {
        val sel = selection ?: return false
        val card = when (sel) {
            is FcSel.Free -> state.freeCells[sel.idx]
            is FcSel.Cascade -> if (sel.cardIdx == state.cascades[sel.col].lastIndex) state.cascades[sel.col].lastOrNull() else null
        } ?: return false
        if (!canFoundation(card, state.foundations[fIdx])) return false
        undoStack.add(state)
        val nextFound = state.foundations.mapIndexed { i, l -> if (i == fIdx) l + card else l }
        state = when (sel) {
            is FcSel.Free -> state.copy(
                freeCells = state.freeCells.mapIndexed { i, c -> if (i == sel.idx) null else c },
                foundations = nextFound, moves = state.moves + 1
            )
            is FcSel.Cascade -> state.copy(
                cascades = state.cascades.mapIndexed { i, l -> if (i == sel.col) l.dropLast(1) else l },
                foundations = nextFound, moves = state.moves + 1
            )
        }
        selection = null
        onPlayCue(SoundCue.TILE_MERGE)
        return true
    }

    fun moveToFreeCell(freeIdx: Int) {
        if (state.freeCells[freeIdx] != null) return
        val sel = selection ?: return
        val card = when (sel) {
            is FcSel.Free -> state.freeCells[sel.idx]
            is FcSel.Cascade -> if (sel.cardIdx == state.cascades[sel.col].lastIndex) state.cascades[sel.col].lastOrNull() else null
        } ?: return
        undoStack.add(state)
        val nextFree = state.freeCells.toMutableList()
        if (sel is FcSel.Free) nextFree[sel.idx] = null
        nextFree[freeIdx] = card
        val nextCasc = if (sel is FcSel.Cascade) state.cascades.mapIndexed { i, l -> if (i == sel.col) l.dropLast(1) else l } else state.cascades
        state = state.copy(freeCells = nextFree, cascades = nextCasc, moves = state.moves + 1)
        selection = null
        onPlayCue(SoundCue.CARD_FLIP)
    }

    fun moveToCascade(targetCol: Int): Boolean {
        val sel = selection ?: return false
        val moving = when (sel) {
            is FcSel.Free -> listOfNotNull(state.freeCells[sel.idx])
            is FcSel.Cascade -> if (sel.col == targetCol) emptyList() else state.cascades[sel.col].drop(sel.cardIdx)
        }
        if (moving.isEmpty()) return false
        val freeOpen = state.freeCells.count { it == null }
        val emptyCasc = state.cascades.indices.count { it != targetCol && state.cascades[it].isEmpty() }
        val maxMove = (freeOpen + 1) * (1 shl emptyCasc)
        if (moving.size > maxMove || !canCascade(moving.first(), state.cascades[targetCol])) {
            shakeCount++
            onPlayCue(SoundCue.ERROR)
            return false
        }
        undoStack.add(state)
        state = when (sel) {
            is FcSel.Free -> state.copy(
                freeCells = state.freeCells.mapIndexed { i, c -> if (i == sel.idx) null else c },
                cascades = state.cascades.mapIndexed { i, l -> if (i == targetCol) l + moving else l },
                moves = state.moves + 1
            )
            is FcSel.Cascade -> state.copy(
                cascades = state.cascades.mapIndexed { i, l ->
                    when (i) {
                        sel.col -> l.take(sel.cardIdx)
                        targetCol -> l + moving
                        else -> l
                    }
                },
                moves = state.moves + 1
            )
        }
        selection = null
        onPlayCue(SoundCue.CARD_FLIP)
        return true
    }

    fun restart() {
        state = deal(); undoStack.clear(); selection = null; recorded = false
        onPlayCue(SoundCue.SHUFFLE)
    }

    GameScaffold(
        game = game,
        subtitle = "Moves: ${state.moves}",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty(),
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty()) { state = undoStack.removeAt(undoStack.lastIndex); selection = null }
        },
        onRestart = ::restart
    ) {
        // 4 Free Cells + 4 Foundations
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                state.freeCells.forEachIndexed { idx, card ->
                    PlayingCardView(
                        card = card,
                        selected = (selection as? FcSel.Free)?.idx == idx,
                        width = 40.dp,
                        height = 58.dp,
                        onClick = {
                            if (card == null) moveToFreeCell(idx)
                            else {
                                selection = if ((selection as? FcSel.Free)?.idx == idx) null else FcSel.Free(idx)
                                onPlayCue(SoundCue.KEY_TICK)
                            }
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                state.foundations.forEachIndexed { fIdx, pile ->
                    PlayingCardView(
                        card = pile.lastOrNull(),
                        width = 40.dp,
                        height = 58.dp,
                        onClick = { moveToFoundation(fIdx) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shakeOnTrigger(shakeCount),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            state.cascades.forEachIndexed { colIdx, col ->
                Box(modifier = Modifier.weight(1f).clickable(enabled = col.isEmpty() && selection != null) { moveToCascade(colIdx) }) {
                    if (col.isEmpty()) {
                        PlayingCardView(card = null, width = 41.dp, height = 60.dp, onClick = { moveToCascade(colIdx) })
                    } else {
                        col.forEachIndexed { cIdx, card ->
                            val isSel = (selection as? FcSel.Cascade)?.let { it.col == colIdx && cIdx >= it.cardIdx } ?: false
                            PlayingCardView(
                                card = card,
                                selected = isSel,
                                width = 41.dp,
                                height = 60.dp,
                                modifier = Modifier.offset(y = (cIdx * 19).dp),
                                onClick = {
                                    if (selection != null && (selection as? FcSel.Cascade)?.col != colIdx) {
                                        if (!moveToCascade(colIdx) && isAlternatingRun(col, cIdx)) {
                                            selection = FcSel.Cascade(colIdx, cIdx)
                                            onPlayCue(SoundCue.KEY_TICK)
                                        }
                                    } else if (isAlternatingRun(col, cIdx)) {
                                        if (selection == FcSel.Cascade(colIdx, cIdx) && cIdx == col.lastIndex) {
                                            val fIdx = state.foundations.indexOfFirst { canFoundation(card, it) }
                                            if (fIdx != -1) moveToFoundation(fIdx) else selection = null
                                        } else {
                                            selection = FcSel.Cascade(colIdx, cIdx)
                                            onPlayCue(SoundCue.KEY_TICK)
                                        }
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
            title = "FreeCell Complete!",
            subtitle = "All 52 cards moved to foundations in ${state.moves} moves.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
