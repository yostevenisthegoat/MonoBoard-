package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.AiDifficulty
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun NimScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("nim") }
    val misere = sessionConfig.ruleVariant.contains("Mis", ignoreCase = true)

    val heaps = remember { mutableStateListOf(3, 5, 7) }
    var activeSeat by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    var selectedHeap by remember { mutableIntStateOf(0) }
    var removeCount by remember { mutableIntStateOf(1) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun takeFromHeap(heapIdx: Int, count: Int) {
        if (winnerSeat != null || count <= 0 || count > heaps[heapIdx]) return
        heaps[heapIdx] -= count
        onPlayCue(SoundCue.MOVE)
        if (heaps.all { it == 0 }) {
            winnerSeat = if (misere) 1 - activeSeat else activeSeat
        } else {
            activeSeat = 1 - activeSeat
            selectedHeap = heaps.indexOfFirst { it > 0 }.coerceAtLeast(0)
            removeCount = 1
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && winnerSeat == null
    LaunchedEffect(activeSeat, isAiTurn) {
        if (isAiTurn) {
            delay(450L)
            val nonZero = heaps.indices.filter { heaps[it] > 0 }
            if (nonZero.isNotEmpty()) {
                val (hIdx, take) = if (sessionConfig.aiDifficulty == AiDifficulty.EASY) {
                    val h = nonZero.random()
                    h to (1..heaps[h]).random()
                } else {
                    // Optimal Nim-sum XOR strategy
                    val xorSum = heaps.fold(0) { acc, v -> acc xor v }
                    if (xorSum != 0) {
                        val targetH = nonZero.firstOrNull { (heaps[it] xor xorSum) < heaps[it] } ?: nonZero.first()
                        val targetRem = heaps[targetH] xor xorSum
                        targetH to (heaps[targetH] - targetRem).coerceAtLeast(1)
                    } else {
                        val h = nonZero.random()
                        h to 1
                    }
                }
                takeFromHeap(hIdx, take)
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val wp = sessionConfig.players[w]
            onRecordOutcome(
                if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN,
                wp.name,
                "${wp.name} won Nim (${sessionConfig.ruleVariant})",
                100,
                30
            )
        }
    }

    fun restart() {
        heaps[0] = 3; heaps[1] = 5; heaps[2] = 7
        activeSeat = sessionConfig.startingPlayerIndex
        selectedHeap = 0; removeCount = 1; winnerSeat = null; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = sessionConfig.ruleVariant,
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            statusMessage = if (misere) "Misère: Avoid taking the last stone." else "Normal: Take the last stone to win."
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            heaps.forEachIndexed { idx, count ->
                val selected = selectedHeap == idx && count > 0
                MonoCard(
                    elevated = selected,
                    borderHighlight = selected,
                    onClick = {
                        if (count > 0 && !isAiTurn) {
                            selectedHeap = idx
                            removeCount = removeCount.coerceIn(1, count)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Heap ${idx + 1} ($count)", style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMonoFontFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(count) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (!isAiTurn && winnerSeat == null && heaps[selectedHeap] > 0) {
            MonoCard(elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Take from Heap ${selectedHeap + 1}: $removeCount", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..heaps[selectedHeap]).forEach { amt ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { removeCount = amt },
                                color = if (removeCount == amt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        amt.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (removeCount == amt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { takeFromHeap(selectedHeap, removeCount) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Confirm Move") }
                }
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = "All heaps cleared under ${sessionConfig.ruleVariant} rules.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
