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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordScrambleScreen(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("word_scramble") }
    val deck = remember { WordBank.scrambleEntries.shuffled() }
    var round by remember { mutableIntStateOf(0) }
    var entry by remember { mutableStateOf(deck[0]) }
    val pool = remember { mutableStateListOf<Pair<Int, Char>>() }
    val chosen = remember { mutableStateListOf<Pair<Int, Char>>() }
    var showHint by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    fun loadWord(idx: Int) {
        val item = deck[idx % deck.size]
        entry = item
        val indexed = item.word.mapIndexed { i, c -> i to c }.shuffled()
        pool.clear()
        pool.addAll(indexed)
        chosen.clear()
        showHint = false
        feedback = null
    }

    remember(Unit) {
        loadWord(0)
        true
    }

    fun checkAnswer() {
        val guess = chosen.joinToString("") { it.second.toString() }
        if (guess == entry.word) {
            onPlayCue(SoundCue.WIN)
            streak++
            score += entry.word.length * 20 + streak * 15 - if (showHint) 25 else 0
            if (round + 1 >= 8) {
                finished = true
                onRecordOutcome(MatchOutcome.COMPLETE, "Player", "Cleared 8 scrambles • Score $score", score, 0)
            } else {
                round++
                loadWord(round)
            }
        } else {
            onPlayCue(SoundCue.ERROR)
            feedback = "Not quite — try rearranging the tiles."
        }
    }

    GameScaffold(
        game = game,
        subtitle = "Round ${round + 1}/8 • Streak $streak • Score $score",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = {
            round = 0
            streak = 0
            score = 0
            finished = false
            loadWord(0)
        }
    ) {
        MonoCard(elevated = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoTag(text = entry.category, emphasized = true)
                    Text(
                        text = "${entry.word.length} Letters",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (showHint) {
                    Text(
                        text = "Hint: ${entry.hint}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Tap letters below to assemble the hidden word.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Answer Slots
        MonoCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in entry.word.indices) {
                        val item = chosen.getOrNull(i)
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(enabled = item != null) {
                                    if (item != null) {
                                        chosen.remove(item)
                                        pool.add(item)
                                        feedback = null
                                        onPlayCue(SoundCue.MOVE)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (item != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item?.second?.toString() ?: "",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (item != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (feedback != null) {
            Spacer(Modifier.height(8.dp))
            Text(feedback!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))

        // Letter Pool
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pool.toList().forEach { item ->
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            pool.remove(item)
                            chosen.add(item)
                            feedback = null
                            onPlayCue(SoundCue.MOVE)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.second.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showHint = true },
                enabled = !showHint,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Hint") }
            OutlinedButton(
                onClick = {
                    val temp = pool.toList().shuffled()
                    pool.clear()
                    pool.addAll(temp)
                },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Shuffle") }
            OutlinedButton(
                onClick = {
                    streak = 0
                    if (round + 1 >= 8) {
                        finished = true
                        onRecordOutcome(MatchOutcome.COMPLETE, "Player", "Score $score", score, 0)
                    } else {
                        round++
                        loadWord(round)
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Skip") }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { checkAnswer() },
            enabled = chosen.size == entry.word.length,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Check Word") }
    }

    if (finished) {
        GameCompletionDialog(
            title = "Scramble Complete!",
            subtitle = "Final Score: $score points across 8 rounds.",
            onPlayAgain = {
                round = 0
                streak = 0
                score = 0
                finished = false
                loadWord(0)
            },
            onExit = onBack
        )
    }
}
