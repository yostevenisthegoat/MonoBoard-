package com.example.ui.games.words

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoTag
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun TriviaScreen(
    gameId: String, // "trivia" or "quiz_challenge"
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val isBlitz = gameId == "quiz_challenge"
    val game = remember(gameId) { GameCatalog.byId(gameId) }
    val playerCount = sessionConfig.players.size.coerceAtLeast(1)

    var questions by remember { mutableStateOf(WordBank.triviaQuestions.shuffled().take(10)) }
    var qIndex by remember { mutableIntStateOf(0) }
    var activeSeat by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    val scores = remember(playerCount) { mutableStateListOf<Int>().apply { repeat(playerCount) { add(0) } } }
    var streak by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var secondsLeft by remember { mutableIntStateOf(15) }
    var totalElapsed by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun resetQuiz() {
        questions = WordBank.triviaQuestions.shuffled().take(10)
        qIndex = 0
        activeSeat = sessionConfig.startingPlayerIndex
        for (i in scores.indices) scores[i] = 0
        streak = 0
        selectedOption = null
        secondsLeft = 15
        totalElapsed = 0
        finished = false
    }

    LaunchedEffect(qIndex, selectedOption, finished) {
        while (!finished && selectedOption == null) {
            delay(1000)
            totalElapsed++
            if (isBlitz) {
                secondsLeft--
                if (secondsLeft <= 0) {
                    selectedOption = -1
                    streak = 0
                    onPlayCue(SoundCue.ERROR)
                }
            }
        }
    }

    val currentQ = questions[qIndex]

    fun nextQuestion() {
        if (qIndex + 1 >= questions.size) {
            finished = true
            onPlayCue(SoundCue.WIN)
            val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
            val winnerName = sessionConfig.players.getOrNull(bestIdx)?.name ?: "Player"
            val topScore = scores[bestIdx]
            onRecordOutcome(
                MatchOutcome.WIN,
                winnerName,
                "Scored $topScore pts across 10 questions",
                topScore,
                totalElapsed
            )
        } else {
            qIndex++
            if (playerCount > 1) {
                activeSeat = (activeSeat + 1) % playerCount
            }
            selectedOption = null
            secondsLeft = 15
        }
    }

    GameScaffold(
        game = game,
        subtitle = "Q${qIndex + 1}/${questions.size} • ${if (isBlitz) "${secondsLeft}s" else currentQ.category}",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = { resetQuiz() }
    ) {
        if (playerCount > 1) {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = activeSeat,
                scores = scores,
                statusMessage = "Question ${qIndex + 1} of ${questions.size}"
            )
            Spacer(Modifier.height(10.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonoTag(text = "Score ${scores[0]}", emphasized = true)
                if (isBlitz) MonoTag(text = "Streak x${(streak + 1).coerceAtMost(4)}")
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isBlitz) {
            LinearProgressIndicator(
                progress = { (secondsLeft / 15f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        MonoCard(elevated = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonoTag(text = currentQ.category)
                Text(
                    text = currentQ.prompt,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            currentQ.options.forEachIndexed { idx, opt ->
                val revealed = selectedOption != null
                val isCorrect = idx == currentQ.correctIndex
                val isSelected = idx == selectedOption
                val bg = when {
                    revealed && isCorrect -> Color(0xFF1E4620)
                    revealed && isSelected && !isCorrect -> Color(0xFF5A1E1E)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    revealed && isCorrect -> Color(0xFF66BB6A)
                    revealed && isSelected && !isCorrect -> Color(0xFFEF5350)
                    else -> MaterialTheme.colorScheme.outline
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !revealed && !finished) {
                            selectedOption = idx
                            if (isCorrect) {
                                streak++
                                val mult = if (isBlitz) streak.coerceAtMost(4) else 1
                                val pts = (if (isBlitz) 100 + secondsLeft * 10 else 100) * mult
                                scores[activeSeat] = scores[activeSeat] + pts
                                onPlayCue(SoundCue.CAPTURE)
                            } else {
                                streak = 0
                                onPlayCue(SoundCue.ERROR)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = bg,
                    border = BorderStroke(1.2.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${'A' + idx}.",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (selectedOption != null) {
            Spacer(Modifier.height(12.dp))
            MonoCard {
                Text(
                    text = currentQ.explanation,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { nextQuestion() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (qIndex + 1 >= questions.size) "See Results" else "Next Question")
            }
        }
    }

    if (finished) {
        val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
        val winner = sessionConfig.players.getOrNull(bestIdx)?.name ?: "Player"
        GameCompletionDialog(
            title = if (playerCount > 1) "$winner Wins!" else "Quiz Complete!",
            subtitle = if (playerCount > 1) {
                sessionConfig.players.mapIndexed { i, p -> "${p.name}: ${scores[i]}" }.joinToString(" • ")
            } else {
                "Final Score: ${scores[0]} points"
            },
            onPlayAgain = { resetQuiz() },
            onExit = onBack
        )
    }
}
