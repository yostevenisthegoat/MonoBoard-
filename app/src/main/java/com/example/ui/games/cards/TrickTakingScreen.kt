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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.PrivacyCurtainOverlay
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import kotlinx.coroutines.delay

@Composable
fun TrickTakingScreen(
    gameId: String, // "hearts" or "spades"
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val isHearts = gameId == "hearts"
    val game = remember(gameId) { GameCatalog.byId(gameId) }

    fun cardPower(c: PlayingCard): Int = if (c.rank == Rank.ACE) 14 else c.rank.value

    fun deal(): List<List<PlayingCard>> {
        val deck = standardDeck(faceUp = true).shuffled()
        return (0..3).map { s ->
            deck.subList(s * 13, (s + 1) * 13).sortedWith(compareBy<PlayingCard> { it.suit.ordinal }.thenBy { cardPower(it) })
        }
    }

    var hands by remember { mutableStateOf(deal()) }
    val scores = remember { mutableStateListOf(0, 0, 0, 0) }
    var currentTrick by remember { mutableStateOf<List<Pair<Int, PlayingCard>>>(emptyList()) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var heartsBroken by remember { mutableStateOf(false) }
    var showCurtain by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Lead a card to start the trick.") }
    var gameOver by remember { mutableStateOf(false) }
    var queenShakeCount by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    fun legalCards(hand: List<PlayingCard>): List<PlayingCard> {
        if (currentTrick.isEmpty()) {
            if (isHearts && !heartsBroken) {
                val nonHearts = hand.filter { it.suit != Suit.HEARTS }
                if (nonHearts.isNotEmpty()) return nonHearts
            }
            return hand
        }
        val leadSuit = currentTrick.first().second.suit
        val matching = hand.filter { it.suit == leadSuit }
        return matching.ifEmpty { hand }
    }

    fun trickWinner(trick: List<Pair<Int, PlayingCard>>): Int {
        val leadSuit = trick.first().second.suit
        if (!isHearts) {
            // Spades trump
            val spades = trick.filter { it.second.suit == Suit.SPADES }
            if (spades.isNotEmpty()) return spades.maxBy { cardPower(it.second) }.first
        }
        return trick.filter { it.second.suit == leadSuit }.maxBy { cardPower(it.second) }.first
    }

    fun playCard(card: PlayingCard) {
        if (gameOver) return
        val nextHands = hands.mapIndexed { idx, h -> if (idx == activeSeat) h - card else h }
        val nextTrick = currentTrick + (activeSeat to card)
        val brokeHeartsNow = isHearts && !heartsBroken && card.suit == Suit.HEARTS
        if (card.suit == Suit.HEARTS) heartsBroken = true
        hands = nextHands
        currentTrick = nextTrick
        val isQueenSpades = isHearts && card.suit == Suit.SPADES && card.rank == Rank.QUEEN
        if (isQueenSpades || brokeHeartsNow) {
            queenShakeCount++
            onPlayCue(SoundCue.EXPLOSION)
        } else {
            onPlayCue(SoundCue.CARD_FLIP)
        }

        if (nextTrick.size == 4) {
            val winner = trickWinner(nextTrick)
            val points = if (isHearts) {
                nextTrick.sumOf { (_, c) ->
                    when {
                        c.suit == Suit.HEARTS -> 1
                        c.suit == Suit.SPADES && c.rank == Rank.QUEEN -> 13
                        else -> 0
                    }
                }
            } else {
                10
            }
            scores[winner] += points
            statusText = "${sessionConfig.players[winner].name} won the trick (+$points pts)"
            currentTrick = emptyList()
            activeSeat = winner
            onPlayCue(SoundCue.TILE_MERGE)
            if (nextHands.all { it.isEmpty() }) {
                if (isHearts) {
                    // Shoot the moon check
                    val moonShooter = scores.indexOfFirst { it == 26 }
                    if (moonShooter != -1) {
                        for (i in 0..3) scores[i] = if (i == moonShooter) 0 else 26
                    }
                }
                gameOver = true
                onPlayCue(SoundCue.WIN)
                return
            }
        } else {
            activeSeat = (activeSeat + 1) % 4
        }
        if (sessionConfig.privacyMode && !sessionConfig.players[activeSeat].isAi) {
            showCurtain = true
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, currentTrick, hands, gameOver) {
        if (!gameOver && activePlayer.isAi) {
            delay(420L)
            val legal = legalCards(hands[activeSeat])
            if (legal.isNotEmpty()) {
                val pick = if (isHearts) legal.minBy { cardPower(it) } else legal.maxBy { cardPower(it) }
                playCard(pick)
            }
        }
    }

    val winnerIdx = if (isHearts) scores.indexOf(scores.minOrNull() ?: 0) else scores.indexOf(scores.maxOrNull() ?: 0)

    LaunchedEffect(gameOver) {
        if (gameOver && !recorded) {
            recorded = true
            val wp = sessionConfig.players[winnerIdx]
            onRecordOutcome(
                if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN,
                wp.name,
                "${wp.name} ${if (isHearts) "lowest" else "highest"} score: ${scores[winnerIdx]}",
                scores[winnerIdx],
                140
            )
        }
    }

    fun restart() {
        hands = deal(); for (i in 0..3) scores[i] = 0; currentTrick = emptyList()
        activeSeat = 0; heartsBroken = false; showCurtain = false; gameOver = false; recorded = false
        statusText = "Lead a card to start the trick."
        onPlayCue(SoundCue.SHUFFLE)
    }

    GameScaffold(
        game = game,
        subtitle = if (isHearts) "Avoid Hearts & Q♠" else "Spades are Trump",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showCurtain && !activePlayer.isAi) {
            PrivacyCurtainOverlay(
                nextPlayer = activePlayer,
                reason = "Your hand is hidden while passing the phone.",
                onRevealClick = { showCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = scores.toList(),
                statusMessage = statusText
            )
            Spacer(Modifier.height(14.dp))

            MonoCard(elevated = true, modifier = Modifier.fillMaxWidth().weight(1f).shakeOnTrigger(queenShakeCount)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("CURRENT TRICK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentTrick.forEach { (seat, card) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.springPop(card.label, popScale = 1.15f)
                            ) {
                                PlayingCardView(card, width = 62.dp, height = 88.dp)
                                Text(sessionConfig.players[seat].name.take(8), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            val legal = legalCards(hands[activeSeat]).toSet()
            Text("${activePlayer.name}'s Hand", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hands[activeSeat].forEach { card ->
                    val ok = card in legal && !activePlayer.isAi && !gameOver
                    PlayingCardView(
                        card = card,
                        selected = ok,
                        width = 58.dp,
                        height = 84.dp,
                        onClick = { if (ok) playCard(card) }
                    )
                }
            }
        }
    }

    if (gameOver) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerIdx].name} Wins!",
            subtitle = "Scores: ${scores.joinToString(" • ")}",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
