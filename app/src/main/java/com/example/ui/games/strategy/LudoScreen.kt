package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.AnimatedDieView
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 15x15 Ludo grid track coordinates (52 outer squares clockwise starting from Red/Player0 entry (6,1))
private val ludoOuterTrack = listOf(
    6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
    5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
    0 to 7, 0 to 8,
    1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
    6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
    7 to 14, 8 to 14,
    8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
    9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
    14 to 7, 14 to 6,
    13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
    8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
    7 to 0, 6 to 0
)

private val ludoSafeTrackIndices = setOf(0, 8, 13, 21, 26, 34, 39, 47)
private val ludoStartOffsets = listOf(0, 13, 26, 39)
private val ludoHomeColumns = listOf(
    listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5, 7 to 6),
    listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7, 6 to 7),
    listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9, 7 to 8),
    listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7, 8 to 7)
)
private val ludoYardSlots = listOf(
    listOf(2 to 2, 2 to 3, 3 to 2, 3 to 3),
    listOf(2 to 11, 2 to 12, 3 to 11, 3 to 12),
    listOf(11 to 11, 11 to 12, 12 to 11, 12 to 12),
    listOf(11 to 2, 11 to 3, 12 to 2, 12 to 3)
)
private val ludoPlayerColors = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFDD835)
)

