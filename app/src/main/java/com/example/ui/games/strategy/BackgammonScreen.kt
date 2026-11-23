package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun BackgammonScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("backgammon") }

    // Positive = P1 (moves 0 -> 23 -> off), Negative = P2 (moves 23 -> 0 -> off)
    fun initPoints(): List<Int> {
        val pts = MutableList(24) { 0 }
        pts[0] = 2; pts[11] = 5; pts[16] = 3; pts[18] = 5
        pts[23] = -2; pts[12] = -5; pts[7] = -3; pts[5] = -5
        return pts
    }

    var points by remember { mutableStateOf(initPoints()) }
    var bar by remember { mutableStateOf(listOf(0, 0)) }
    var borneOff by remember { mutableStateOf(listOf(0, 0)) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var diceRemaining by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedSource by remember { mutableStateOf<Int?>(null) } // -1 = bar
    var statusText by remember { mutableStateOf("Tap Roll Dice to start.") }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun canBearOff(pts: List<Int>, b: List<Int>, seat: Int): Boolean {
        if (b[seat] > 0) return false
        return if (seat == 0) (0..17).all { pts[it] <= 0 } else (6..23).all { pts[it] >= 0 }
    }

    fun validMovesForDie(pts: List<Int>, b: List<Int>, seat: Int, die: Int): List<Pair<Int, Int>> {
        val dir = if (seat == 0) 1 else -1
        val out = mutableListOf<Pair<Int, Int>>()
        if (b[seat] > 0) {
            val dest = if (seat == 0) die - 1 else 24 - die
            val oppCount = if (seat == 0) -pts[dest] else pts[dest]
            if (oppCount <= 1) out.add(-1 to dest)
            return out
        }
        val bearOff = canBearOff(pts, b, seat)
        for (i in 0..23) {
            val hasMine = if (seat == 0) pts[i] > 0 else pts[i] < 0
            if (!hasMine) continue
            val dest = i + dir * die
            if (dest in 0..23) {
                val oppCount = if (seat == 0) -pts[dest] else pts[dest]
                if (oppCount <= 1) out.add(i to dest)
            } else if (bearOff) {
                out.add(i to 24) // 24 = bear off
            }
        }
        return out
    }

    fun allLegalMoves(pts: List<Int>, b: List<Int>, seat: Int, dice: List<Int>): List<Triple<Int, Int, Int>> {
        return dice.distinct().flatMap { d ->
            validMovesForDie(pts, b, seat, d).map { (s, t) -> Triple(s, t, d) }
        }
    }

    fun executeMove(src: Int, dst: Int, dieUsed: Int) {
        val nextPts = points.toMutableList()
        val nextBar = bar.toMutableList()
        val nextOff = borneOff.toMutableList()

        if (src == -1) nextBar[activeSeat]--
        else if (activeSeat == 0) nextPts[src]-- else nextPts[src]++

        var hit = false
        if (dst == 24) {
            nextOff[activeSeat]++
        } else {
            if (activeSeat == 0) {
                if (nextPts[dst] == -1) { nextPts[dst] = 0; nextBar[1]++; hit = true }
                nextPts[dst]++
            } else {
                if (nextPts[dst] == 1) { nextPts[dst] = 0; nextBar[0]++; hit = true }
                nextPts[dst]--
            }
        }

        points = nextPts
        bar = nextBar
        borneOff = nextOff
        selectedSource = null
        onPlayCue(if (hit) SoundCue.CAPTURE else SoundCue.MOVE)

        if (nextOff[activeSeat] >= 15) {
            winnerSeat = activeSeat
            onPlayCue(SoundCue.WIN)
            return
        }

        val rem = diceRemaining.toMutableList()
        rem.remove(dieUsed)
        if (rem.isEmpty() || allLegalMoves(nextPts, nextBar, activeSeat, rem).isEmpty()) {
            diceRemaining = emptyList()
            activeSeat = 1 - activeSeat
            statusText = "${sessionConfig.players[activeSeat].name}'s turn — Roll Dice"
        } else {
            diceRemaining = rem
        }
    }

    fun rollDice() {
        if (diceRemaining.isNotEmpty() || winnerSeat != null) return
        val d1 = (1..6).random(); val d2 = (1..6).random()
        val dice = if (d1 == d2) listOf(d1, d1, d1, d1) else listOf(d1, d2)
        onPlayCue(SoundCue.MOVE)
        if (allLegalMoves(points, bar, activeSeat, dice).isEmpty()) {
            statusText = "${sessionConfig.players[activeSeat].name} rolled $d1 & $d2 — blocked!"
            activeSeat = 1 - activeSeat
        } else {
            diceRemaining = dice
            statusText = "Rolled ${dice.joinToString(", ")} — select checker & target"
            if (bar[activeSeat] > 0) selectedSource = -1
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, diceRemaining, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi) {
            delay(420L)
            if (diceRemaining.isEmpty()) {
                rollDice()
            } else {
                val legal = allLegalMoves(points, bar, activeSeat, diceRemaining)
                if (legal.isNotEmpty()) {
                    val best = legal.firstOrNull { it.second == 24 }
                        ?: legal.firstOrNull { (_, dst, _) -> dst in 0..23 && points[dst] == 1 }
                        ?: legal.random()
                    executeMove(best.first, best.second, best.third)
                }
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} bore off all 15 checkers", 100, 120)
        }
    }

    fun restart() {
        points = initPoints(); bar = listOf(0, 0); borneOff = listOf(0, 0)
        activeSeat = 0; diceRemaining = emptyList(); selectedSource = null
        winnerSeat = null; recorded = false; statusText = "Tap Roll Dice to start."
    }

    val legalMoves = remember(points, bar, activeSeat, diceRemaining) {
        allLegalMoves(points, bar, activeSeat, diceRemaining)
    }

    GameScaffold(
        game = game,
        subtitle = "Borne Off: ${borneOff[0]} vs ${borneOff[1]}",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = borneOff,
            statusMessage = statusText
        )
        Spacer(Modifier.height(10.dp))

        // Top Half: points 12..23 (left to right)
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (idx in 12..23) {
                        BackgammonPointColumn(
                            idx = idx,
                            count = points[idx],
                            isTop = true,
                            isSelected = selectedSource == idx,
                            isLegalDest = selectedSource != null && legalMoves.any { it.first == selectedSource && it.second == idx },
                            onClick = {
                                val move = legalMoves.firstOrNull { it.first == selectedSource && it.second == idx }
                                if (move != null && !activePlayer.isAi) {
                                    executeMove(move.first, move.second, move.third)
                                } else if (!activePlayer.isAi && legalMoves.any { it.first == idx }) {
                                    selectedSource = idx
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Center Bar & Bear Off strip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BAR: P1(${bar[0]}) P2(${bar[1]})",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.clickable(enabled = bar[activeSeat] > 0 && !activePlayer.isAi) {
                            selectedSource = -1
                        }
                    )
                    val canBearSelected = selectedSource != null && legalMoves.any { it.first == selectedSource && it.second == 24 }
                    Surface(
                        modifier = Modifier.clickable(enabled = canBearSelected && !activePlayer.isAi) {
                            val m = legalMoves.first { it.first == selectedSource && it.second == 24 }
                            executeMove(m.first, m.second, m.third)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (canBearSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = if (canBearSelected) "BEAR OFF" else "DICE: ${if (diceRemaining.isEmpty()) "—" else diceRemaining.joinToString(",")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canBearSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                // Bottom Half: points 11 down to 0
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (idx in 11 downTo 0) {
                        BackgammonPointColumn(
                            idx = idx,
                            count = points[idx],
                            isTop = false,
                            isSelected = selectedSource == idx,
                            isLegalDest = selectedSource != null && legalMoves.any { it.first == selectedSource && it.second == idx },
                            onClick = {
                                val move = legalMoves.firstOrNull { it.first == selectedSource && it.second == idx }
                                if (move != null && !activePlayer.isAi) {
                                    executeMove(move.first, move.second, move.third)
                                } else if (!activePlayer.isAi && legalMoves.any { it.first == idx }) {
                                    selectedSource = idx
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = ::rollDice,
            enabled = diceRemaining.isEmpty() && !activePlayer.isAi && winnerSeat == null,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (diceRemaining.isEmpty()) "Roll Dice" else "Dice Remaining: ${diceRemaining.joinToString(", ")}")
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins Backgammon!",
            subtitle = "All 15 checkers borne off.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}

@Composable
private fun BackgammonPointColumn(
    idx: Int,
    count: Int,
    isTop: Boolean,
    isSelected: Boolean,
    isLegalDest: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        isLegalDest -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        idx % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.background
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(bg, RoundedCornerShape(4.dp))
            .border(
                width = if (isLegalDest) 1.5.dp else 0.5.dp,
                color = if (isLegalDest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isTop) Arrangement.Top else Arrangement.Bottom
    ) {
        val absCount = kotlin.math.abs(count)
        val shown = minOf(absCount, 5)
        for (k in 0 until shown) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(if (count > 0) Color(0xFFF2F2F2) else Color(0xFF2B2B2B))
                    .border(1.dp, Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (k == shown - 1 && absCount > 5) {
                    Text(
                        absCount.toString(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
