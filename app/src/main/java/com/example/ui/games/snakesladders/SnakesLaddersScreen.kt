package com.example.ui.games.snakesladders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.PassAndPlayTurnHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ladders = mapOf(
    4 to 25,
    13 to 46,
    33 to 49,
    42 to 63,
    50 to 69,
    62 to 81,
    74 to 92
)

private val snakes = mapOf(
    27 to 5,
    40 to 3,
    43 to 18,
    54 to 31,
    66 to 45,
    76 to 58,
    89 to 53,
    99 to 41
)

private val tokenColors = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF4FC3F7),
    Color(0xFFFFB74D),
    Color(0xFF81C784)
)

@Composable
fun SnakesLaddersScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("snakes_ladders") }
    val bounceBack = sessionConfig.ruleVariant.contains("Bounce", ignoreCase = true)
    val scope = rememberCoroutineScope()

    val positions = remember {
        mutableStateListOf<Int>().apply { repeat(sessionConfig.players.size) { add(1) } }
    }
    var currentPlayer by remember { mutableIntStateOf(sessionConfig.startingPlayerIndex) }
    var diceValue by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap Roll Dice to start.") }
    var winnerIdx by remember { mutableStateOf<Int?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var outcomeRecorded by remember { mutableStateOf(false) }

    LaunchedEffect(winnerIdx) {
        while (winnerIdx == null) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    LaunchedEffect(winnerIdx) {
        val w = winnerIdx
        if (w != null && !outcomeRecorded) {
            outcomeRecorded = true
            onPlayCue(SoundCue.WIN)
            val player = sessionConfig.players[w]
            val outcome = if (player.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
            onRecordOutcome(outcome, player.name, "${player.name} reached square 100!", 100, elapsedSeconds)
        }
    }

    fun performTurn() {
        if (isRolling || winnerIdx != null) return
        val seat = currentPlayer
        val player = sessionConfig.players[seat]
        scope.launch {
            isRolling = true
            repeat(6) {
                diceValue = (1..6).random()
                delay(65L)
            }
            val roll = (1..6).random()
            diceValue = roll
            onPlayCue(SoundCue.MOVE)

            val startPos = positions[seat]
            var target = startPos + roll
            if (target > 100) {
                target = if (bounceBack) 100 - (target - 100) else startPos
            }

            if (target != startPos) {
                val step = if (target > startPos) 1 else -1
                var cursor = startPos
                while (cursor != target) {
                    cursor += step
                    positions[seat] = cursor
                    delay(120L)
                }
            }

            val ladderDest = ladders[positions[seat]]
            val snakeDest = snakes[positions[seat]]
            when {
                ladderDest != null -> {
                    statusText = "${player.name} rolled $roll and climbed ladder ${positions[seat]} → $ladderDest!"
                    onPlayCue(SoundCue.TURN_PASS)
                    delay(240L)
                    positions[seat] = ladderDest
                }
                snakeDest != null -> {
                    statusText = "${player.name} rolled $roll and slid down snake ${positions[seat]} → $snakeDest!"
                    onPlayCue(SoundCue.CAPTURE)
                    delay(240L)
                    positions[seat] = snakeDest
                }
                else -> {
                    statusText = "${player.name} rolled $roll → Square ${positions[seat]}"
                }
            }

            if (positions[seat] == 100) {
                winnerIdx = seat
            } else if (roll == 6) {
                statusText += " (Bonus roll on 6!)"
            } else {
                currentPlayer = (currentPlayer + 1) % sessionConfig.players.size
            }
            isRolling = false
        }
    }

    // AI auto-roll
    val activePlayer = sessionConfig.players[currentPlayer]
    LaunchedEffect(currentPlayer, isRolling, winnerIdx) {
        if (winnerIdx == null && !isRolling && activePlayer.isAi) {
            delay(500L)
            performTurn()
        }
    }

    fun restart() {
        for (i in positions.indices) positions[i] = 1
        currentPlayer = sessionConfig.startingPlayerIndex
        diceValue = 1
        isRolling = false
        statusText = "New match ready. Tap Roll Dice."
        winnerIdx = null
        elapsedSeconds = 0
        outcomeRecorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${sessionConfig.players.size} Players • ${sessionConfig.ruleVariant}",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = currentPlayer,
            scores = positions.toList(),
            statusMessage = statusText
        )

        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(boardSize),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (visualRow in 0..9) {
                            val boardRowFromBottom = 9 - visualRow
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (visualCol in 0..9) {
                                    val colInRow = if (boardRowFromBottom % 2 == 0) visualCol else 9 - visualCol
                                    val squareNum = boardRowFromBottom * 10 + colInRow + 1
                                    val isDarkTile = (visualRow + visualCol) % 2 == 1
                                    val tileColor = when {
                                        squareNum == 100 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        squareNum in ladders -> Color(0xFF2E7D32).copy(alpha = 0.22f)
                                        squareNum in snakes -> Color(0xFFC62828).copy(alpha = 0.22f)
                                        isDarkTile -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    val occupants = positions.mapIndexedNotNull { idx, pos -> if (pos == squareNum) idx else null }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .background(tileColor)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            .padding(1.5.dp)
                                    ) {
                                        Text(
                                            text = squareNum.toString(),
                                            fontSize = 8.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                        if (occupants.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.align(Alignment.BottomEnd),
                                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                occupants.forEach { pIdx ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(tokenColors[pIdx % tokenColors.size])
                                                            .border(1.dp, Color.Black, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Draw clean vector Ladders and Snakes overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        fun centerOf(sq: Int): Offset {
                            val zero = sq - 1
                            val rBottom = zero / 10
                            val cRaw = zero % 10
                            val visualCol = if (rBottom % 2 == 0) cRaw else 9 - cRaw
                            val visualRow = 9 - rBottom
                            val cellW = size.width / 10f
                            val cellH = size.height / 10f
                            return Offset((visualCol + 0.5f) * cellW, (visualRow + 0.5f) * cellH)
                        }

                        ladders.forEach { (start, end) ->
                            val p1 = centerOf(start)
                            val p2 = centerOf(end)
                            drawLine(
                                color = Color(0xFF66BB6A).copy(alpha = 0.65f),
                                start = p1,
                                end = p2,
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                        snakes.forEach { (start, end) ->
                            val p1 = centerOf(start)
                            val p2 = centerOf(end)
                            val mid = Offset((p1.x + p2.x) / 2f + 22f, (p1.y + p2.y) / 2f)
                            val path = Path().apply {
                                moveTo(p1.x, p1.y)
                                quadraticTo(mid.x, mid.y, p2.x, p2.y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFEF5350).copy(alpha = 0.65f),
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dice & Roll Action Footer
        MonoCard(elevated = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = diceValue.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column {
                        Text(activePlayer.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Square ${positions[currentPlayer]} / 100",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = ::performTurn,
                    enabled = !isRolling && winnerIdx == null && !activePlayer.isAi,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("roll_dice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRolling) "Rolling..." else "Roll Dice")
                }
            }
        }
    }

    if (winnerIdx != null) {
        val wName = sessionConfig.players[winnerIdx!!].name
        GameCompletionDialog(
            title = "$wName Reached 100!",
            subtitle = "Victory in Snakes & Ladders.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
