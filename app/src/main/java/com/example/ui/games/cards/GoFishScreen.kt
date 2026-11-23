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
import androidx.compose.material3.FilterChip
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
import com.example.ui.components.bouncyPress
import com.example.ui.components.springPop
import kotlinx.coroutines.delay

@Composable
fun GoFishScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("go_fish") }
    val pCount = sessionConfig.players.size

    fun stripBooks(hand: List<PlayingCard>): Pair<List<PlayingCard>, Int> {
        val grouped = hand.groupBy { it.rank }
        var books = 0
        val rem = mutableListOf<PlayingCard>()
        grouped.forEach { (_, cards) ->
            if (cards.size == 4) books++ else rem.addAll(cards)
        }
        return rem.sortedBy { it.rank.value } to books
    }

    fun deal(): Pair<List<List<PlayingCard>>, List<PlayingCard>> {
        val deck = standardDeck(faceUp = true).shuffled()
        val perHand = if (pCount == 2) 7 else 5
        val hands = (0 until pCount).map { idx -> deck.subList(idx * perHand, (idx + 1) * perHand).sortedBy { it.rank.value } }
        return hands to deck.drop(pCount * perHand)
    }

    var dealt by remember(pCount) { mutableStateOf(deal()) }
    var hands by remember(dealt) { mutableStateOf(dealt.first) }
    var pond by remember(dealt) { mutableStateOf(dealt.second) }
    val books = remember(pCount) { mutableStateListOf<Int>().apply { repeat(pCount) { add(0) } } }
    var activeSeat by remember { mutableIntStateOf(0) }
    var targetSeat by remember { mutableIntStateOf(1) }
    var selectedRank by remember { mutableStateOf<Rank?>(null) }
    var showCurtain by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Pick a rank from your hand to ask an opponent.") }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun finishCheck(updatedHands: List<List<PlayingCard>>, updatedPond: List<PlayingCard>) {
        if (books.sum() >= 13 || (updatedPond.isEmpty() && updatedHands.all { it.isEmpty() })) {
            val best = books.maxOrNull() ?: 0
            winnerSeat = books.indexOf(best)
            onPlayCue(SoundCue.WIN)
        }
    }

    fun askForRank(targetIdx: Int, rank: Rank) {
        if (winnerSeat != null) return
        val matching = hands[targetIdx].filter { it.rank == rank }
        val nextHands = hands.toMutableList()
        var nextPond = pond

        if (matching.isNotEmpty()) {
            nextHands[targetIdx] = nextHands[targetIdx] - matching.toSet()
            val (cleaned, gained) = stripBooks(nextHands[activeSeat] + matching)
            nextHands[activeSeat] = cleaned
            books[activeSeat] += gained
            onPlayCue(if (gained > 0) SoundCue.TILE_MERGE else SoundCue.BOUNCE)
            statusText = "${sessionConfig.players[activeSeat].name} got ${matching.size}× ${rank.label} from ${sessionConfig.players[targetIdx].name}!"
            hands = nextHands
            selectedRank = cleaned.firstOrNull()?.rank
            finishCheck(nextHands, nextPond)
        } else {
            // Go Fish!
            onPlayCue(SoundCue.SPLASH)
            if (nextPond.isNotEmpty()) {
                val drawn = nextPond.first()
                nextPond = nextPond.drop(1)
                val (cleaned, gained) = stripBooks(nextHands[activeSeat] + drawn)
                nextHands[activeSeat] = cleaned
                books[activeSeat] += gained
                statusText = "Go Fish! ${sessionConfig.players[activeSeat].name} drew from pond."
            } else {
                statusText = "Go Fish! Pond is empty."
            }
            hands = nextHands
            pond = nextPond
            finishCheck(nextHands, nextPond)
            if (winnerSeat == null) {
                var nextSeat = (activeSeat + 1) % pCount
                // Skip empty hands if pond empty
                repeat(pCount) {
                    if (nextHands[nextSeat].isEmpty() && nextPond.isNotEmpty()) {
                        nextHands[nextSeat] = listOf(nextPond.first())
                        nextPond = nextPond.drop(1)
                    }
                    if (nextHands[nextSeat].isNotEmpty()) return@repeat
                    nextSeat = (nextSeat + 1) % pCount
                }
                hands = nextHands
                pond = nextPond
                activeSeat = nextSeat
                targetSeat = (nextSeat + 1) % pCount
                selectedRank = hands[nextSeat].firstOrNull()?.rank
                if (sessionConfig.privacyMode && !sessionConfig.players[nextSeat].isAi) showCurtain = true
            }
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, hands, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi) {
            delay(500L)
            if (hands[activeSeat].isEmpty() && pond.isNotEmpty()) {
                val drawn = pond.first()
                pond = pond.drop(1)
                hands = hands.mapIndexed { i, h -> if (i == activeSeat) listOf(drawn) else h }
            }
            val rankPick = hands[activeSeat].randomOrNull()?.rank
            val oppPick = (0 until pCount).filter { it != activeSeat && hands[it].isNotEmpty() }.randomOrNull()
                ?: ((activeSeat + 1) % pCount)
            if (rankPick != null) askForRank(oppPick, rankPick)
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} collected ${books[w]} books", books[w] * 25, 90)
        }
    }

    fun restart() {
        dealt = deal(); for (i in books.indices) books[i] = 0
        activeSeat = 0; targetSeat = 1; selectedRank = null; showCurtain = false
        winnerSeat = null; recorded = false; statusText = "Pick a rank from your hand to ask an opponent."
        onPlayCue(SoundCue.SHUFFLE)
    }

    GameScaffold(
        game = game,
        subtitle = "Pond: ${pond.size} • Books: ${books.sum()}/13",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showCurtain && !activePlayer.isAi) {
            PrivacyCurtainOverlay(
                nextPlayer = activePlayer,
                reason = "Your Go Fish hand is hidden from other players.",
                onRevealClick = { showCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = books.toList(),
                statusMessage = statusText
            )
            Spacer(Modifier.height(12.dp))

            MonoCard(elevated = true) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ask Opponent:", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sessionConfig.players.forEachIndexed { idx, p ->
                            if (idx != activeSeat) {
                                FilterChip(
                                    selected = targetSeat == idx,
                                    onClick = { targetSeat = idx; onPlayCue(SoundCue.KEY_TICK) },
                                    label = { Text("${p.name} (${hands[idx].size})") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("${activePlayer.name}'s Hand (Tap a card to select rank)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hands[activeSeat].forEach { card ->
                    PlayingCardView(
                        card = card,
                        selected = selectedRank == card.rank,
                        width = 58.dp,
                        height = 84.dp,
                        onClick = { if (!activePlayer.isAi) { selectedRank = card.rank; onPlayCue(SoundCue.KEY_TICK) } }
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val r = selectedRank ?: hands[activeSeat].firstOrNull()?.rank
                    if (r != null) askForRank(targetSeat, r)
                },
                enabled = !activePlayer.isAi && hands[activeSeat].isNotEmpty() && winnerSeat == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bouncyPress(enabled = !activePlayer.isAi && hands[activeSeat].isNotEmpty() && winnerSeat == null)
                    .springPop(selectedRank, popScale = 1.04f)
            ) {
                Text("Ask ${sessionConfig.players[targetSeat].name} for ${selectedRank?.label ?: "Rank"}")
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins Go Fish!",
            subtitle = "Collected ${books[winnerSeat!!]} books.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
