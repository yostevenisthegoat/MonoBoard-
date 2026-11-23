package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tactile spring bounce when pressed.
 */
fun Modifier.bouncyPress(enabled: Boolean = true): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bouncy_press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val anyDown = event.changes.any { it.pressed }
                    pressed = anyDown
                }
            }
        }
}

/**
 * Overshoot pop animation whenever [key] changes (e.g., piece placed, tile spawned, score updated).
 */
fun Modifier.springPop(key: Any?, popScale: Float = 1.18f): Modifier = composed {
    val anim = remember { Animatable(1f) }
    LaunchedEffect(key) {
        if (key != null) {
            anim.snapTo(popScale)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }
    this.graphicsLayer {
        scaleX = anim.value
        scaleY = anim.value
    }
}

/**
 * Damped horizontal shake whenever [triggerCount] increments (> 0).
 */
fun Modifier.shakeOnTrigger(triggerCount: Int): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            val sequence = listOf(-18f, 16f, -12f, 10f, -6f, 4f, 0f)
            for (target in sequence) {
                offsetX.animateTo(target, tween(34))
            }
        }
    }
    this.offset { IntOffset(offsetX.value.roundToInt(), 0) }
}

/**
 * Subtle breathing pulse for active turns, check alerts, or highlights.
 */
fun Modifier.pulseEffect(active: Boolean, maxScale: Float = 1.07f): Modifier = composed {
    if (!active) return@composed this
    val inf = rememberInfiniteTransition(label = "pulse")
    val scale by inf.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Smooth 3D card flip between front and back faces.
 */
@Composable
fun FlipCardBox(
    faceUp: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 0f else 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "card_flip"
    )
    val density = LocalDensity.current.density
    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density
        }
    ) {
        if (rotation <= 90f) {
            front()
        } else {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                back()
            }
        }
    }
}

/**
 * Animated tumbling 2D/isometric die with pips and settle bounce.
 */
@Composable
fun AnimatedDieView(
    value: Int,
    isRolling: Boolean,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var displayValue by remember(value) { mutableStateOf(value.coerceIn(1, 6)) }

    LaunchedEffect(isRolling, value) {
        if (isRolling) {
            for (step in 0 until 7) {
                displayValue = ((displayValue + step) % 6) + 1
                rotation.animateTo((step + 1) * 45f, tween(45))
                delay(10)
            }
            displayValue = value.coerceIn(1, 6)
            rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        } else {
            displayValue = value.coerceIn(1, 6)
            scale.snapTo(1.22f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation.value
                scaleX = scale.value
                scaleY = scale.value
            },
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val r = w * 0.095f
            val pts: List<Pair<Float, Float>> = when (displayValue) {
                1 -> listOf(0.5f to 0.5f)
                2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
                3 -> listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f)
                4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
                5 -> listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.5f to 0.5f, 0.26f to 0.74f, 0.74f to 0.74f)
                else -> listOf(
                    0.28f to 0.24f, 0.72f to 0.24f,
                    0.28f to 0.5f, 0.72f to 0.5f,
                    0.28f to 0.76f, 0.72f to 0.76f
                )
            }
            pts.forEach { (px, py) ->
                drawCircle(color = fg, radius = r, center = Offset(w * px, h * py))
            }
        }
    }
}

/**
 * Animated integer/string badge pop transition.
 */
@Composable
fun AnimatedCounterText(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (scaleIn(initialScale = 0.8f) + fadeIn(tween(140)))
                .togetherWith(scaleOut(targetScale = 1.1f) + fadeOut(tween(110)))
        },
        label = "counter_pop"
    ) { value ->
        Text(text = value, style = style, color = color, fontWeight = fontWeight)
    }
}

/**
 * Celebration confetti burst canvas for win dialogs & solved puzzles.
 */
@Composable
fun ConfettiBurstOverlay(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val rng = Random(42)
        val colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFBDBDBD),
            Color(0xFF4CAF50),
            Color(0xFF42A5F5),
            Color(0xFFFFCA28),
            Color(0xFFEF5350)
        )
        List(36) { i ->
            val angle = (i / 36.0) * 2.0 * PI + rng.nextDouble() * 0.25
            val speed = 0.45f + rng.nextFloat() * 0.55f
            Triple(angle, speed, colors[i % colors.size])
        }
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier) {
        val p = progress.value
        if (p >= 0.99f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxDist = size.minDimension * 0.65f
        val alpha = (1f - p).coerceIn(0f, 1f)
        particles.forEach { (ang, spd, col) ->
            val dist = maxDist * spd * p
            val x = cx + (cos(ang) * dist).toFloat()
            val y = cy + (sin(ang) * dist).toFloat() + (p * p * 48f)
            drawCircle(
                color = col.copy(alpha = alpha),
                radius = (1f - p * 0.5f) * 5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
