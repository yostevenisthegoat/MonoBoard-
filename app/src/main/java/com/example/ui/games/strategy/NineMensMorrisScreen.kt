package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Normalized 0..6 grid coordinates for 24 points of Nine Men's Morris
private val morrisCoords = listOf(
    0 to 0, 3 to 0, 6 to 0,
    1 to 1, 3 to 1, 5 to 1,
    2 to 2, 3 to 2, 4 to 2,
    0 to 3, 1 to 3, 2 to 3, 4 to 3, 5 to 3, 6 to 3,
    2 to 4, 3 to 4, 4 to 4,
    1 to 5, 3 to 5, 5 to 5,
    0 to 6, 3 to 6, 6 to 6
)

private val morrisAdj = listOf(
    listOf(1, 9), listOf(0, 2, 4), listOf(1, 14),
    listOf(4, 10), listOf(1, 3, 5, 7), listOf(4, 13),
    listOf(7, 11), listOf(4, 6, 8), listOf(7, 12),
    listOf(0, 10, 21), listOf(3, 9, 11, 18), listOf(6, 10, 15),
    listOf(8, 13, 17), listOf(5, 12, 14, 20), listOf(2, 13, 23),
    listOf(11, 16), listOf(15, 17, 19), listOf(12, 16),
    listOf(10, 19), listOf(16, 18, 20, 22), listOf(13, 19),
    listOf(9, 22), listOf(19, 21, 23), listOf(14, 22)
)

private val morrisMills = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(9, 10, 11), listOf(12, 13, 14),
    listOf(15, 16, 17), listOf(18, 19, 20), listOf(21, 22, 23),
    listOf(0, 9, 21), listOf(3, 10, 18), listOf(6, 11, 15),
    listOf(1, 4, 7), listOf(16, 19, 22),
    listOf(8, 12, 17), listOf(5, 13, 20), listOf(2, 14, 23)
)

