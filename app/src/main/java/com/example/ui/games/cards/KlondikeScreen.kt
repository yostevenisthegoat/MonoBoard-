package com.example.ui.games.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.ui.components.bouncyPress
import com.example.ui.components.springPop

private data class KlondikeState(
    val stock: List<PlayingCard>,
    val waste: List<PlayingCard>,
    val foundations: List<List<PlayingCard>>,
    val tableau: List<List<PlayingCard>>,
    val moves: Int = 0
)

private sealed class KlondikeSelection {
    data object Waste : KlondikeSelection()
    data class Tableau(val col: Int, val cardIndex: Int) : KlondikeSelection()
}

@Composable
fun KlondikeScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("solitaire") }
    val drawThree = sessionConfig.ruleVariant.contains("3")

    fun deal(): KlondikeState {
        val deck = standardDeck(faceUp = false).shuffled()
        var cursor = 0
        val tab = MutableList(7) { col ->
            val count = col + 1
            val slice = deck.subList(cursor, cursor + count).mapIndexed { idx, c ->
                c.copy(faceUp = idx == count - 1)
            }
            cursor += count
            slice
        }
        val stock = deck.drop(cursor)
        return KlondikeState(stock = stock, waste = emptyList(), foundations = List(4) { emptyList() }, tableau = tab)
    }

    var state by remember { mutableStateOf(deal()) }
    val undoStack = remember { mutableStateListOf<KlondikeState>() }
    var selection by remember { mutableStateOf<KlondikeSelection?>(null) }
    var recorded by remember { mutableStateOf(false) }

    val won = state.foundations.sumOf { it.size } == 52

    LaunchedEffect(won) {
        if (won && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            onRecordOutcome(MatchOutcome.WIN, "Player 1", "Completed Klondike in ${state.moves} moves", (400 - state.moves).coerceAtLeast(100), 120)
        }
    }

    fun canPlaceOnFoundation(card: PlayingCard, pile: List<PlayingCard>): Boolean {
        val top = pile.lastOrNull() ?: return card.rank == Rank.ACE
        return card.suit == top.suit && card.rank.value == top.rank.value + 1
    }

    fun canPlaceOnTableau(card: PlayingCard, col: List<PlayingCard>): Boolean {
        val top = col.lastOrNull() ?: return card.rank == Rank.KING
        return top.faceUp && card.suit.isRed != top.suit.isRed && card.rank.value == top.rank.value - 1
    }

    fun flipTops(tab: List<List<PlayingCard>>): List<List<PlayingCard>> = tab.map { col ->
        if (col.isNotEmpty() && !col.last().faceUp) col.dropLast(1) + col.last().copy(faceUp = true) else col
    }

    fun tryMoveToFoundation(fIdx: Int): Boolean {
        val sel = selection ?: return false
        val card = when (sel) {
            is KlondikeSelection.Waste -> state.waste.lastOrNull()
            is KlondikeSelection.Tableau -> {
                val col = state.tableau[sel.col]
                if (sel.cardIndex == col.lastIndex) col.lastOrNull() else null
            }
        } ?: return false
        if (!canPlaceOnFoundation(card, state.foundations[fIdx])) return false
        undoStack.add(state)
        val nextFound = state.foundations.mapIndexed { i, p -> if (i == fIdx) p + card else p }
        state = when (sel) {
            is KlondikeSelection.Waste -> state.copy(waste = state.waste.dropLast(1), foundations = nextFound, moves = state.moves + 1)
            is KlondikeSelection.Tableau -> {
                val nextTab = flipTops(state.tableau.mapIndexed { i, c -> if (i == sel.col) c.dropLast(1) else c })
                state.copy(tableau = nextTab, foundations = nextFound, moves = state.moves + 1)
            }
        }
        selection = null
        onPlayCue(SoundCue.TILE_MERGE)
        return true
    }

    fun tryMoveToTableau(targetCol: Int): Boolean {
        val sel = selection ?: return false
        val movingCards = when (sel) {
            is KlondikeSelection.Waste -> listOfNotNull(state.waste.lastOrNull())
            is KlondikeSelection.Tableau -> if (sel.col == targetCol) emptyList() else state.tableau[sel.col].drop(sel.cardIndex)
        }
        if (movingCards.isEmpty()) return false
        if (!canPlaceOnTableau(movingCards.first(), state.tableau[targetCol])) return false
        undoStack.add(state)
        state = when (sel) {
            is KlondikeSelection.Waste -> {
                val nextTab = state.tableau.mapIndexed { i, c -> if (i == targetCol) c + movingCards else c }
                state.copy(waste = state.waste.dropLast(1), tableau = nextTab, moves = state.moves + 1)
            }
            is KlondikeSelection.Tableau -> {
                val nextTab = flipTops(
                    state.tableau.mapIndexed { i, c ->
                        when (i) {
                            sel.col -> c.take(sel.cardIndex)
                            targetCol -> c + movingCards
                            else -> c
                        }
                    }
                )
                state.copy(tableau = nextTab, moves = state.moves + 1)
            }
        }
        selection = null
        onPlayCue(SoundCue.CARD_FLIP)
        return true
    }

    fun drawStock() {
        undoStack.add(state)
        selection = null
        if (state.stock.isNotEmpty()) {
            val count = if (drawThree) minOf(3, state.stock.size) else 1
            val drawn = state.stock.takeLast(count).reversed().map { it.copy(faceUp = true) }
            state = state.copy(
                stock = state.stock.dropLast(count),
                waste = state.waste + drawn,
                moves = state.moves + 1
            )
            onPlayCue(SoundCue.CARD_FLIP)
        } else if (state.waste.isNotEmpty()) {
            state = state.copy(
                stock = state.waste.reversed().map { it.copy(faceUp = false) },
                waste = emptyList(),
                moves = state.moves + 1
            )
            onPlayCue(SoundCue.SHUFFLE)
        }
    }

    fun restart() {
        state = deal(); undoStack.clear(); selection = null; recorded = false
        onPlayCue(SoundCue.SHUFFLE)
    }

    GameScaffold(
        game = game,
        subtitle = "${sessionConfig.ruleVariant} • ${state.moves} Moves",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty(),
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty()) {
                state = undoStack.removeAt(undoStack.lastIndex)
                selection = null
            }
        },
        onRestart = ::restart
    ) {
        // Top Row: Stock, Waste, 4 Foundations
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .size(width = 46.dp, height = 66.dp)
                        .bouncyPress()
                        .springPop(state.stock.size, popScale = 1.06f)
                        .clickable { drawStock() },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (state.stock.isNotEmpty()) "${state.stock.size}" else "↻", fontSize = 14.sp)
                    }
                }
                PlayingCardView(
                    card = state.waste.lastOrNull(),
                    selected = selection is KlondikeSelection.Waste,
                    width = 46.dp,
                    height = 66.dp,
                    onClick = {
                        val top = state.waste.lastOrNull() ?: return@PlayingCardView
                        if (selection is KlondikeSelection.Waste) {
                            // Auto send to foundation on double-tap
                            val fIdx = state.foundations.indexOfFirst { canPlaceOnFoundation(top, it) }
                            if (fIdx != -1) tryMoveToFoundation(fIdx) else selection = null
                        } else {
                            selection = KlondikeSelection.Waste
                            onPlayCue(SoundCue.KEY_TICK)
                        }
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                state.foundations.forEachIndexed { fIdx, pile ->
                    PlayingCardView(
                        card = pile.lastOrNull(),
                        width = 44.dp,
                        height = 66.dp,
                        onClick = { tryMoveToFoundation(fIdx) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 7 Tableau Columns
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.tableau.forEachIndexed { colIdx, col ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = col.isEmpty() && selection != null) { tryMoveToTableau(colIdx) }
                ) {
                    if (col.isEmpty()) {
                        PlayingCardView(card = null, width = 46.dp, height = 66.dp, onClick = { tryMoveToTableau(colIdx) })
                    } else {
                        col.forEachIndexed { cardIdx, card ->
                            val isSel = (selection as? KlondikeSelection.Tableau)?.let {
                                it.col == colIdx && cardIdx >= it.cardIndex
                            } ?: false
                            PlayingCardView(
                                card = card,
                                selected = isSel,
                                width = 46.dp,
                                height = 66.dp,
                                modifier = Modifier.offset(y = (cardIdx * 20).dp),
                                onClick = {
                                    if (!card.faceUp) return@PlayingCardView
                                    val sel = selection
                                    if (sel == null) {
                                        selection = KlondikeSelection.Tableau(colIdx, cardIdx)
                                        onPlayCue(SoundCue.KEY_TICK)
                                    } else if (sel is KlondikeSelection.Tableau && sel.col == colIdx && sel.cardIndex == cardIdx) {
                                        // Double tap top card -> auto foundation
                                        if (cardIdx == col.lastIndex) {
                                            val fIdx = state.foundations.indexOfFirst { canPlaceOnFoundation(card, it) }
                                            if (fIdx != -1) tryMoveToFoundation(fIdx) else selection = null
                                        } else selection = null
                                    } else {
                                        if (!tryMoveToTableau(colIdx)) {
                                            selection = KlondikeSelection.Tableau(colIdx, cardIdx)
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
            title = "Solitaire Complete!",
            subtitle = "All 4 foundations built in ${state.moves} moves.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
