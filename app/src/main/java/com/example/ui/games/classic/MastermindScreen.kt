package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily

private data class MastermindGuess(
    val code: List<Int>,
    val exact: Int,
    val partial: Int
)

private val pegSymbols = listOf("▲", "●", "■", "◆", "★", "✚")
private val pegColors = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF4FC3F7),
    Color(0xFF81C784),
    Color(0xFFFFB74D),
    Color(0xFFE57373),
    Color(0xFFBA68C8)
)

@Composable
fun MastermindScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("mastermind") }
    val isTwoPlayer = sessionConfig.playMode == PlayMode.PASS_AND_PLAY

    var secretCode by remember(isTwoPlayer) {
        mutableStateOf(if (isTwoPlayer) emptyList() else List(4) { (0..5).random() })
    }
    var setterDraft by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
    val guesses = remember { mutableStateListOf<MastermindGuess>() }
    val currentDraft = remember { mutableStateListOf(0, 0, 0, 0) }
    var won by remember { mutableStateOf(false) }
    var lost by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }

    fun evaluate(guess: List<Int>, secret: List<Int>): Pair<Int, Int> {
        var exact = 0
        val remS = mutableListOf<Int>()
        val remG = mutableListOf<Int>()
        for (i in 0..3) {
            if (guess[i] == secret[i]) exact++
            else { remS.add(secret[i]); remG.add(guess[i]) }
        }
        var partial = 0
        remG.forEach { g ->
            val idx = remS.indexOf(g)
            if (idx != -1) { partial++; remS.removeAt(idx) }
        }
        return exact to partial
    }

    LaunchedEffect(won, lost) {
        if ((won || lost) && !recorded) {
            recorded = true
            onPlayCue(if (won) SoundCue.WIN else SoundCue.ERROR)
            onRecordOutcome(
                if (won) MatchOutcome.WIN else MatchOutcome.LOSS,
                if (won) sessionConfig.players.last().name else "Code",
                if (won) "Cracked in ${guesses.size} attempts" else "Failed to crack code",
                if (won) (11 - guesses.size) * 25 else 0,
                40
            )
        }
    }

    fun restart() {
        secretCode = if (isTwoPlayer) emptyList() else List(4) { (0..5).random() }
        guesses.clear()
        for (i in 0..3) currentDraft[i] = 0
        won = false; lost = false; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "10 Attempts • 6 Symbols",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (secretCode.isEmpty()) {
            MonoCard(elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${sessionConfig.players.first().name}: Set Secret 4-Peg Code", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        setterDraft.forEachIndexed { idx, peg ->
                            PegToken(peg = peg, onClick = {
                                setterDraft = setterDraft.toMutableList().apply { this[idx] = (peg + 1) % 6 }
                            })
                        }
                    }
                    Button(
                        onClick = { secretCode = setterDraft },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Lock Code & Pass Device") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(guesses) { idx, g ->
                    MonoCard(elevated = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMonoFontFamily)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                g.code.forEach { PegToken(peg = it, onClick = null) }
                            }
                            Text(
                                text = "●${g.exact}  ○${g.partial}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            MonoCard(elevated = true, borderHighlight = true) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (i in 0..3) {
                            PegToken(
                                peg = currentDraft[i],
                                onClick = {
                                    if (!won && !lost) {
                                        currentDraft[i] = (currentDraft[i] + 1) % 6
                                        onPlayCue(SoundCue.KEY_TICK)
                                    }
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (won || lost) return@Button
                            val attempt = currentDraft.toList()
                            val (ex, pa) = evaluate(attempt, secretCode)
                            guesses.add(MastermindGuess(attempt, ex, pa))
                            onPlayCue(if (ex > 0) SoundCue.TILE_MERGE else SoundCue.WOOD_TAP)
                            if (ex == 4) won = true
                            else if (guesses.size >= 10) lost = true
                        },
                        enabled = !won && !lost,
                        modifier = Modifier.fillMaxWidth().height(48.dp).bouncyPress(enabled = !won && !lost)
                    ) {
                        Text("Submit Guess (${guesses.size}/10) — Tap pegs to cycle")
                    }
                }
            }
        }
    }

    if (won || lost) {
        GameCompletionDialog(
            title = if (won) "Code Cracked!" else "Out of Attempts",
            subtitle = "Secret: ${secretCode.joinToString(" ") { pegSymbols[it] }}",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}

@Composable
private fun PegToken(peg: Int, onClick: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .springPop(peg, popScale = 1.18f)
            .then(if (onClick != null) Modifier.bouncyPress() else Modifier)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = pegColors[peg].copy(alpha = 0.2f),
        border = BorderStroke(1.5.dp, pegColors[peg])
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(pegSymbols[peg], color = pegColors[peg], fontWeight = FontWeight.Bold)
        }
    }
}
