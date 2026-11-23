package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.theme.JetBrainsMonoFontFamily

private val hangmanDictionary = listOf(
    "ELEPHANT", "PLANET", "SYMPHONY", "ARCHITECT", "LABYRINTH",
    "HORIZON", "PENDULUM", "COMPASS", "QUANTUM", "MONOLITH",
    "GALAXY", "PYRAMID", "STRATEGY", "OBSIDIAN", "CHESSBOARD",
    "MERIDIAN", "CATALYST", "SPECTRUM", "CIPHER", "DOMINO"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HangmanScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("hangman") }
    val isTwoPlayer = sessionConfig.playMode == PlayMode.PASS_AND_PLAY

    var secretWord by remember(isTwoPlayer) {
        mutableStateOf(if (isTwoPlayer) "" else hangmanDictionary.random())
    }
    var customInput by remember { mutableStateOf("") }
    var guessed by remember { mutableStateOf(emptySet<Char>()) }
    var recorded by remember { mutableStateOf(false) }

    val wrongGuesses = remember(secretWord, guessed) {
        if (secretWord.isEmpty()) 0 else guessed.count { it !in secretWord }
    }
    val won = secretWord.isNotEmpty() && secretWord.all { it in guessed }
    val lost = secretWord.isNotEmpty() && wrongGuesses >= 6

    LaunchedEffect(won, lost) {
        if ((won || lost) && !recorded) {
            recorded = true
            onPlayCue(if (won) SoundCue.WIN else SoundCue.ERROR)
            onRecordOutcome(
                if (won) MatchOutcome.WIN else MatchOutcome.LOSS,
                if (won) sessionConfig.players.last().name else "Word",
                "Word: $secretWord ($wrongGuesses wrong)",
                if (won) (6 - wrongGuesses) * 25 else 0,
                30
            )
        }
    }

    fun restart() {
        secretWord = if (isTwoPlayer) "" else hangmanDictionary.random()
        customInput = ""
        guessed = emptySet()
        recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = if (isTwoPlayer) "Pass & Play Secret Word" else "Offline Dictionary",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (secretWord.isEmpty()) {
            MonoCard(elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${sessionConfig.players.first().name}: Enter Secret Word",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Keep the screen hidden from ${sessionConfig.players.getOrNull(1)?.name ?: "Player 2"} while typing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { s -> customInput = s.filter { it.isLetter() }.uppercase().take(12) },
                        label = { Text("Secret Word (3–12 letters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { if (customInput.length >= 3) secretWord = customInput },
                        enabled = customInput.length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lock Word & Pass Device")
                    }
                }
            }
        } else {
            MonoCard(elevated = true) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(170.dp).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ink = MaterialTheme.colorScheme.onSurface
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val w = size.width; val h = size.height
                        // Gallows
                        drawLine(ink, Offset(w * 0.15f, h * 0.92f), Offset(w * 0.85f, h * 0.92f), 6f, StrokeCap.Round)
                        drawLine(ink, Offset(w * 0.3f, h * 0.92f), Offset(w * 0.3f, h * 0.08f), 6f, StrokeCap.Round)
                        drawLine(ink, Offset(w * 0.3f, h * 0.08f), Offset(w * 0.68f, h * 0.08f), 6f, StrokeCap.Round)
                        drawLine(ink, Offset(w * 0.68f, h * 0.08f), Offset(w * 0.68f, h * 0.22f), 5f, StrokeCap.Round)
                        if (wrongGuesses >= 1) drawCircle(ink, radius = w * 0.1f, center = Offset(w * 0.68f, h * 0.32f), style = Stroke(5f))
                        if (wrongGuesses >= 2) drawLine(ink, Offset(w * 0.68f, h * 0.42f), Offset(w * 0.68f, h * 0.66f), 5f, StrokeCap.Round)
                        if (wrongGuesses >= 3) drawLine(ink, Offset(w * 0.68f, h * 0.48f), Offset(w * 0.54f, h * 0.58f), 5f, StrokeCap.Round)
                        if (wrongGuesses >= 4) drawLine(ink, Offset(w * 0.68f, h * 0.48f), Offset(w * 0.82f, h * 0.58f), 5f, StrokeCap.Round)
                        if (wrongGuesses >= 5) drawLine(ink, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.56f, h * 0.82f), 5f, StrokeCap.Round)
                        if (wrongGuesses >= 6) drawLine(ink, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.80f, h * 0.82f), 5f, StrokeCap.Round)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = secretWord.map { if (it in guessed || lost) it else '_' }.joinToString(" "),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Wrong guesses: $wrongGuesses / 6",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ('A'..'Z').forEach { ch ->
                    val used = ch in guessed
                    val hit = used && ch in secretWord
                    Surface(
                        modifier = Modifier
                            .size(width = 40.dp, height = 44.dp)
                            .clickable(enabled = !used && !won && !lost) {
                                guessed = guessed + ch
                                onPlayCue(if (ch in secretWord) SoundCue.MOVE else SoundCue.ERROR)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            hit -> MaterialTheme.colorScheme.primary
                            used -> MaterialTheme.colorScheme.background
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = ch.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    hit -> MaterialTheme.colorScheme.onPrimary
                                    used -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (won || lost) {
        GameCompletionDialog(
            title = if (won) "Word Solved!" else "Out of Guesses",
            subtitle = "The word was $secretWord.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