@Composable
fun NineMensMorrisScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("nine_mens_morris") }
    var nodes by remember { mutableStateOf(List(24) { 0 }) } // 0=empty, 1=P1, 2=P2
    var toPlace by remember { mutableStateOf(listOf(9, 9)) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var selectedNode by remember { mutableStateOf<Int?>(null) }
    var pendingRemoval by remember { mutableStateOf(false) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }
    var errorShakeCount by remember { mutableIntStateOf(0) }

    fun formsMill(board: List<Int>, idx: Int, player: Int): Boolean {
        return morrisMills.any { mill -> idx in mill && mill.all { board[it] == player } }
    }

    fun hasAnyLegalMove(board: List<Int>, seat: Int): Boolean {
        if (toPlace[seat] > 0) return board.any { it == 0 }
        val p = seat + 1
        val count = board.count { it == p }
        if (count < 3) return false
        if (count == 3) return board.any { it == 0 }
        return board.indices.any { i -> board[i] == p && morrisAdj[i].any { board[it] == 0 } }
    }

    fun advanceTurn(updatedBoard: List<Int>) {
        val opp = 1 - activeSeat
        val oppPiecesOnBoard = updatedBoard.count { it == opp + 1 }
        if (toPlace[opp] == 0 && (oppPiecesOnBoard < 3 || !hasAnyLegalMove(updatedBoard, opp))) {
            winnerSeat = activeSeat
        } else {
            activeSeat = opp
        }
    }

    fun handleNodeTap(idx: Int) {
        if (winnerSeat != null) return
        val p = activeSeat + 1
        val oppP = (1 - activeSeat) + 1

        if (pendingRemoval) {
            if (nodes[idx] != oppP) return
            val nonMillOpp = nodes.indices.filter { nodes[it] == oppP && !formsMill(nodes, it, oppP) }
            if (nonMillOpp.isNotEmpty() && formsMill(nodes, idx, oppP)) {
                errorShakeCount++
                onPlayCue(SoundCue.ERROR)
                return
            }
            val next = nodes.toMutableList().apply { this[idx] = 0 }
            nodes = next
            pendingRemoval = false
            onPlayCue(SoundCue.CAPTURE)
            advanceTurn(next)
            return
        }

        if (toPlace[activeSeat] > 0) {
            if (nodes[idx] != 0) return
            val next = nodes.toMutableList().apply { this[idx] = p }
            nodes = next
            toPlace = toPlace.toMutableList().apply { this[activeSeat]-- }
            if (formsMill(next, idx, p)) {
                pendingRemoval = true
                onPlayCue(SoundCue.BOUNCE)
            } else {
                onPlayCue(SoundCue.WOOD_TAP)
                advanceTurn(next)
            }
        } else {
            val sel = selectedNode
            val myCount = nodes.count { it == p }
            val canFly = myCount == 3
            if (nodes[idx] == p) {
                selectedNode = idx
                onPlayCue(SoundCue.KEY_TICK)
            } else if (sel != null && nodes[idx] == 0 && (canFly || idx in morrisAdj[sel])) {
                val next = nodes.toMutableList().apply { this[sel] = 0; this[idx] = p }
                nodes = next
                selectedNode = null
                if (formsMill(next, idx, p)) {
                    pendingRemoval = true
                    onPlayCue(SoundCue.BOUNCE)
                } else {
                    onPlayCue(SoundCue.WOOD_TAP)
                    advanceTurn(next)
                }
            }
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && winnerSeat == null
    LaunchedEffect(nodes, activeSeat, pendingRemoval, isAiTurn) {
        if (isAiTurn) {
            delay(350L)
            if (pendingRemoval) {
                val opp = 1
                val remCandidates = nodes.indices.filter { nodes[it] == opp && !formsMill(nodes, it, opp) }
                    .ifEmpty { nodes.indices.filter { nodes[it] == opp } }
                remCandidates.randomOrNull()?.let { handleNodeTap(it) }
            } else if (toPlace[1] > 0) {
                val empties = nodes.indices.filter { nodes[it] == 0 }
                val millPick = empties.firstOrNull { idx ->
                    val t = nodes.toMutableList().apply { this[idx] = 2 }
                    formsMill(t, idx, 2)
                }
                (millPick ?: empties.randomOrNull())?.let { handleNodeTap(it) }
            } else {
                val myCount = nodes.count { it == 2 }
                val canFly = myCount == 3
                val sources = nodes.indices.filter { nodes[it] == 2 }
                val moves = sources.flatMap { src ->
                    val dests = if (canFly) nodes.indices.filter { nodes[it] == 0 } else morrisAdj[src].filter { nodes[it] == 0 }
                    dests.map { src to it }
                }
                val pick = moves.randomOrNull()
                if (pick != null) {
                    selectedNode = pick.first
                    handleNodeTap(pick.second)
                }
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} won Nine Men's Morris", 100, 90)
        }
    }

    fun restart() {
        nodes = List(24) { 0 }
        toPlace = listOf(9, 9)
        activeSeat = 0; selectedNode = null; pendingRemoval = false; winnerSeat = null; recorded = false
    }

    val phaseText = when {
        pendingRemoval -> "Mill formed! Remove an opponent piece."
        toPlace[activeSeat] > 0 -> "Placing phase (${toPlace[activeSeat]} left to place)"
        nodes.count { it == activeSeat + 1 } == 3 -> "Flying phase! Move to any empty point."
        else -> "Sliding phase — move along lines."
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
            scores = listOf(nodes.count { it == 1 }, nodes.count { it == 2 }),
            statusMessage = phaseText
        )
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight, 360.dp)
            Surface(
                modifier = Modifier.size(boardSize).shakeOnTrigger(errorShakeCount),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wPx = constraints.maxWidth.toFloat()
                    val hPx = constraints.maxHeight.toFloat()
                    val pad = wPx * 0.1f
                    val step = (wPx - 2 * pad) / 6f
                    val lineColor = MaterialTheme.colorScheme.outline

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in morrisAdj.indices) {
                            val (x1, y1) = morrisCoords[i]
                            for (j in morrisAdj[i]) {
                                if (j > i) {
                                    val (x2, y2) = morrisCoords[j]
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(pad + x1 * step, pad + y1 * step),
                                        end = Offset(pad + x2 * step, pad + y2 * step),
                                        strokeWidth = 4f
                                    )
                                }
                            }
                        }
                    }

                    val oppP = (1 - activeSeat) + 1
                    morrisCoords.forEachIndexed { idx, (gx, gy) ->
                        val nodeSize = 28.dp
                        val cx = pad + gx * step
                        val cy = pad + gy * step
                        val valP = nodes[idx]
                        val isSel = selectedNode == idx
                        val isRemovableTarget = pendingRemoval && valP == oppP && !isAiTurn && winnerSeat == null
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((cx - nodeSize.toPx() / 2f).roundToInt(), (cy - nodeSize.toPx() / 2f).roundToInt()) }
                                .size(nodeSize)
                                .springPop(valP, popScale = 1.24f)
                                .pulseEffect(isSel || isRemovableTarget, maxScale = 1.18f)
                                .bouncyPress(enabled = !isAiTurn && winnerSeat == null)
                                .clip(CircleShape)
                                .background(
                                    when (valP) {
                                        1 -> Color(0xFFF5F5F5)
                                        2 -> Color(0xFF333333)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    width = if (isSel || isRemovableTarget) 3.dp else 1.5.dp,
                                    color = if (isRemovableTarget) Color(0xFFE53935) else if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable(enabled = !isAiTurn && winnerSeat == null) { handleNodeTap(idx) }
                        )
                    }
                }
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = "Opponent reduced below 3 pieces or blocked.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
