package com.example.ui.games.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlipCardBox
import com.example.ui.components.bouncyPress
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily

enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false),
    HEARTS("♥", true),
    CLUBS("♣", false),
    DIAMONDS("♦", true)
}

enum class Rank(val label: String, val value: Int) {
    ACE("A", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13)
}

data class PlayingCard(
    val suit: Suit,
    val rank: Rank,
    val faceUp: Boolean = true
) {
    val label: String get() = "${rank.label}${suit.symbol}"
    val blackjackValue: Int
        get() = when {
            rank == Rank.ACE -> 11
            rank.value >= 10 -> 10
            else -> rank.value
        }
}

fun standardDeck(faceUp: Boolean = true): List<PlayingCard> =
    Suit.entries.flatMap { s -> Rank.entries.map { r -> PlayingCard(s, r, faceUp) } }

@Composable
fun PlayingCardView(
    card: PlayingCard?,
    selected: Boolean = false,
    width: Dp = 54.dp,
    height: Dp = 76.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val liftScale by animateFloatAsState(
        targetValue = if (selected) 1.09f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_lift"
    )
    val liftY by animateFloatAsState(
        targetValue = if (selected) -8f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_y"
    )

    val interactiveModifier = modifier
        .size(width = width, height = height)
        .graphicsLayer {
            scaleX = liftScale
            scaleY = liftScale
            translationY = liftY
        }
        .springPop(card?.label, popScale = 1.07f)
        .then(if (onClick != null) Modifier.bouncyPress() else Modifier)
        .clip(RoundedCornerShape(8.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (card == null) {
        Surface(
            modifier = interactiveModifier,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
        return
    }

    val suitColor = if (card.suit.isRed) Color(0xFFD32F2F) else Color(0xFF141414)
    val frontBg = if (selected) Color(0xFFE0E0E0) else Color(0xFFFDFDFD)

    FlipCardBox(
        faceUp = card.faceUp,
        modifier = interactiveModifier,
        front = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = frontBg,
                border = BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = suitColor
                    )
                    Text(
                        text = card.suit.symbol,
                        fontSize = 18.sp,
                        color = suitColor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = card.rank.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = suitColor,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        },
        back = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("◈", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
