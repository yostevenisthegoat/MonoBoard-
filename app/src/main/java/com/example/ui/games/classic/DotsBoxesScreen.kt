package com.example.ui.games.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.springPop
import kotlinx.coroutines.delay
import kotlin.math.abs

private val boxOwnerColors = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF64B5F6),
    Color(0xFFFFB74D),
    Color(0xFF81C784)
)

@Composable
fun DotsBoxesScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("dots_boxes") }
    val boxCount = if (sessionConfig.ruleVariant.contains("5")) 5 else 4
    val dotCount = boxCount + 1

    // hEdges[r][c] for r in 0..boxCount, c in 0 until boxCount
    var hEdges by remember(boxCount) { mutableStateOf(List(dotCount) { List(boxCount) { -1 } }) }
    // vEdges[r][c] for r in 0 until boxCount, c in 0..boxCount
    var vEdges by remember(boxCount) { mutableStateOf(List(boxCount) { List(dotCount) { -1 } }) }
    var boxes by remember(boxCount) { mutableStateOf(List(boxCount) { List(boxCount) { -1 } }) }

    val scores = remember(sessionConfig.players.size) {
        mutableStateListOf<Int>().apply { repeat(sessionConfig.players.size) { add(0) } }
    }
    var activeSeat by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    val isComplete = boxes.all { row -> row.all { it != -1 } }

    fun claimCompletedBoxes(
        h: List<List<Int>>,
        v: List<List<Int>>,
        b: List<List<Int>>,
        seat: Int
    ): Pair<List<List<Int>>, Int> {
        val nextB = b.map { it.toMutableList() }
        var claimed = 0
        for (r in 0 until boxCount) {
            for (c in 0 until boxCount) {
                if (nextB[r][c] == -1 && h[r][c] != -1 && h[r + 1][c] != -1 && v[r][c] != -1 && v[r][c + 1] != -1) {
                    nextB[r][c] = seat
                    claimed++
                }
            }
        }
        return nextB.map { it.toList() } to claimed
    }

    fun playEdge(isHoriz: Boolean, r: Int, c: Int) {
        if (isComplete) return
        if (isHoriz) {
            if (hEdges[r][c] != -1) return
            val nextH = hEdges.map { it.toMutableList() }
            nextH[r][c] = activeSeat
            hEdges = nextH.map { it.toList() }
        } else {
            if (vEdges[r][c] != -1) return
            val nextV = vEdges.map { it.toMutableList() }
            nextV[r][c] = activeSeat
            vEdges = nextV.map { it.toList() }
        }
        val (nextB, claimed) = claimCompletedBoxes(hEdges, vEdges, boxes, activeSeat)
        boxes = nextB
        if (claimed > 0) {
            scores[activeSeat] += claimed
            onPlayCue(SoundCue.TILE_MERGE)
        } else {
            onPlayCue(SoundCue.KEY_TICK)
            activeSeat = (activeSeat + 1) % sessionConfig.players.size
        }
    }

    LaunchedEffect(isComplete) {
        while (!isComplete) {
            delay(1000L)
            elapsed++
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, hEdges, vEdges, isComplete) {
        if (!isComplete && activePlayer.isAi) {
            delay(320L)
            val avail = mutableListOf<Triple<Boolean, Int, Int>>()
            for (r in 0..boxCount) for (c in 0 until boxCount) if (hEdges[r][c] == -1) avail.add(Triple(true, r, c))
            for (r in 0 until boxCount) for (c in 0..boxCount) if (vEdges[r][c] == -1) avail.add(Triple(false, r, c))
            if (avail.isNotEmpty()) {
                // Prefer any edge that completes a box
                val scoring = avail.firstOrNull { (isH, r, c) ->
                    val th = hEdges.map { it.toMutableList() }
                    val tv = vEdges.map { it.toMutableList() }
                    if (isH) th[r][c] = activeSeat else tv[r][c] = activeSeat
                    claimCompletedBoxes(th, tv, boxes, activeSeat).second > 0
                }
                val pick = scoring ?: avail.random()
                playEdge(pick.first, pick.second, pick.third)
            }
        }
    }

    LaunchedEffect(isComplete) {
        if (isComplete && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val maxScore = scores.maxOrNull() ?: 0
            val winners = scores.mapIndexedNotNull { idx, sc -> if (sc == maxScore) idx else null }
            if (winners.size > 1) {
                onRecordOutcome(MatchOutcome.DRAW, "Tie", "Tied at $maxScore boxes", maxScore, elapsed)
            } else {
                val wp = sessionConfig.players[winners.first()]
                val outcome = if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
                onRecordOutcome(outcome, wp.name, "${wp.name} claimed $maxScore boxes", maxScore * 20, elapsed)
            }
        }
    }

    fun restart() {
        hEdges = List(dotCount) { List(boxCount) { -1 } }
        vEdges = List(boxCount) { List(dotCount) { -1 } }
        boxes = List(boxCount) { List(boxCount) { -1 } }
        for (i in scores.indices) scores[i] = 0
        activeSeat = sessionConfig.startingPlayerIndex
        elapsed = 0
        recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${boxCount}×${boxCount} Grid",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = scores.toList()
        )
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight, 360.dp)
            val totalClaimed = boxes.sumOf { row -> row.count { it != -1 } }
            Surface(
                modifier = Modifier
                    .size(size)
                    .springPop(totalClaimed, popScale = 1.04f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val gridOutline = MaterialTheme.colorScheme.outline
                    val dotColor = MaterialTheme.colorScheme.onSurface
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(hEdges, vEdges, activeSeat, isComplete) {
                                detectTapGestures { offset ->
                                    if (isComplete || activePlayer.isAi) return@detectTapGestures
                                    val pad = 36f
                                    val span = (this.size.width - 2 * pad) / boxCount
                                    var bestDist = Float.MAX_VALUE
                                    var bestMove: Triple<Boolean, Int, Int>? = null
                                    for (r in 0..boxCount) {
                                        for (c in 0 until boxCount) {
                                            val mx = pad + (c + 0.5f) * span
                                            val my = pad + r * span
                                            val d = abs(offset.x - mx) + abs(offset.y - my)
                                            if (d < bestDist && abs(offset.x - mx) < span * 0.45f && abs(offset.y - my) < span * 0.35f) {
                                                bestDist = d
                                                bestMove = Triple(true, r, c)
                                            }
                                        }
                                    }
                                    for (r in 0 until boxCount) {
                                        for (c in 0..boxCount) {
                                            val mx = pad + c * span
                                            val my = pad + (r + 0.5f) * span
                                            val d = abs(offset.x - mx) + abs(offset.y - my)
                                            if (d < bestDist && abs(offset.x - mx) < span * 0.35f && abs(offset.y - my) < span * 0.45f) {
                                                bestDist = d
                                                bestMove = Triple(false, r, c)
                                            }
                                        }
                                    }
                                    bestMove?.let { playEdge(it.first, it.second, it.third) }
                                }
                            }
                    ) {
                        val pad = 36f
                        val span = (this.size.width - 2 * pad) / boxCount

                        // Filled boxes
                        for (r in 0 until boxCount) {
                            for (c in 0 until boxCount) {
                                val owner = boxes[r][c]
                                if (owner != -1) {
                                    drawRect(
                                        color = boxOwnerColors[owner % boxOwnerColors.size].copy(alpha = 0.25f),
                                        topLeft = Offset(pad + c * span + 4f, pad + r * span + 4f),
                                        size = Size(span - 8f, span - 8f)
                                    )
                                }
                            }
                        }

                        // Horizontal edges
                        for (r in 0..boxCount) {
                            for (c in 0 until boxCount) {
                                val owner = hEdges[r][c]
                                drawLine(
                                    color = if (owner == -1) gridOutline.copy(alpha = 0.25f) else boxOwnerColors[owner % boxOwnerColors.size],
                                    start = Offset(pad + c * span, pad + r * span),
                                    end = Offset(pad + (c + 1) * span, pad + r * span),
                                    strokeWidth = if (owner == -1) 3f else 10f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        // Vertical edges
                        for (r in 0 until boxCount) {
                            for (c in 0..boxCount) {
                                val owner = vEdges[r][c]
                                drawLine(
                                    color = if (owner == -1) gridOutline.copy(alpha = 0.25f) else boxOwnerColors[owner % boxOwnerColors.size],
                                    start = Offset(pad + c * span, pad + r * span),
                                    end = Offset(pad + c * span, pad + (r + 1) * span),
                                    strokeWidth = if (owner == -1) 3f else 10f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        // Dots
                        for (r in 0..boxCount) {
                            for (c in 0..boxCount) {
                                drawCircle(
                                    color = dotColor,
                                    radius = 8f,
                                    center = Offset(pad + c * span, pad + r * span)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isComplete) {
        val maxScore = scores.maxOrNull() ?: 0
        val winners = scores.mapIndexedNotNull { idx, sc -> if (sc == maxScore) idx else null }
        val title = if (winners.size > 1) "Match Tied!" else "${sessionConfig.players[winners.first()].name} Wins!"
        GameCompletionDialog(
            title = title,
            subtitle = scores.mapIndexed { idx, sc -> "${sessionConfig.players[idx].name}: $sc" }.joinToString(" • "),
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
