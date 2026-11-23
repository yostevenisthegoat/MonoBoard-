package com.example.ui.games.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.StatMetricTile

private enum class BjPhase { BETTING, PLAYER_TURN, RESOLVED }

@Composable
fun BlackjackScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("blackjack") }
    var chips by remember { mutableIntStateOf(500) }
    var bet by remember { mutableIntStateOf(25) }
    var deck by remember { mutableStateOf(standardDeck(faceUp = true).shuffled()) }
    var playerHand by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var dealerHand by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var phase by remember { mutableStateOf(BjPhase.BETTING) }
    var statusText by remember { mutableStateOf("Place an offline chip bet and deal.") }

    fun handValue(hand: List<PlayingCard>): Int {
        var sum = hand.filter { it.faceUp }.sumOf { it.blackjackValue }
        var aces = hand.count { it.faceUp && it.rank == Rank.ACE }
        while (sum > 21 && aces > 0) {
            sum -= 10
            aces--
        }
        return sum
    }

    fun dealRound() {
        if (chips < bet) bet = chips.coerceAtLeast(5)
        if (chips <= 0) chips = 500
        if (deck.size < 12) deck = standardDeck(faceUp = true).shuffled()
        chips -= bet
        playerHand = listOf(deck[0], deck[2])
        dealerHand = listOf(deck[1], deck[3].copy(faceUp = false))
        deck = deck.drop(4)
        phase = BjPhase.PLAYER_TURN
        onPlayCue(SoundCue.MOVE)

        if (handValue(playerHand) == 21) {
            val revealedDealer = dealerHand.map { it.copy(faceUp = true) }
            dealerHand = revealedDealer
            phase = BjPhase.RESOLVED
            if (handValue(revealedDealer) == 21) {
                chips += bet
                statusText = "Push — Both have Blackjack!"
                onRecordOutcome(MatchOutcome.DRAW, "Push", "Blackjack Push", chips, 15)
            } else {
                val payout = (bet * 2.5).toInt()
                chips += payout
                statusText = "BLACKJACK! Won +${payout - bet} chips!"
                onPlayCue(SoundCue.WIN)
                onRecordOutcome(MatchOutcome.WIN, "Player 1", "Natural Blackjack", chips, 15)
            }
        } else {
            statusText = "Your hand: ${handValue(playerHand)} — Hit, Stand, or Double"
        }
    }

    fun resolveDealer() {
        var curDealer = dealerHand.map { it.copy(faceUp = true) }
        var curDeck = deck
        while (handValue(curDealer) < 17 && curDeck.isNotEmpty()) {
            curDealer = curDealer + curDeck.first()
            curDeck = curDeck.drop(1)
        }
        dealerHand = curDealer
        deck = curDeck
        phase = BjPhase.RESOLVED

        val pv = handValue(playerHand)
        val dv = handValue(curDealer)
        when {
            dv > 21 || pv > dv -> {
                chips += bet * 2
                statusText = "You win! ($pv vs $dv)"
                onPlayCue(SoundCue.WIN)
                onRecordOutcome(MatchOutcome.WIN, "Player 1", "Won $pv vs $dv", chips, 20)
            }
            pv < dv -> {
                statusText = "Dealer wins ($dv vs $pv)"
                onPlayCue(SoundCue.ERROR)
                onRecordOutcome(MatchOutcome.LOSS, "Dealer", "Lost $pv vs $dv", chips, 20)
            }
            else -> {
                chips += bet
                statusText = "Push at $pv"
                onRecordOutcome(MatchOutcome.DRAW, "Push", "Push at $pv", chips, 20)
            }
        }
    }

    fun hit() {
        if (phase != BjPhase.PLAYER_TURN || deck.isEmpty()) return
        val next = playerHand + deck.first()
        playerHand = next
        deck = deck.drop(1)
        onPlayCue(SoundCue.MOVE)
        val pv = handValue(next)
        if (pv > 21) {
            dealerHand = dealerHand.map { it.copy(faceUp = true) }
            phase = BjPhase.RESOLVED
            statusText = "Bust ($pv)! Dealer takes the pot."
            onPlayCue(SoundCue.ERROR)
            onRecordOutcome(MatchOutcome.LOSS, "Dealer", "Busted at $pv", chips, 15)
        } else if (pv == 21) {
            resolveDealer()
        } else {
            statusText = "Your hand: $pv"
        }
    }

    fun doubleDown() {
        if (phase != BjPhase.PLAYER_TURN || playerHand.size != 2 || chips < bet || deck.isEmpty()) return
        chips -= bet
        bet *= 2
        val next = playerHand + deck.first()
        playerHand = next
        deck = deck.drop(1)
        if (handValue(next) > 21) {
            dealerHand = dealerHand.map { it.copy(faceUp = true) }
            phase = BjPhase.RESOLVED
            statusText = "Bust (${handValue(next)})!"
            onPlayCue(SoundCue.ERROR)
            bet /= 2
        } else {
            resolveDealer()
            bet /= 2
        }
    }

    fun resetBankroll() {
        chips = 500; bet = 25; playerHand = emptyList(); dealerHand = emptyList()
        phase = BjPhase.BETTING; statusText = "Bankroll reset to 500 chips."
    }

    GameScaffold(
        game = game,
        subtitle = "Offline Chips • Dealer Stands on 17",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::resetBankroll
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMetricTile(label = "CHIPS", value = chips.toString(), modifier = Modifier.weight(1f))
            StatMetricTile(label = "WAGER", value = bet.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))

        MonoCard(elevated = true, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DEALER (${if (dealerHand.isEmpty()) 0 else handValue(dealerHand)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dealerHand.forEach { PlayingCardView(it, width = 62.dp, height = 88.dp) }
                    }
                }

                Text(statusText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))

                Column {
                    Text("YOUR HAND (${handValue(playerHand)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        playerHand.forEach { PlayingCardView(it, width = 62.dp, height = 88.dp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (phase != BjPhase.PLAYER_TURN) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 25, 50, 100).forEach { amt ->
                    OutlinedButton(onClick = { bet = amt }, modifier = Modifier.weight(1f)) {
                        Text("$amt")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::dealRound, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Deal Hand (Bet $bet)")
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = ::hit, modifier = Modifier.weight(1f).height(50.dp)) { Text("Hit") }
                Button(onClick = ::resolveDealer, modifier = Modifier.weight(1f).height(50.dp)) { Text("Stand") }
                OutlinedButton(
                    onClick = ::doubleDown,
                    enabled = playerHand.size == 2 && chips >= bet,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Double") }
            }
        }
    }
}
