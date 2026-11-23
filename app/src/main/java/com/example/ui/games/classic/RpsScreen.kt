package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.PrivacyCurtainOverlay

private enum class RpsChoice(val label: String, val icon: ImageVector) {
    ROCK("Rock", Icons.Rounded.RadioButtonChecked),
    PAPER("Paper", Icons.Rounded.Description),
    SCISSORS("Scissors", Icons.Rounded.ContentCut)
}

@Composable
fun RpsScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("rps") }
    val targetWins = remember(sessionConfig.ruleVariant) {
        when {
            sessionConfig.ruleVariant.contains("5") -> 3
            sessionConfig.ruleVariant.contains("7") -> 4
            else -> 2
        }
    }

    val scores = remember { mutableStateListOf(0, 0) }
    var p1Pick by remember { mutableStateOf<RpsChoice?>(null) }
    var p2Pick by remember { mutableStateOf<RpsChoice?>(null) }
    var showCurtain by remember { mutableStateOf(false) }
    var roundResult by remember { mutableStateOf<String?>(null) }
    var matchWinner by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun resolveRound(c1: RpsChoice, c2: RpsChoice) {
        p1Pick = c1
        p2Pick = c2
        when {
            c1 == c2 -> {
                roundResult = "Tie — Both chose ${c1.label}"
                onPlayCue(SoundCue.MOVE)
            }
            (c1 == RpsChoice.ROCK && c2 == RpsChoice.SCISSORS) ||
                (c1 == RpsChoice.PAPER && c2 == RpsChoice.ROCK) ||
                (c1 == RpsChoice.SCISSORS && c2 == RpsChoice.PAPER) -> {
                scores[0]++
                roundResult = "${sessionConfig.players[0].name} wins round (${c1.label} beats ${c2.label})"
                onPlayCue(SoundCue.TURN_PASS)
            }
            else -> {
                scores[1]++
                roundResult = "${sessionConfig.players[1].name} wins round (${c2.label} beats ${c1.label})"
                onPlayCue(SoundCue.CAPTURE)
            }
        }
        if (scores[0] >= targetWins) matchWinner = 0
        else if (scores[1] >= targetWins) matchWinner = 1
    }

    LaunchedEffect(matchWinner) {
        val w = matchWinner
        if (w != null && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val wp = sessionConfig.players[w]
            val outcome = if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
            onRecordOutcome(outcome, wp.name, "${wp.name} won ${scores[0]}–${scores[1]}", scores[w] * 50, 20)
        }
    }

    fun restart() {
        scores[0] = 0; scores[1] = 0
        p1Pick = null; p2Pick = null
        showCurtain = false; roundResult = null; matchWinner = null; recorded = false
    }

    val activeSeat = if (p1Pick == null) 0 else 1

    GameScaffold(
        game = game,
        subtitle = "${sessionConfig.ruleVariant} • First to $targetWins",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showCurtain) {
            PrivacyCurtainOverlay(
                nextPlayer = sessionConfig.players[1],
                reason = "${sessionConfig.players[0].name} has locked in a secret choice.",
                onRevealClick = { showCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = scores.toList()
            )
            Spacer(Modifier.height(20.dp))

            MonoCard(elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (roundResult != null) {
                        Text("${p1Pick?.label}  vs  ${p2Pick?.label}", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(roundResult!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            p1Pick = null; p2Pick = null; roundResult = null
                        }) {
                            Text("Next Round")
                        }
                    } else {
                        Text(
                            text = "${sessionConfig.players[activeSeat].name}, choose your move",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (roundResult == null && matchWinner == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RpsChoice.entries.forEach { choice ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (sessionConfig.playMode == PlayMode.VS_AI) {
                                        resolveRound(choice, RpsChoice.entries.random())
                                    } else if (p1Pick == null) {
                                        p1Pick = choice
                                        if (sessionConfig.privacyMode) showCurtain = true
                                    } else {
                                        resolveRound(p1Pick!!, choice)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(choice.icon, contentDescription = choice.label, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(choice.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (matchWinner != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[matchWinner!!].name} Wins!",
            subtitle = "Final score: ${scores[0]} – ${scores[1]}",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
