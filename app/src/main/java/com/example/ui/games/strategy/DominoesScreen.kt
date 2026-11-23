package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

private data class DominoTile(val a: Int, val b: Int) {
    val pips: Int get() = a + b
    fun flipped() = DominoTile(b, a)
}

@Composable
fun DominoesScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("dominoes") }
    val playerCount = sessionConfig.players.size

    fun dealAll(): Pair<List<List<DominoTile>>, List<DominoTile>> {
        val deck = buildList {
            for (i in 0..6) for (j in i..6) add(DominoTile(i, j))
        }.shuffled()
        val handSize = if (playerCount <= 2) 7 else 5
        val hands = (0 until playerCount).map { p -> deck.subList(p * handSize, (p + 1) * handSize) }
        val boneyard = deck.drop(playerCount * handSize)
        return hands to boneyard
    }

    var deal by remember(playerCount) { mutableStateOf(dealAll()) }
    var hands by remember(deal) { mutableStateOf(deal.first) }
    var boneyard by remember(deal) { mutableStateOf(deal.second) }
    var chain by remember(deal) { mutableStateOf<List<DominoTile>>(emptyList()) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var consecutivePasses by remember { mutableIntStateOf(0) }
    var showCurtain by remember { mutableStateOf(false) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var statusText by remember { mutableStateOf("Play any tile to open the line.") }
    var recorded by remember { mutableStateOf(false) }

    val leftEnd = chain.firstOrNull()?.a
    val rightEnd = chain.lastOrNull()?.b

    fun canPlayTile(tile: DominoTile): Boolean {
        if (chain.isEmpty()) return true
        return tile.a == leftEnd || tile.b == leftEnd || tile.a == rightEnd || tile.b == rightEnd
    }

    fun playTile(tile: DominoTile, placeLeft: Boolean = true) {
        if (winnerSeat != null) return
        val nextChain = if (chain.isEmpty()) {
            listOf(tile)
        } else if (placeLeft && (tile.a == leftEnd || tile.b == leftEnd)) {
            val oriented = if (tile.b == leftEnd) tile else tile.flipped()
            listOf(oriented) + chain
        } else if (tile.a == rightEnd || tile.b == rightEnd) {
            val oriented = if (tile.a == rightEnd) tile else tile.flipped()
            chain + oriented
        } else if (tile.a == leftEnd || tile.b == leftEnd) {
            val oriented = if (tile.b == leftEnd) tile else tile.flipped()
            listOf(oriented) + chain
        } else return

        chain = nextChain
        val newHands = hands.mapIndexed { idx, list -> if (idx == activeSeat) list - tile else list }
        hands = newHands
        consecutivePasses = 0
        onPlayCue(SoundCue.MOVE)

        if (newHands[activeSeat].isEmpty()) {
            winnerSeat = activeSeat
            onPlayCue(SoundCue.WIN)
        } else {
            val nextSeat = (activeSeat + 1) % playerCount
            activeSeat = nextSeat
            statusText = "${sessionConfig.players[nextSeat].name}'s turn"
            if (sessionConfig.privacyMode && !sessionConfig.players[nextSeat].isAi) {
                showCurtain = true
            }
        }
    }

    fun drawOrPass() {
        if (winnerSeat != null) return
        if (boneyard.isNotEmpty()) {
            val drawn = boneyard.first()
            boneyard = boneyard.drop(1)
            hands = hands.mapIndexed { idx, list -> if (idx == activeSeat) list + drawn else list }
            statusText = "${sessionConfig.players[activeSeat].name} drew from boneyard."
            onPlayCue(SoundCue.MOVE)
        } else {
            consecutivePasses++
            onPlayCue(SoundCue.TURN_PASS)
            if (consecutivePasses >= playerCount) {
                // Blocked game: lowest pip sum wins
                winnerSeat = hands.indices.minBy { idx -> hands[idx].sumOf { it.pips } }
                onPlayCue(SoundCue.WIN)
            } else {
                val nextSeat = (activeSeat + 1) % playerCount
                activeSeat = nextSeat
                statusText = "${sessionConfig.players[nextSeat].name}'s turn (previous passed)"
                if (sessionConfig.privacyMode && !sessionConfig.players[nextSeat].isAi) showCurtain = true
            }
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, hands, boneyard, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi) {
            delay(480L)
            val playable = hands[activeSeat].filter { canPlayTile(it) }
            if (playable.isNotEmpty()) {
                val pick = playable.maxBy { it.pips }
                playTile(pick)
            } else {
                drawOrPass()
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            val pts = hands.filterIndexed { idx, _ -> idx != w }.flatten().sumOf { it.pips }
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} scored $pts pips", pts, 80)
        }
    }

    fun restart() {
        deal = dealAll()
        activeSeat = 0; consecutivePasses = 0; showCurtain = false; winnerSeat = null; recorded = false
        statusText = "Play any tile to open the line."
    }

    GameScaffold(
        game = game,
        subtitle = "Boneyard: ${boneyard.size} tiles",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showCurtain && !activePlayer.isAi) {
            PrivacyCurtainOverlay(
                nextPlayer = activePlayer,
                reason = "Your domino hand is hidden from other players.",
                onRevealClick = { showCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = hands.map { it.size },
                statusMessage = "$statusText (Score shown = tiles left)"
            )
            Spacer(Modifier.height(14.dp))

            MonoCard(elevated = true, modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (chain.isEmpty()) "TABLE OPEN" else "OPEN ENDS: [$leftEnd] ... [$rightEnd]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chain.forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "${t.a} | ${t.b}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("${activePlayer.name}'s Hand", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hands[activeSeat].forEach { tile ->
                    val playable = canPlayTile(tile) && !activePlayer.isAi && winnerSeat == null
                    Surface(
                        modifier = Modifier
                            .size(width = 64.dp, height = 84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = playable) { playTile(tile) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (playable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                tile.a.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (playable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("—", color = if (playable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline)
                            Text(
                                tile.b.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (playable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            val hasPlayable = hands[activeSeat].any { canPlayTile(it) }
            OutlinedButton(
                onClick = ::drawOrPass,
                enabled = !hasPlayable && !activePlayer.isAi && winnerSeat == null,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (boneyard.isNotEmpty()) "Draw from Boneyard (${boneyard.size})" else "Pass Turn")
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins Dominoes!",
            subtitle = "Round complete.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
