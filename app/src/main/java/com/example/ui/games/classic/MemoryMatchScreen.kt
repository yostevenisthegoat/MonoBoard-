package com.example.ui.games.classic

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.FlipCardBox
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val memoryIcons: List<ImageVector> = listOf(
    Icons.Rounded.AutoAwesome,
    Icons.Rounded.Bolt,
    Icons.Rounded.Diamond,
    Icons.Rounded.Hexagon,
    Icons.Rounded.Shield,
    Icons.Rounded.Star,
    Icons.Rounded.AcUnit,
    Icons.Rounded.WbSunny,
    Icons.Rounded.Favorite,
    Icons.Rounded.Eco,
    Icons.Rounded.Explore,
    Icons.Rounded.ChangeHistory
)

private data class MemoryCard(
    val id: Int,
    val pairId: Int,
    val faceUp: Boolean = false,
    val matched: Boolean = false
)

@Composable
fun MemoryMatchScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("memory_match") }
    val isLarge = sessionConfig.ruleVariant.contains("24") || sessionConfig.ruleVariant.contains("12")
    val pairCount = if (isLarge) 12 else 8
    val cols = 4
    val rows = if (isLarge) 6 else 4
    val scope = rememberCoroutineScope()

    fun buildDeck(): List<MemoryCard> {
        val ids = (0 until pairCount).flatMap { listOf(it, it) }.shuffled()
        return ids.mapIndexed { idx, pId -> MemoryCard(id = idx, pairId = pId) }
    }

    var cards by remember(pairCount) { mutableStateOf(buildDeck()) }
    var firstPick by remember { mutableStateOf<Int?>(null) }
    var lockInput by remember { mutableStateOf(false) }
    var activeSeat by remember { mutableIntStateOf(0) }
    val scores = remember(sessionConfig.players.size) {
        mutableStateListOf<Int>().apply { repeat(sessionConfig.players.size) { add(0) } }
    }
    var moves by remember { mutableIntStateOf(0) }
    var mismatchShakeCount by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val allMatched = cards.all { it.matched }

    LaunchedEffect(allMatched) {
        while (!allMatched) {
            delay(1000L)
            elapsed++
        }
    }

    LaunchedEffect(allMatched) {
        if (allMatched && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val maxScore = scores.maxOrNull() ?: 0
            val winnerIdx = scores.indexOf(maxScore).coerceAtLeast(0)
            val wp = sessionConfig.players[winnerIdx]
            onRecordOutcome(MatchOutcome.WIN, wp.name, "Cleared $pairCount pairs in $moves moves", (200 - moves * 4).coerceAtLeast(50), elapsed)
        }
    }

    fun flipCard(index: Int) {
        if (lockInput || cards[index].faceUp || cards[index].matched) return
        val next = cards.toMutableList()
        next[index] = next[index].copy(faceUp = true)
        cards = next
        onPlayCue(SoundCue.CARD_FLIP)

        val first = firstPick
        if (first == null) {
            firstPick = index
        } else {
            moves++
            if (cards[first].pairId == cards[index].pairId) {
                val matched = cards.toMutableList()
                matched[first] = matched[first].copy(matched = true)
                matched[index] = matched[index].copy(matched = true)
                cards = matched
                scores[activeSeat]++
                firstPick = null
                onPlayCue(SoundCue.TILE_MERGE)
            } else {
                lockInput = true
                scope.launch {
                    delay(280L)
                    mismatchShakeCount++
                    onPlayCue(SoundCue.ERROR)
                    delay(420L)
                    val reset = cards.toMutableList()
                    reset[first] = reset[first].copy(faceUp = false)
                    reset[index] = reset[index].copy(faceUp = false)
                    cards = reset
                    firstPick = null
                    if (sessionConfig.players.size > 1) {
                        activeSeat = (activeSeat + 1) % sessionConfig.players.size
                    }
                    lockInput = false
                }
            }
        }
    }

    fun restart() {
        cards = buildDeck()
        firstPick = null; lockInput = false; activeSeat = 0
        for (i in scores.indices) scores[i] = 0
        moves = 0; mismatchShakeCount = 0; elapsed = 0; recorded = false
        onPlayCue(SoundCue.SHUFFLE)
    }

    GameScaffold(
        game = game,
        subtitle = "$pairCount Pairs • $moves Moves",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (sessionConfig.players.size > 1) {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = scores.toList()
            )
            Spacer(Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shakeOnTrigger(mismatchShakeCount),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0 until cols) {
                        val idx = r * cols + c
                        val card = cards[idx]
                        FlipCardBox(
                            faceUp = card.faceUp || card.matched,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .bouncyPress(enabled = !lockInput && !card.faceUp && !card.matched)
                                .springPop(if (card.matched) card.id else null, popScale = 1.18f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { flipCard(idx) },
                            front = {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (card.matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = memoryIcons[card.pairId % memoryIcons.size],
                                            contentDescription = "Card symbol",
                                            tint = if (card.matched) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            back = {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text("?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (allMatched) {
        val maxScore = scores.maxOrNull() ?: 0
        val wp = sessionConfig.players[scores.indexOf(maxScore).coerceAtLeast(0)]
        GameCompletionDialog(
            title = if (sessionConfig.players.size > 1) "${wp.name} Wins!" else "All Pairs Matched!",
            subtitle = "Completed in $moves moves (%02d:%02d).".format(elapsed / 60, elapsed % 60),
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
