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

@Composable
fun WarScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("war") }

    fun splitDeck(): Pair<List<PlayingCard>, List<PlayingCard>> {
        val d = standardDeck(faceUp = true).shuffled()
        return d.take(26) to d.drop(26)
    }

    var split by remember { mutableStateOf(splitDeck()) }
    var p1Deck by remember(split) { mutableStateOf(split.first) }
    var p2Deck by remember(split) { mutableStateOf(split.second) }
    var p1Card by remember { mutableStateOf<PlayingCard?>(null) }
    var p2Card by remember { mutableStateOf<PlayingCard?>(null) }
    var rounds by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Tap Flip Cards to clash!") }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun rankPower(c: PlayingCard): Int = if (c.rank == Rank.ACE) 14 else c.rank.value

    fun flipClash() {
        if (winnerSeat != null || p1Deck.isEmpty() || p2Deck.isEmpty()) return
        rounds++
        var d1 = p1Deck.toMutableList()
        var d2 = p2Deck.toMutableList()
        val pot = mutableListOf<PlayingCard>()

        while (d1.isNotEmpty() && d2.isNotEmpty()) {
            val c1 = d1.removeAt(0)
            val c2 = d2.removeAt(0)
            p1Card = c1; p2Card = c2
            pot.add(c1); pot.add(c2)
            val v1 = rankPower(c1); val v2 = rankPower(c2)
            if (v1 > v2) {
                d1.addAll(pot.shuffled())
                statusText = "${sessionConfig.players[0].name} wins ${pot.size} cards (${c1.label} > ${c2.label})"
                onPlayCue(SoundCue.MOVE)
                break
            } else if (v2 > v1) {
                d2.addAll(pot.shuffled())
                statusText = "${sessionConfig.players[1].name} wins ${pot.size} cards (${c2.label} > ${c1.label})"
                onPlayCue(SoundCue.CAPTURE)
                break
            } else {
                // WAR! Burn up to 2 cards each and draw again
                onPlayCue(SoundCue.TURN_PASS)
                repeat(2) {
                    if (d1.size > 1) pot.add(d1.removeAt(0))
                    if (d2.size > 1) pot.add(d2.removeAt(0))
                }
            }
        }
        p1Deck = d1
        p2Deck = d2
        if (d1.isEmpty()) winnerSeat = 1
        else if (d2.isEmpty()) winnerSeat = 0
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "Won all 52 cards in $rounds flips", 100, rounds)
        }
    }

    fun restart() {
        split = splitDeck(); p1Card = null; p2Card = null; rounds = 0; winnerSeat = null; recorded = false
        statusText = "Tap Flip Cards to clash!"
    }

    GameScaffold(
        game = game,
        subtitle = "Rounds: $rounds",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = 0,
            scores = listOf(p1Deck.size, p2Deck.size),
            statusMessage = statusText
        )
        Spacer(Modifier.height(20.dp))

        MonoCard(elevated = true, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sessionConfig.players[0].name, style = MaterialTheme.typography.titleMedium)
                    PlayingCardView(p1Card, width = 88.dp, height = 124.dp)
                    Text("${p1Deck.size} cards", style = MaterialTheme.typography.labelSmall)
                }
                Text("VS", style = MaterialTheme.typography.headlineMedium)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sessionConfig.players[1].name, style = MaterialTheme.typography.titleMedium)
                    PlayingCardView(p2Card, width = 88.dp, height = 124.dp)
                    Text("${p2Deck.size} cards", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = ::flipClash,
            enabled = winnerSeat == null,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Flip Cards") }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins War!",
            subtitle = "Captured all 52 cards in $rounds clashes.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
