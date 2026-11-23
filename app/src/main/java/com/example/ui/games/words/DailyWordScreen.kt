package com.example.ui.games.words

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs

private enum class LetterState {
    EMPTY, EXACT, PRESENT, ABSENT
}

@Composable
fun DailyWordScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("daily_word") }
    val endlessMode = sessionConfig.ruleVariant.contains("Endless", ignoreCase = true)

    fun pickWord(roundSeed: Int): String {
        val list = WordBank.fiveLetterTargets
        return if (!endlessMode && roundSeed == 0) {
            val cal = Calendar.getInstance()
            val dayCode = cal.get(Calendar.YEAR) * 372 + cal.get(Calendar.DAY_OF_YEAR)
            list[abs(dayCode) % list.size]
        } else {
            list.random()
        }
    }

    var roundIndex by remember { mutableIntStateOf(0) }
    var targetWord by remember { mutableStateOf(pickWord(0)) }
    val guesses = remember { mutableStateListOf<String>() }
    var currentInput by remember { mutableStateOf("") }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var won by remember { mutableStateOf(false) }
    var lost by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(won, lost) {
        while (!won && !lost) {
            delay(1000)
            elapsed++
        }
    }

    fun restart() {
        roundIndex++
        targetWord = pickWord(roundIndex)
        guesses.clear()
        currentInput = ""
        statusBanner = null
        won = false
        lost = false
        elapsed = 0
        recorded = false
    }

    fun evaluateGuess(guess: String): List<LetterState> {
        val res = MutableList(5) { LetterState.ABSENT }
        val remaining = targetWord.toMutableList()
        for (i in 0 until 5) {
            if (guess[i] == targetWord[i]) {
                res[i] = LetterState.EXACT
                remaining[i] = '_'
            }
        }
        for (i in 0 until 5) {
            if (res[i] == LetterState.EXACT) continue
            val idx = remaining.indexOf(guess[i])
            if (idx >= 0) {
                res[i] = LetterState.PRESENT
                remaining[idx] = '_'
            }
        }
        return res
    }

    fun keyStatus(ch: Char): LetterState {
        var best = LetterState.EMPTY
        guesses.forEach { g ->
            val eval = evaluateGuess(g)
            g.forEachIndexed { idx, c ->
                if (c == ch) {
                    val s = eval[idx]
                    if (s == LetterState.EXACT) best = LetterState.EXACT
                    else if (s == LetterState.PRESENT && best != LetterState.EXACT) best = LetterState.PRESENT
                    else if (s == LetterState.ABSENT && best == LetterState.EMPTY) best = LetterState.ABSENT
                }
            }
        }
        return best
    }

    fun submitGuess() {
        if (won || lost) return
        if (currentInput.length < 5) {
            statusBanner = "Enter a 5-letter word"
            errorShakeCount++
            onPlayCue(SoundCue.ERROR)
            return
        }
        val guess = currentInput.uppercase()
        guesses.add(guess)
        currentInput = ""
        statusBanner = null

        if (guess == targetWord) {
            won = true
            onPlayCue(SoundCue.WIN)
            if (!recorded) {
                recorded = true
                val score = (7 - guesses.size) * 150
                onRecordOutcome(
                    MatchOutcome.WIN,
                    "Player",
                    "Solved $targetWord in ${guesses.size}/6",
                    score,
                    elapsed
                )
            }
        } else if (guesses.size >= 6) {
            lost = true
            onPlayCue(SoundCue.ERROR)
            if (!recorded) {
                recorded = true
                onRecordOutcome(
                    MatchOutcome.LOSS,
                    "Missed",
                    "Word was $targetWord",
                    0,
                    elapsed
                )
            }
        } else {
            onPlayCue(SoundCue.TILE_MERGE)
        }
    }

    GameScaffold(
        game = game,
        subtitle = "${if (endlessMode) "Practice" else "Daily"} • ${guesses.size}/6 • ${elapsed}s",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = { restart() }
    ) {
        if (statusBanner != null) {
            MonoCard(elevated = true) {
                Text(
                    text = statusBanner!!,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        // 6x5 Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shakeOnTrigger(errorShakeCount),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 6) {
                val word = when {
                    row < guesses.size -> guesses[row]
                    row == guesses.size -> currentInput
                    else -> ""
                }
                val eval = if (row < guesses.size) evaluateGuess(guesses[row]) else null

                Row(
                    modifier = Modifier.fillMaxWidth(0.84f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 5) {
                        val ch = word.getOrNull(col)?.toString() ?: ""
                        val wasSubmitted = row < guesses.size
                        var revealed by remember(row, roundIndex) { mutableStateOf(false) }
                        val flipScaleY = remember(row, roundIndex, col) { Animatable(1f) }
                        LaunchedEffect(wasSubmitted) {
                            if (wasSubmitted && !revealed) {
                                delay(col * 85L)
                                flipScaleY.animateTo(0.05f, tween(90))
                                revealed = true
                                flipScaleY.animateTo(
                                    1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                        val state = if (revealed) (eval?.get(col) ?: LetterState.EMPTY) else LetterState.EMPTY
                        val bg = when (state) {
                            LetterState.EXACT -> Color(0xFF2E7D32)
                            LetterState.PRESENT -> Color(0xFFC79100)
                            LetterState.ABSENT -> Color(0xFF2C2C2C)
                            LetterState.EMPTY -> MaterialTheme.colorScheme.surface
                        }
                        val fg = if (state == LetterState.EMPTY) MaterialTheme.colorScheme.onSurface else Color.White
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .springPop(if (ch.isNotEmpty() && !wasSubmitted) ch else null, popScale = 1.14f)
                                .graphicsLayer { scaleY = flipScaleY.value },
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = BorderStroke(
                                1.5.dp,
                                if (ch.isNotEmpty() && state == LetterState.EMPTY) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ch,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = fg
                                )
                            }
                        }
                    }
                }
            }
        }

        // On-Screen QWERTY Keyboard
        val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEachIndexed { rIdx, keys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (rIdx == 2) {
                        KeyCap(
                            label = "GO",
                            modifier = Modifier.weight(1.5f),
                            bg = MaterialTheme.colorScheme.primary,
                            fg = MaterialTheme.colorScheme.onPrimary,
                            onClick = { submitGuess() }
                        )
                    }
                    keys.forEach { ch ->
                        val st = keyStatus(ch)
                        val bg = when (st) {
                            LetterState.EXACT -> Color(0xFF2E7D32)
                            LetterState.PRESENT -> Color(0xFFC79100)
                            LetterState.ABSENT -> Color(0xFF222222)
                            LetterState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val fg = when (st) {
                            LetterState.ABSENT -> Color(0xFF777777)
                            LetterState.EMPTY -> MaterialTheme.colorScheme.onSurface
                            else -> Color.White
                        }
                        KeyCap(
                            label = ch.toString(),
                            modifier = Modifier.weight(1f),
                            bg = bg,
                            fg = fg,
                            onClick = {
                                if (!won && !lost && currentInput.length < 5) {
                                    currentInput += ch
                                    onPlayCue(SoundCue.KEY_TICK)
                                }
                            }
                        )
                    }
                    if (rIdx == 2) {
                        KeyCap(
                            label = "DEL",
                            modifier = Modifier.weight(1.5f),
                            bg = MaterialTheme.colorScheme.surfaceVariant,
                            fg = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                if (!won && !lost && currentInput.isNotEmpty()) {
                                    currentInput = currentInput.dropLast(1)
                                    onPlayCue(SoundCue.KEY_TICK)
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }

    if (won || lost) {
        GameCompletionDialog(
            title = if (won) "Word Solved!" else "Out of Guesses",
            subtitle = if (won) "Found $targetWord in ${guesses.size}/6 tries (${elapsed}s)."
            else "The hidden word was $targetWord.",
            onPlayAgain = { restart() },
            onExit = onBack
        )
    }
}

@Composable
private fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .bouncyPress()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (label.length > 1) 12.sp else 15.sp,
                color = fg
            )
        }
    }
}
