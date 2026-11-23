package com.example.ui.games.cards

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.PrivacyCurtainOverlay
import kotlinx.coroutines.delay

@Composable
fun CrazyEightsScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("crazy_eights") }
    val pCount = sessionConfig.players.size

    fun deal(): Triple<List<List<PlayingCard>>, List<PlayingCard>, PlayingCard> {
        val deck = standardDeck(faceUp = true).shuffled()
        val hands = (0 until pCount).map { idx -> deck.subList(idx * 5, (idx + 1) * 5) }
        val top = deck[pCount * 5]
        val stock = deck.drop(pCount * 5 + 1)
        return Triple(hands, stock, top)
    }

    var dealt by remember(pCount) { mutableStateOf(deal()) }
    var hands by remember(dealt) { mutableStateOf(dealt.first) }
    var stock by remember(dealt) { mutableStateOf(dealt.second) }
    var discardPile by remember(dealt) { mutableStateOf(listOf(dealt.third)) }
    var activeSuit by remember(dealt) { mutableStateOf(dealt.third.suit) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var showCurtain by remember { mutableStateOf(false) }
    var pendingWildCard by remember { mutableStateOf<PlayingCard?>(null) }
    var statusText by remember { mutableStateOf("Match suit or rank, or play an 8!") }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    val topCard = discardPile.last()

    fun isPlayable(c: PlayingCard): Boolean =
        c.rank == Rank.EIGHT || c.suit == activeSuit || c.rank == topCard.rank

    fun advanceTurn() {
        val next = (activeSeat + 1) % pCount
        activeSeat = next
        if (sessionConfig.privacyMode && !sessionConfig.players[next].isAi) {
            showCurtain = true
        }
    }

    fun playCard(card: PlayingCard, chosenSuit: Suit? = null) {
        if (card.rank == Rank.EIGHT && chosenSuit == null && !sessionConfig.players[activeSeat].isAi) {
            pendingWildCard = card
            return
        }
        val nextSuit = if (card.rank == Rank.EIGHT) chosenSuit ?: Suit.SPADES else card.suit
        val nextHands = hands.mapIndexed { idx, h -> if (idx == activeSeat) h - card else h }
        hands = nextHands
        discardPile = discardPile + card
        activeSuit = nextSuit
        onPlayCue(SoundCue.MOVE)
        statusText = "${sessionConfig.players[activeSeat].name} played ${card.label} (Suit: ${nextSuit.symbol})"

        if (nextHands[activeSeat].isEmpty()) {
            winnerSeat = activeSeat
            onPlayCue(SoundCue.WIN)
        } else {
            advanceTurn()
        }
    }

    fun drawCard() {
        if (stock.isEmpty() && discardPile.size > 1) {
            stock = discardPile.dropLast(1).shuffled()
            discardPile = listOf(discardPile.last())
        }
        if (stock.isEmpty()) {
            statusText = "${sessionConfig.players[activeSeat].name} passed (empty deck)."
            advanceTurn()
            return
        }
        val drawn = stock.first()
        stock = stock.drop(1)
        hands = hands.mapIndexed { idx, h -> if (idx == activeSeat) h + drawn else h }
        onPlayCue(SoundCue.MOVE)
        statusText = "${sessionConfig.players[activeSeat].name} drew a card."
        if (!isPlayable(drawn)) advanceTurn()
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, hands, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi) {
            delay(480L)
            val playable = hands[activeSeat].filter { isPlayable(it) }
            if (playable.isNotEmpty()) {
                val pick = playable.firstOrNull { it.rank != Rank.EIGHT } ?: playable.first()
                val aiSuit = hands[activeSeat].groupingBy { it.suit }.eachCount().maxByOrNull { it.value }?.key ?: Suit.SPADES
                playCard(pick, chosenSuit = aiSuit)
            } else {
                drawCard()
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} emptied their hand", 100, 60)
        }
    }

    fun restart() {
        dealt = deal(); activeSeat = 0; showCurtain = false; pendingWildCard = null
        winnerSeat = null; recorded = false; statusText = "Match suit or rank, or play an 8!"
    }

    GameScaffold(
        game = game,
        subtitle = "Active Suit: ${activeSuit.symbol} • Deck: ${stock.size}",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showCurtain && !activePlayer.isAi) {
            PrivacyCurtainOverlay(
                nextPlayer = activePlayer,
                reason = "Your cards are hidden while passing the device.",
                onRevealClick = { showCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = hands.map { it.size },
                statusMessage = "$statusText (Counts = cards left)"
            )
            Spacer(Modifier.height(16.dp))

            MonoCard(elevated = true, modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("ACTIVE SUIT: ${activeSuit.symbol}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    PlayingCardView(topCard, width = 84.dp, height = 118.dp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("${activePlayer.name}'s Hand", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hands[activeSeat].forEach { card ->
                    val ok = isPlayable(card) && !activePlayer.isAi && winnerSeat == null
                    PlayingCardView(
                        card = card,
                        selected = ok,
                        width = 62.dp,
                        height = 88.dp,
                        onClick = { if (ok) playCard(card) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = ::drawCard,
                enabled = !activePlayer.isAi && winnerSeat == null,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Draw Card") }
        }
    }

    if (pendingWildCard != null) {
        AlertDialog(
            onDismissRequest = { pendingWildCard = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Crazy 8 — Pick Suit") },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Suit.entries.forEach { s ->
                        OutlinedButton(onClick = {
                            val card = pendingWildCard!!
                            pendingWildCard = null
                            playCard(card, chosenSuit = s)
                        }) { Text(s.symbol) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = "Emptied hand first in Crazy Eights.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
