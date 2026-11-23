package com.example.ui.games.words

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoTag
import com.example.ui.components.bouncyPress
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnagramsScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("anagrams") }
    var rackIdx by remember { mutableIntStateOf(0) }
    var rack by remember { mutableStateOf(WordBank.anagramRacks[0]) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val foundWords = remember { mutableStateListOf<String>() }
    var score by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(90) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    fun startRack(nextIdx: Int) {
        rackIdx = nextIdx % WordBank.anagramRacks.size
        rack = WordBank.anagramRacks[rackIdx]
        selectedIndices.clear()
        foundWords.clear()
        score = 0
        remainingSeconds = 90
        feedback = null
        finished = false
    }

    LaunchedEffect(rackIdx, finished) {
        while (!finished && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            if (remainingSeconds in 1..5) {
                onPlayCue(SoundCue.CHECK_ALERT)
            }
            if (remainingSeconds == 0) {
                finished = true
                onPlayCue(SoundCue.WIN)
                onRecordOutcome(
                    MatchOutcome.COMPLETE,
                    "Player",
                    "Found ${foundWords.size}/${rack.validWords.size} words • $score pts",
                    score,
                    90
                )
            }
        }
    }

    fun submitWord() {
        val word = selectedIndices.joinToString("") { rack.letters[it].toString() }
        selectedIndices.clear()
        when {
            word.length < 3 -> {
                feedback = "Minimum 3 letters"
                errorShakeCount++
                onPlayCue(SoundCue.ERROR)
            }
            word in foundWords -> {
                feedback = "$word already discovered"
                errorShakeCount++
                onPlayCue(SoundCue.ERROR)
            }
            word in rack.validWords -> {
                foundWords.add(0, word)
                score += word.length * word.length * 10
                feedback = "+${word.length * word.length * 10} pts for $word!"
                if (foundWords.size == rack.validWords.size) {
                    finished = true
                    onPlayCue(SoundCue.WIN)
                    onRecordOutcome(MatchOutcome.WIN, "Player", "Cleared all ${rack.validWords.size} anagrams", score, 90 - remainingSeconds)
                } else {
                    onPlayCue(SoundCue.TILE_MERGE)
                }
            }
            else -> {
                feedback = "$word is not in this rack's dictionary"
                errorShakeCount++
                onPlayCue(SoundCue.ERROR)
            }
        }
    }

    GameScaffold(
        game = game,
        subtitle = "Found ${foundWords.size}/${rack.validWords.size} • $score pts • ${remainingSeconds}s",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = { startRack(rackIdx + 1) }
    ) {
        MonoCard(elevated = true, modifier = Modifier.shakeOnTrigger(errorShakeCount)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val built = selectedIndices.joinToString("") { rack.letters[it].toString() }
                Text(
                    text = built.ifEmpty { "TAP LETTERS" },
                    modifier = Modifier.springPop(built, popScale = 1.08f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = if (built.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                if (feedback != null) {
                    Text(feedback!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            rack.letters.forEachIndexed { idx, ch ->
                val used = idx in selectedIndices
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .springPop(used, popScale = 1.15f)
                        .bouncyPress(!used && !finished)
                        .clickable(enabled = !used && !finished) {
                            selectedIndices.add(idx)
                            onPlayCue(SoundCue.KEY_TICK)
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (used) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (used) "·" else ch.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    selectedIndices.clear()
                    onPlayCue(SoundCue.KEY_TICK)
                },
                modifier = Modifier.weight(1f).height(48.dp).bouncyPress()
            ) { Text("Clear") }
            val canSubmit = selectedIndices.size >= 3 && !finished
            Button(
                onClick = { submitWord() },
                enabled = canSubmit,
                modifier = Modifier.weight(2f).height(48.dp).bouncyPress(canSubmit)
            ) { Text("Submit Word") }
        }

        Spacer(Modifier.height(12.dp))

        Text("Discovered Words (${foundWords.size}/${rack.validWords.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        MonoCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                foundWords.forEach { w ->
                    Box(modifier = Modifier.springPop(w, popScale = 1.16f)) {
                        MonoTag(text = w, emphasized = w.length >= 6)
                    }
                }
            }
        }
    }

    if (finished) {
        GameCompletionDialog(
            title = "Rack Finished!",
            subtitle = "Discovered ${foundWords.size} of ${rack.validWords.size} words • Score: $score",
            onPlayAgain = { startRack(rackIdx + 1) },
            onExit = onBack
        )
    }
}
