package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun MancalaScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("mancala") }
    // Indices: 0..5 = P1 pits (L->R), 6 = P1 Store, 7..12 = P2 pits (R->L visually), 13 = P2 Store
    var pits by remember { mutableStateOf(List(14) { if (it == 6 || it == 13) 0 else 4 }) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Tap one of your bottom pits to sow seeds.") }
    var gameOver by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }

    fun sow(pitIdx: Int) {
        if (gameOver) return
        val next = pits.toMutableList()
        var seeds = next[pitIdx]
        if (seeds <= 0) return
        next[pitIdx] = 0
        var cursor = pitIdx
        val skipStore = if (activeSeat == 0) 13 else 6
        while (seeds > 0) {
            cursor = (cursor + 1) % 14
            if (cursor == skipStore) continue
            next[cursor]++
            seeds--
        }

        val myStore = if (activeSeat == 0) 6 else 13
        val ownRange = if (activeSeat == 0) 0..5 else 7..12

        var extraTurn = cursor == myStore
        if (!extraTurn && cursor in ownRange && next[cursor] == 1) {
            val opposite = 12 - cursor
            if (next[opposite] > 0) {
                next[myStore] += next[opposite] + 1
                next[opposite] = 0
                next[cursor] = 0
                onPlayCue(SoundCue.CAPTURE)
                statusText = "${sessionConfig.players[activeSeat].name} captured opposite seeds!"
            } else {
                onPlayCue(SoundCue.WOOD_TAP)
            }
        } else {
            onPlayCue(if (extraTurn) SoundCue.BOUNCE else SoundCue.WOOD_TAP)
            statusText = if (extraTurn) "${sessionConfig.players[activeSeat].name} earned an extra turn!" else "${sessionConfig.players[1 - activeSeat].name}'s turn"
        }

        // Check game end
        val p1Empty = (0..5).all { next[it] == 0 }
        val p2Empty = (7..12).all { next[it] == 0 }
        if (p1Empty || p2Empty) {
            for (i in 0..5) { next[6] += next[i]; next[i] = 0 }
            for (i in 7..12) { next[13] += next[i]; next[i] = 0 }
            pits = next
            gameOver = true
        } else {
            pits = next
            if (!extraTurn) activeSeat = 1 - activeSeat
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && !gameOver
    LaunchedEffect(pits, activeSeat, isAiTurn) {
        if (isAiTurn) {
            delay(450L)
            val valid = (7..12).filter { pits[it] > 0 }
            if (valid.isNotEmpty()) {
                val extraMove = valid.firstOrNull { (it + pits[it]) % 14 == 13 }
                sow(extraMove ?: valid.maxBy { pits[it] })
            }
        }
    }

    LaunchedEffect(gameOver) {
        if (gameOver && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val s1 = pits[6]; val s2 = pits[13]
            when {
                s1 > s2 -> onRecordOutcome(MatchOutcome.WIN, sessionConfig.players[0].name, "$s1 – $s2 seeds", s1 * 10, 60)
                s2 > s1 -> onRecordOutcome(if (sessionConfig.players[1].isAi) MatchOutcome.LOSS else MatchOutcome.WIN, sessionConfig.players[1].name, "$s2 – $s1 seeds", s2 * 10, 60)
                else -> onRecordOutcome(MatchOutcome.DRAW, "Draw", "Tied $s1 – $s2", s1 * 10, 60)
            }
        }
    }

    fun restart() {
        pits = List(14) { if (it == 6 || it == 13) 0 else 4 }
        activeSeat = 0
        gameOver = false
        recorded = false
        statusText = "Tap one of your pits to sow seeds."
    }

    GameScaffold(
        game = game,
        subtitle = if (sessionConfig.playMode == PlayMode.VS_AI) "vs AI" else "Pass & Play",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = listOf(pits[6], pits[13]),
            statusMessage = statusText
        )
        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(230.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // P2 Store (left)
                MancalaStore(label = "P2", count = pits[13], active = activeSeat == 1)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: P2 pits 12 down to 7
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (idx in 12 downTo 7) {
                            val canTap = activeSeat == 1 && !isAiTurn && pits[idx] > 0 && !gameOver
                            MancalaPit(
                                count = pits[idx],
                                enabled = canTap,
                                highlighted = activeSeat == 1 && pits[idx] > 0,
                                onClick = { sow(idx) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Bottom row: P1 pits 0..5
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (idx in 0..5) {
                            val canTap = activeSeat == 0 && pits[idx] > 0 && !gameOver
                            MancalaPit(
                                count = pits[idx],
                                enabled = canTap,
                                highlighted = activeSeat == 0 && pits[idx] > 0,
                                onClick = { sow(idx) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // P1 Store (right)
                MancalaStore(label = "P1", count = pits[6], active = activeSeat == 0)
            }
        }
    }

    if (gameOver) {
        val s1 = pits[6]; val s2 = pits[13]
        val title = when {
            s1 > s2 -> "${sessionConfig.players[0].name} Wins!"
            s2 > s1 -> "${sessionConfig.players[1].name} Wins!"
            else -> "Match Draw!"
        }
        GameCompletionDialog(
            title = title,
            subtitle = "Final seed count: $s1 – $s2",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}

@Composable
private fun MancalaStore(label: String, count: Int, active: Boolean) {
    Surface(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .pulseEffect(active, maxScale = 1.03f)
            .springPop(count, popScale = 1.08f),
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            AnimatedCounterText(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MancalaPit(
    count: Int,
    enabled: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .bouncyPress(enabled)
            .springPop(count, popScale = 1.10f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedCounterText(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
