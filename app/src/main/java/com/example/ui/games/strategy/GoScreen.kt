package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun GoScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("go") }
    val size = 9
    var board by remember { mutableStateOf(List(size) { List(size) { 0 } }) } // 1=Black, 2=White
    var previousBoardKey by remember { mutableStateOf("") }
    var activeSeat by remember { mutableIntStateOf(0) }
    var captures by remember { mutableStateOf(listOf(0, 0)) }
    var consecutivePasses by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }

    fun boardKey(b: List<List<Int>>): String = b.joinToString("") { r -> r.joinToString("") }

    fun groupAndLiberties(b: List<List<Int>>, r: Int, c: Int): Pair<Set<Pair<Int, Int>>, Set<Pair<Int, Int>>> {
        val color = b[r][c]
        val group = mutableSetOf<Pair<Int, Int>>()
        val liberties = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(r to c)
        group.add(r to c)
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        while (queue.isNotEmpty()) {
            val (cr, cc) = queue.removeFirst()
            for ((dr, dc) in dirs) {
                val nr = cr + dr; val nc = cc + dc
                if (nr in 0 until size && nc in 0 until size) {
                    when (b[nr][nc]) {
                        0 -> liberties.add(nr to nc)
                        color -> if ((nr to nc) !in group) {
                            group.add(nr to nc)
                            queue.add(nr to nc)
                        }
                    }
                }
            }
        }
        return group to liberties
    }

    fun tryPlaceStone(r: Int, c: Int): Boolean {
        if (board[r][c] != 0 || gameOver) return false
        val p = activeSeat + 1
        val opp = (1 - activeSeat) + 1
        val next = board.map { it.toMutableList() }
        next[r][c] = p
        var capCount = 0
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until size && nc in 0 until size && next[nr][nc] == opp) {
                val (grp, libs) = groupAndLiberties(next, nr, nc)
                if (libs.isEmpty()) {
                    grp.forEach { (gr, gc) -> next[gr][gc] = 0 }
                    capCount += grp.size
                }
            }
        }
        val (_, ownLibs) = groupAndLiberties(next, r, c)
        if (ownLibs.isEmpty()) return false // Suicide rule
        val immutable = next.map { it.toList() }
        val key = boardKey(immutable)
        if (key == previousBoardKey) return false // Ko rule

        previousBoardKey = boardKey(board)
        board = immutable
        captures = captures.toMutableList().apply { this[activeSeat] += capCount }
        consecutivePasses = 0
        activeSeat = 1 - activeSeat
        onPlayCue(if (capCount > 0) SoundCue.CAPTURE else SoundCue.MOVE)
        return true
    }

    fun passTurn() {
        consecutivePasses++
        onPlayCue(SoundCue.TURN_PASS)
        if (consecutivePasses >= 2) {
            gameOver = true
        } else {
            activeSeat = 1 - activeSeat
        }
    }

    fun areaScores(): Pair<Int, Double> {
        var bScore = 0
        var wScore = 5.5 // Komi
        val visited = Array(size) { BooleanArray(size) }
        for (r in 0 until size) for (c in 0 until size) {
            when (board[r][c]) {
                1 -> bScore++
                2 -> wScore += 1.0
                0 -> if (!visited[r][c]) {
                    val region = mutableListOf<Pair<Int, Int>>()
                    val borders = mutableSetOf<Int>()
                    val q = ArrayDeque<Pair<Int, Int>>()
                    q.add(r to c); visited[r][c] = true
                    while (q.isNotEmpty()) {
                        val (cr, cc) = q.removeFirst()
                        region.add(cr to cc)
                        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                            val nr = cr + dr; val nc = cc + dc
                            if (nr in 0 until size && nc in 0 until size) {
                                when (board[nr][nc]) {
                                    0 -> if (!visited[nr][nc]) { visited[nr][nc] = true; q.add(nr to nc) }
                                    else -> borders.add(board[nr][nc])
                                }
                            }
                        }
                    }
                    if (borders == setOf(1)) bScore += region.size
                    if (borders == setOf(2)) wScore += region.size
                }
            }
        }
        return bScore to wScore
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && !gameOver
    LaunchedEffect(board, activeSeat, isAiTurn) {
        if (isAiTurn) {
            delay(360L)
            val candidates = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until size) for (c in 0 until size) if (board[r][c] == 0) candidates.add(r to c)
            val placed = candidates.shuffled().any { (r, c) -> tryPlaceStone(r, c) }
            if (!placed) passTurn()
        }
    }

    LaunchedEffect(gameOver) {
        if (gameOver && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val (bs, ws) = areaScores()
            if (bs > ws) {
                onRecordOutcome(MatchOutcome.WIN, sessionConfig.players[0].name, "Black $bs vs White $ws", bs, 100)
            } else {
                val p2 = sessionConfig.players[1]
                onRecordOutcome(if (p2.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, p2.name, "White $ws vs Black $bs", ws.toInt(), 100)
            }
        }
    }

    fun restart() {
        board = List(size) { List(size) { 0 } }
        previousBoardKey = ""
        activeSeat = 0; captures = listOf(0, 0); consecutivePasses = 0; gameOver = false; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "9×9 Go • Komi 5.5",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = activeSeat,
            scores = captures,
            statusMessage = "Scores shown = captured stones"
        )
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight, 360.dp)
            Surface(
                modifier = Modifier.size(boardSize),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD7B67A),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wPx = constraints.maxWidth.toFloat()
                    val pad = wPx * 0.08f
                    val step = (wPx - 2 * pad) / (size - 1)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until size) {
                            drawLine(Color(0xFF3E2723), Offset(pad, pad + i * step), Offset(wPx - pad, pad + i * step), 2.5f)
                            drawLine(Color(0xFF3E2723), Offset(pad + i * step, pad), Offset(pad + i * step, wPx - pad), 2.5f)
                        }
                        listOf(2 to 2, 2 to 6, 4 to 4, 6 to 2, 6 to 6).forEach { (r, c) ->
                            drawCircle(Color(0xFF3E2723), radius = 5f, center = Offset(pad + c * step, pad + r * step))
                        }
                    }

                    for (r in 0 until size) {
                        for (c in 0 until size) {
                            val st = board[r][c]
                            val touchSize = (boardSize / 9.5f)
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (pad + c * step - touchSize.toPx() / 2f).roundToInt(),
                                            (pad + r * step - touchSize.toPx() / 2f).roundToInt()
                                        )
                                    }
                                    .size(touchSize)
                                    .clickable(enabled = !isAiTurn && !gameOver && st == 0) {
                                        tryPlaceStone(r, c)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (st != 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.84f)
                                            .clip(CircleShape)
                                            .background(if (st == 1) Color(0xFF121212) else Color(0xFFFDFDFD))
                                            .border(1.dp, Color.DarkGray, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = ::passTurn,
                enabled = !isAiTurn && !gameOver,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Pass Turn") }
        }
    }

    if (gameOver) {
        val (bs, ws) = areaScores()
        val winner = if (bs > ws) sessionConfig.players[0].name else sessionConfig.players[1].name
        GameCompletionDialog(
            title = "$winner Wins Go!",
            subtitle = "Black $bs  vs  White $ws (incl. 5.5 Komi)",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
