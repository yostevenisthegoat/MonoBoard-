package com.example.ui.games.strategy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private enum class CoinType { WHITE, BLACK, QUEEN, STRIKER }

private data class CarromBody(
    val id: Int,
    val type: CoinType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 0.036f,
    var pocketed: Boolean = false
)

@Composable
fun CarromScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("carrom") }
    val scope = rememberCoroutineScope()

    fun initBodies(): List<CarromBody> {
        val list = mutableListOf<CarromBody>()
        list.add(CarromBody(0, CoinType.QUEEN, 0.5f, 0.5f))
        var id = 1
        for (k in 0 until 6) {
            val ang = k * Math.PI / 3.0
            val r = 0.075f
            list.add(
                CarromBody(
                    id = id++,
                    type = if (k % 2 == 0) CoinType.WHITE else CoinType.BLACK,
                    x = 0.5f + (r * cos(ang)).toFloat(),
                    y = 0.5f + (r * sin(ang)).toFloat()
                )
            )
        }
        return list
    }

    var bodies by remember { mutableStateOf(initBodies()) }
    var activeSeat by remember { mutableIntStateOf(0) } // 0=White, 1=Black
    var strikerX by remember { mutableFloatStateOf(0.5f) }
    var isSimulating by remember { mutableStateOf(false) }
    var dragVector by remember { mutableStateOf<Offset?>(null) }
    var scores by remember { mutableStateOf(listOf(0, 0)) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }
    var lastPocketPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    val pocketRipple = remember { Animatable(1f) }
    val strikerPulseInf = rememberInfiniteTransition(label = "carrom_striker")
    val strikerPulseScale by strikerPulseInf.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "carrom_striker_scale"
    )

    val pockets = remember {
        listOf(0.06f to 0.06f, 0.94f to 0.06f, 0.06f to 0.94f, 0.94f to 0.94f)
    }

    fun launchShot(vx: Float, vy: Float) {
        if (isSimulating || winnerSeat != null) return
        val sy = if (activeSeat == 0) 0.84f else 0.16f
        val striker = CarromBody(99, CoinType.STRIKER, strikerX, sy, vx, vy, radius = 0.044f)
        val simList = (bodies.filter { !it.pocketed }.map { it.copy() } + striker).toMutableList()
        isSimulating = true
        onPlayCue(SoundCue.PIECE_SLIDE)

        scope.launch {
            var anyPocketedForCurrent = false
            var lastClackStep = -5
            repeat(150) { step ->
                var moving = false
                for (b in simList) {
                    if (b.pocketed) continue
                    b.x += b.vx
                    b.y += b.vy
                    b.vx *= 0.965f
                    b.vy *= 0.965f
                    if (hypot(b.vx, b.vy) < 0.0008f) { b.vx = 0f; b.vy = 0f }
                    else moving = true

                    if (b.x - b.radius < 0.03f) { b.x = 0.03f + b.radius; b.vx = -b.vx * 0.85f }
                    if (b.x + b.radius > 0.97f) { b.x = 0.97f - b.radius; b.vx = -b.vx * 0.85f }
                    if (b.y - b.radius < 0.03f) { b.y = 0.03f + b.radius; b.vy = -b.vy * 0.85f }
                    if (b.y + b.radius > 0.97f) { b.y = 0.97f - b.radius; b.vy = -b.vy * 0.85f }

                    // Pocket check
                    for ((px, py) in pockets) {
                        if (hypot(b.x - px, b.y - py) < 0.055f) {
                            b.pocketed = true
                            b.vx = 0f; b.vy = 0f
                            if (b.type == CoinType.WHITE) {
                                scores = scores.toMutableList().apply { this[0] += 1 }
                                if (activeSeat == 0) anyPocketedForCurrent = true
                            } else if (b.type == CoinType.BLACK) {
                                scores = scores.toMutableList().apply { this[1] += 1 }
                                if (activeSeat == 1) anyPocketedForCurrent = true
                            } else if (b.type == CoinType.QUEEN) {
                                scores = scores.toMutableList().apply { this[activeSeat] += 3 }
                                anyPocketedForCurrent = true
                            }
                            lastPocketPos = px to py
                            scope.launch {
                                pocketRipple.snapTo(0f)
                                pocketRipple.animateTo(1f, tween(380))
                            }
                            onPlayCue(SoundCue.CAPTURE)
                        }
                    }
                }
                // Elastic collisions
                for (i in simList.indices) {
                    for (j in i + 1 until simList.size) {
                        val a = simList[i]; val b = simList[j]
                        if (a.pocketed || b.pocketed) continue
                        val dx = b.x - a.x; val dy = b.y - a.y
                        val dist = hypot(dx, dy)
                        val minDist = a.radius + b.radius
                        if (dist > 0f && dist < minDist) {
                            val nx = dx / dist; val ny = dy / dist
                            val overlap = minDist - dist
                            a.x -= nx * overlap * 0.5f; a.y -= ny * overlap * 0.5f
                            b.x += nx * overlap * 0.5f; b.y += ny * overlap * 0.5f
                            val dvx = a.vx - b.vx; val dvy = a.vy - b.vy
                            val dot = dvx * nx + dvy * ny
                            if (dot > 0f) {
                                a.vx -= dot * nx * 0.92f; a.vy -= dot * ny * 0.92f
                                b.vx += dot * nx * 0.92f; b.vy += dot * ny * 0.92f
                                if (dot > 0.008f && step - lastClackStep >= 3) {
                                    lastClackStep = step
                                    onPlayCue(SoundCue.WOOD_TAP)
                                }
                            }
                        }
                    }
                }
                bodies = simList.filter { it.type != CoinType.STRIKER }
                if (!moving) return@repeat
                delay(16L)
            }

            val remainingCoins = bodies.count { !it.pocketed && it.type != CoinType.QUEEN }
            if (remainingCoins == 0 || bodies.all { it.pocketed }) {
                winnerSeat = if (scores[0] >= scores[1]) 0 else 1
                onPlayCue(SoundCue.WIN)
            } else if (!anyPocketedForCurrent) {
                activeSeat = 1 - activeSeat
            }
            isSimulating = false
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, isSimulating, winnerSeat) {
        if (winnerSeat == null && !isSimulating && activePlayer.isAi) {
            delay(550L)
            val target = bodies.firstOrNull { !it.pocketed && it.type == CoinType.BLACK } ?: bodies.firstOrNull { !it.pocketed }
            if (target != null) {
                val dx = target.x - strikerX
                val dy = target.y - 0.16f
                val len = hypot(dx, dy).coerceAtLeast(0.01f)
                launchShot((dx / len) * 0.042f, (dy / len) * 0.042f)
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "Score ${scores[0]}–${scores[1]}", scores[w] * 25, 80)
        }
    }

    fun restart() {
        bodies = initBodies(); activeSeat = 0; strikerX = 0.5f; scores = listOf(0, 0); winnerSeat = null; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "Drag back from striker & release to flick",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(players = sessionConfig.players, activeIndex = activeSeat, scores = scores)
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE5CFA4),
                border = BorderStroke(6.dp, Color(0xFF3E2723))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeSeat, isSimulating, strikerX, winnerSeat) {
                            detectDragGestures(
                                onDragStart = { dragVector = Offset.Zero },
                                onDrag = { _, dragAmount ->
                                    if (!isSimulating && !activePlayer.isAi && winnerSeat == null) {
                                        dragVector = (dragVector ?: Offset.Zero) + dragAmount
                                    }
                                },
                                onDragEnd = {
                                    val dv = dragVector
                                    dragVector = null
                                    if (dv != null && !isSimulating && !activePlayer.isAi && winnerSeat == null) {
                                        val mag = hypot(dv.x, dv.y)
                                        if (mag > 15f) {
                                            val scale = (mag / 320f).coerceIn(0.2f, 1.0f) * 0.055f
                                            launchShot((-dv.x / mag) * scale, (-dv.y / mag) * scale)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val w = this.size.width; val h = this.size.height
                    // Pockets & pocket flash ripple
                    pockets.forEach { (px, py) ->
                        drawCircle(Color(0xFF141414), radius = w * 0.052f, center = Offset(px * w, py * h))
                    }
                    lastPocketPos?.let { (px, py) ->
                        val rp = pocketRipple.value
                        if (rp < 0.99f) {
                            drawCircle(
                                color = Color(0xFFFFCA28).copy(alpha = (1f - rp) * 0.85f),
                                radius = w * (0.055f + rp * 0.06f),
                                center = Offset(px * w, py * h),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                    // Center circle & baselines
                    drawCircle(Color(0xFF8D6E63), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(3f))
                    drawLine(Color(0xFF8D6E63), Offset(w * 0.18f, h * 0.84f), Offset(w * 0.82f, h * 0.84f), 3f)
                    drawLine(Color(0xFF8D6E63), Offset(w * 0.18f, h * 0.16f), Offset(w * 0.82f, h * 0.16f), 3f)

                    // Coins
                    bodies.filter { !it.pocketed }.forEach { b ->
                        val c = when (b.type) {
                            CoinType.WHITE -> Color(0xFFFAFAFA)
                            CoinType.BLACK -> Color(0xFF212121)
                            CoinType.QUEEN -> Color(0xFFC62828)
                            CoinType.STRIKER -> Color(0xFF1976D2)
                        }
                        drawCircle(c, radius = b.radius * w, center = Offset(b.x * w, b.y * h))
                        drawCircle(Color.Gray, radius = b.radius * w, center = Offset(b.x * w, b.y * h), style = Stroke(2f))
                    }

                    // Striker & aim indicator when idle
                    if (!isSimulating && winnerSeat == null) {
                        val sy = if (activeSeat == 0) 0.84f else 0.16f
                        val center = Offset(strikerX * w, sy * h)
                        if (!activePlayer.isAi) {
                            drawCircle(
                                color = Color(0xFF1976D2).copy(alpha = 0.25f),
                                radius = 0.044f * w * strikerPulseScale,
                                center = center
                            )
                        }
                        drawCircle(Color(0xFF1976D2), radius = 0.044f * w, center = center)
                        drawCircle(Color.White, radius = 0.044f * w, center = center, style = Stroke(2.5f))
                        dragVector?.let { dv ->
                            drawLine(Color.Red, center, center - dv * 0.6f, strokeWidth = 4f)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        MonoCard(elevated = true) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text("Striker Position", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = strikerX,
                    onValueChange = { if (!isSimulating && !activePlayer.isAi) strikerX = it },
                    valueRange = 0.2f..0.8f
                )
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins Carrom!",
            subtitle = "Final points: ${scores[0]} – ${scores[1]}",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
