package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class StarCell(val q: Int, val r: Int, val homeSeat: Int) // axial hex coords

private val starBoardCells: List<StarCell> by lazy {
    val cells = mutableListOf<StarCell>()
    for (r in -8..8) {
        for (q in -8..8) {
            val s = -q - r
            val inA = kotlin.math.abs(q) <= 4 && kotlin.math.abs(r) <= 4 && kotlin.math.abs(s) <= 4
            val inTop = r < -4 && q in 0..(-r - 4) && s in 0..(-r - 4)
            val inBottom = r > 4 && q in (-r + 4)..0 && s in (-r + 4)..0
            val inTopRight = q > 4 && r in (-q + 4)..0 && s in (-q + 4)..0
            val inBotLeft = q < -4 && r in 0..(-q - 4) && s in 0..(-q - 4)
            val inTopLeft = s > 4 && q in (-s + 4)..0 && r in (-s + 4)..0
            val inBotRight = s < -4 && q in 0..(-s - 4) && r in 0..(-s - 4)
            if (inA || inTop || inBottom || inTopRight || inBotLeft || inTopLeft || inBotRight) {
                val home = when {
                    inBottom -> 0
                    inTop -> 1
                    inTopRight -> 2
                    inBotLeft -> 3
                    else -> -1
                }
                cells.add(StarCell(q, r, home))
            }
        }
    }
    cells
}

private val hexDirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to -1, -1 to 1)
private val ccColors = listOf(Color(0xFFFFFFFF), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726))

@Composable
fun ChineseCheckersScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("chinese_checkers") }
    val playerCount = sessionConfig.players.size
    val cellSet = remember { starBoardCells.map { it.q to it.r }.toSet() }

    fun homeOwnerForSeat(seat: Int): Int = when {
        playerCount == 2 && seat == 1 -> 1
        else -> seat
    }
    fun goalHomeForSeat(seat: Int): Int = when (homeOwnerForSeat(seat)) {
        0 -> 1; 1 -> 0; 2 -> 3; else -> 2
    }

    fun initialPieces(): Map<Pair<Int, Int>, Int> {
        val map = mutableMapOf<Pair<Int, Int>, Int>()
        for (seat in 0 until playerCount) {
            val h = homeOwnerForSeat(seat)
            starBoardCells.filter { it.homeSeat == h }.forEach { map[it.q to it.r] = seat }
        }
        return map
    }

    var pieces by remember(playerCount) { mutableStateOf(initialPieces()) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun reachableFrom(pos: Pair<Int, Int>): Set<Pair<Int, Int>> {
        val dests = mutableSetOf<Pair<Int, Int>>()
        // Single adjacent steps
        for ((dq, dr) in hexDirs) {
            val n = (pos.first + dq) to (pos.second + dr)
            if (n in cellSet && n !in pieces) dests.add(n)
        }
        // BFS chain hops
        val visited = mutableSetOf(pos)
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(pos)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for ((dq, dr) in hexDirs) {
                val mid = (cur.first + dq) to (cur.second + dr)
                val land = (cur.first + 2 * dq) to (cur.second + 2 * dr)
                if (mid in pieces && land in cellSet && land !in pieces && land !in visited) {
                    visited.add(land)
                    dests.add(land)
                    queue.add(land)
                }
            }
        }
        return dests
    }

    val validDests = remember(selected, pieces) { selected?.let { reachableFrom(it) } ?: emptySet() }

    fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val next = pieces.toMutableMap()
        val seat = next.remove(from) ?: return
        next[to] = seat
        pieces = next
        selected = null
        onPlayCue(SoundCue.MOVE)

        val goalCells = starBoardCells.filter { it.homeSeat == goalHomeForSeat(seat) }.map { it.q to it.r }
        if (goalCells.all { next[it] == seat }) {
            winnerSeat = seat
            onPlayCue(SoundCue.WIN)
        } else {
            activeSeat = (activeSeat + 1) % playerCount
        }
    }

    val activePlayer = sessionConfig.players[activeSeat]
    LaunchedEffect(activeSeat, pieces, winnerSeat) {
        if (winnerSeat == null && activePlayer.isAi) {
            delay(360L)
            val goal = starBoardCells.filter { it.homeSeat == goalHomeForSeat(activeSeat) }.map { it.q to it.r }.firstOrNull()
            val myPieces = pieces.filterValues { it == activeSeat }.keys.toList()
            val allMoves = myPieces.flatMap { src -> reachableFrom(src).map { dst -> src to dst } }
            val best = if (goal != null && allMoves.isNotEmpty()) {
                allMoves.minBy { (src, dst) ->
                    val dDst = kotlin.math.abs(dst.first - goal.first) + kotlin.math.abs(dst.second - goal.second)
                    val dSrc = kotlin.math.abs(src.first - goal.first) + kotlin.math.abs(src.second - goal.second)
                    dDst - dSrc
                }
            } else null
            best?.let { movePiece(it.first, it.second) }
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[w]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} filled opposite triangle", 100, 120)
        }
    }

    fun restart() {
        pieces = initialPieces(); activeSeat = 0; selected = null; winnerSeat = null; recorded = false
    }

    GameScaffold(
        game = game,
        subtitle = "${playerCount}-Player Hex Star",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(players = sessionConfig.players, activeIndex = activeSeat)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(boardSize),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wPx = constraints.maxWidth.toFloat()
                    val hPx = constraints.maxHeight.toFloat()
                    val cx = wPx / 2f
                    val cy = hPx / 2f
                    val spacing = wPx / 18.5f

                    starBoardCells.forEach { cell ->
                        val coord = cell.q to cell.r
                        val px = cx + (cell.q + cell.r * 0.5f) * spacing
                        val py = cy + (cell.r * 0.866f) * spacing
                        val owner = pieces[coord]
                        val isSel = selected == coord
                        val isDest = coord in validDests
                        val dotDp = 18.dp
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((px - dotDp.toPx() / 2f).roundToInt(), (py - dotDp.toPx() / 2f).roundToInt()) }
                                .size(dotDp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        owner != null -> ccColors[owner % ccColors.size]
                                        isDest -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable(enabled = !activePlayer.isAi && winnerSeat == null) {
                                    if (isDest && selected != null) {
                                        movePiece(selected!!, coord)
                                    } else if (owner == activeSeat) {
                                        selected = coord
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Wins!",
            subtitle = "All 10 marbles arrived at the opposite star point.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