@Composable
fun LudoScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("ludo") }
    // Token progress: -1 = in yard, 0..50 = outer track steps from player's start, 51..56 = home column (56 = Home!)
    var tokens by remember(sessionConfig.players.size) {
        mutableStateOf(List(sessionConfig.players.size) { List(4) { -1 } })
    }
    var activeSeat by remember { mutableIntStateOf(0) }
    var rolledValue by remember { mutableStateOf<Int?>(null) }
    var lastRolledFace by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Roll dice to begin.") }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun seatTrackColorIndex(seat: Int): Int {
        return if (sessionConfig.players.size == 2 && seat == 1) 2 else seat
    }

    fun movableTokens(seat: Int, roll: Int): List<Int> {
        return tokens[seat].indices.filter { idx ->
            val p = tokens[seat][idx]
            if (p == -1) roll == 6 else p + roll <= 56
        }
    }

    fun globalTrackIndex(seat: Int, progress: Int): Int? {
        if (progress !in 0..50) return null
        val colorIdx = seatTrackColorIndex(seat)
        return (ludoStartOffsets[colorIdx] + progress) % 52
    }

    fun tokenCoord(seat: Int, tokenIdx: Int): Pair<Int, Int> {
        val progress = tokens[seat][tokenIdx]
        val colorIdx = seatTrackColorIndex(seat)
        return when {
            progress == -1 -> ludoYardSlots[colorIdx][tokenIdx]
            progress in 0..50 -> ludoOuterTrack[(ludoStartOffsets[colorIdx] + progress) % 52]
            else -> ludoHomeColumns[colorIdx][(progress - 51).coerceIn(0, 5)]
        }
    }

    fun moveToken(tokenIdx: Int) {
        val roll = rolledValue ?: return
        val next = tokens.map { it.toMutableList() }
        val cur = next[activeSeat][tokenIdx]
        val nextPos = if (cur == -1) 0 else cur + roll
        next[activeSeat][tokenIdx] = nextPos

        var captured = false
        val landedGlobal = globalTrackIndex(activeSeat, nextPos)
        if (landedGlobal != null && landedGlobal !in ludoSafeTrackIndices) {
            for (other in next.indices) {
                if (other == activeSeat) continue
                for (oi in 0..3) {
                    if (globalTrackIndex(other, next[other][oi]) == landedGlobal) {
                        next[other][oi] = -1
                        captured = true
                    }
                }
            }
        }

        tokens = next.map { it.toList() }
        rolledValue = null

        if (next[activeSeat].all { it == 56 }) {
            winnerSeat = activeSeat
            onPlayCue(SoundCue.WIN)
            return
        }

        onPlayCue(if (captured) SoundCue.CAPTURE else SoundCue.BOUNCE)
        if (roll == 6 || captured || nextPos == 56) {
            statusText = "${sessionConfig.players[activeSeat].name} earned a bonus roll!"
        } else {
            activeSeat = (activeSeat + 1) % sessionConfig.players.size
            statusText = "${sessionConfig.players[activeSeat].name}'s turn — Roll Dice"
        }
    }

    fun rollDice() {
        if (isRolling || rolledValue != null || winnerSeat != null) return
        isRolling = true
        onPlayCue(SoundCue.DICE_ROLL)
        scope.launch {
            delay(360L)
            val r = (1..6).random()
            lastRolledFace = r
            isRolling = false
            rolledValue = r
            val movable = movableTokens(activeSeat, r)
            if (movable.isEmpty()) {
                statusText = "${sessionConfig.players[activeSeat].name} rolled $r — no legal moves."
                onPlayCue(SoundCue.TURN_PASS)
                rolledValue = null
                activeSeat = (activeSeat + 1) % sessionConfig.players.size
            } else if (movable.size == 1 && !sessionConfig.players[activeSeat].isAi) {
                moveToken(movable.first())
            } else {
                statusText = "${sessionConfig.players[activeSeat].name} rolled $r — pick a highlighted token."
            }
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, rolledValue, isRolling, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi && !isRolling) {
            delay(450L)
            if (rolledValue == null) {
                rollDice()
            } else {
                val movable = movableTokens(activeSeat, rolledValue!!)
                if (movable.isNotEmpty()) {
                    // Prefer exiting yard or reaching home or advancing furthest
                    val pick = movable.maxBy { tokens[activeSeat][it] }
                    moveToken(pick)
                }
            }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "All 4 tokens home", 100, 120)
        }
    }

    fun restart() {
        tokens = List(sessionConfig.players.size) { List(4) { -1 } }
        activeSeat = 0; rolledValue = null; lastRolledFace = 1; isRolling = false; winnerSeat = null; recorded = false
        statusText = "Roll dice to begin."
    }

    val homeCounts = tokens.map { seatTokens -> seatTokens.count { it == 56 } }
    val movableSet = rolledValue?.let { movableTokens(activeSeat, it).toSet() } ?: emptySet()

    GameScaffold(
        game = game,
        subtitle = "${sessionConfig.players.size} Players",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = homeCounts,
            statusMessage = statusText
        )
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(boardSize),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    for (r in 0..14) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..14) {
                                val trackIdx = ludoOuterTrack.indexOf(r to c)
                                val isSafeStar = trackIdx in ludoSafeTrackIndices
                                val homeColorIdx = ludoHomeColumns.indexOfFirst { (r to c) in it }
                                val yardIdx = when {
                                    r < 6 && c < 6 -> 0
                                    r < 6 && c > 8 -> 1
                                    r > 8 && c > 8 -> 2
                                    r > 8 && c < 6 -> 3
                                    else -> -1
                                }
                                val isCenterGoal = r in 6..8 && c in 6..8

                                val bg = when {
                                    isCenterGoal -> MaterialTheme.colorScheme.surfaceVariant
                                    yardIdx != -1 -> ludoPlayerColors[yardIdx].copy(alpha = 0.2f)
                                    homeColorIdx != -1 -> ludoPlayerColors[homeColorIdx].copy(alpha = 0.32f)
                                    trackIdx != -1 -> MaterialTheme.colorScheme.background
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                val tokensHere = mutableListOf<Pair<Int, Int>>()
                                tokens.forEachIndexed { sIdx, seatList ->
                                    seatList.forEachIndexed { tIdx, _ ->
                                        if (tokenCoord(sIdx, tIdx) == (r to c)) tokensHere.add(sIdx to tIdx)
                                    }
                                }
                                val clickableToken = tokensHere.firstOrNull { (s, t) -> s == activeSeat && t in movableSet }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(bg)
                                        .border(0.4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                        .then(
                                            if (clickableToken != null && !activePlayer.isAi) {
                                                Modifier.clickable { moveToken(clickableToken.second) }
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSafeStar && tokensHere.isEmpty()) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = "Safe Square",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    if (tokensHere.isNotEmpty()) {
                                        val (sIdx, tIdx) = tokensHere.first()
                                        val isMovable = sIdx == activeSeat && tIdx in movableSet
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.75f)
                                                .springPop(sIdx to tIdx, popScale = 1.22f)
                                                .pulseEffect(isMovable && !activePlayer.isAi, maxScale = 1.18f)
                                                .clip(CircleShape)
                                                .background(ludoPlayerColors[seatTrackColorIndex(sIdx)])
                                                .border(
                                                    width = if (isMovable) 2.5.dp else 1.dp,
                                                    color = Color.White,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        MonoCard(elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedDieView(
                        value = lastRolledFace,
                        isRolling = isRolling,
                        size = 42.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(ludoPlayerColors[seatTrackColorIndex(activeSeat)])
                    )
                    Text(
                        text = if (isRolling) "Rolling..." else rolledValue?.let { "Rolled: $it" } ?: "${activePlayer.name}'s Turn",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
                val canRoll = !isRolling && rolledValue == null && !activePlayer.isAi && winnerSeat == null
                Button(
                    onClick = ::rollDice,
                    enabled = canRoll,
                    modifier = Modifier.bouncyPress(canRoll)
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Roll")
                }
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins Ludo!",
            subtitle = "All 4 tokens safely reached the center home.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
