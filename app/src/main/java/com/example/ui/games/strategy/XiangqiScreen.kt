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
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.components.springPop
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class XqSide { RED, BLACK }
private enum class XqType(val redLabel: String, val blackLabel: String, val value: Int) {
    GENERAL("帥", "將", 10000),
    ADVISOR("仕", "士", 120),
    ELEPHANT("相", "象", 120),
    HORSE("傌", "馬", 270),
    CHARIOT("俥", "車", 600),
    CANNON("炮", "砲", 285),
    SOLDIER("兵", "卒", 60)
}
private data class XqPiece(val side: XqSide, val type: XqType)

@Composable
fun XiangqiScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("xiangqi") }

    fun initBoard(): List<List<XqPiece?>> {
        val g = MutableList(10) { MutableList<XqPiece?>(9) { null } }
        val back = listOf(XqType.CHARIOT, XqType.HORSE, XqType.ELEPHANT, XqType.ADVISOR, XqType.GENERAL, XqType.ADVISOR, XqType.ELEPHANT, XqType.HORSE, XqType.CHARIOT)
        for (c in 0..8) {
            g[0][c] = XqPiece(XqSide.BLACK, back[c])
            g[9][c] = XqPiece(XqSide.RED, back[c])
        }
        g[2][1] = XqPiece(XqSide.BLACK, XqType.CANNON); g[2][7] = XqPiece(XqSide.BLACK, XqType.CANNON)
        g[7][1] = XqPiece(XqSide.RED, XqType.CANNON); g[7][7] = XqPiece(XqSide.RED, XqType.CANNON)
        for (c in listOf(0, 2, 4, 6, 8)) {
            g[3][c] = XqPiece(XqSide.BLACK, XqType.SOLDIER)
            g[6][c] = XqPiece(XqSide.RED, XqType.SOLDIER)
        }
        return g.map { it.toList() }
    }

    var board by remember { mutableStateOf(initBoard()) }
    var turn by remember { mutableStateOf(XqSide.RED) }
    val undoStack = remember { mutableStateListOf<Pair<List<List<XqPiece?>>, XqSide>>() }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var winner by remember { mutableStateOf<XqSide?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun legalMovesFrom(b: List<List<XqPiece?>>, r: Int, c: Int): List<Pair<Int, Int>> {
        val p = b[r][c] ?: return emptyList()
        val out = mutableListOf<Pair<Int, Int>>()
        fun addIfOk(nr: Int, nc: Int) {
            if (nr in 0..9 && nc in 0..8 && b[nr][nc]?.side != p.side) out.add(nr to nc)
        }
        when (p.type) {
            XqType.GENERAL -> {
                val rRange = if (p.side == XqSide.RED) 7..9 else 0..2
                for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in rRange && nc in 3..5) addIfOk(nr, nc)
                }
            }
            XqType.ADVISOR -> {
                val rRange = if (p.side == XqSide.RED) 7..9 else 0..2
                for ((dr, dc) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in rRange && nc in 3..5) addIfOk(nr, nc)
                }
            }
            XqType.ELEPHANT -> {
                val rRange = if (p.side == XqSide.RED) 5..9 else 0..4
                for ((dr, dc) in listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in rRange && nc in 0..8 && b[r + dr / 2][c + dc / 2] == null) addIfOk(nr, nc)
                }
            }
            XqType.HORSE -> {
                val steps = listOf(
                    Triple(-2, -1, -1 to 0), Triple(-2, 1, -1 to 0),
                    Triple(2, -1, 1 to 0), Triple(2, 1, 1 to 0),
                    Triple(-1, -2, 0 to -1), Triple(1, -2, 0 to -1),
                    Triple(-1, 2, 0 to 1), Triple(1, 2, 0 to 1)
                )
                for ((dr, dc, leg) in steps) {
                    val lr = r + leg.first; val lc = c + leg.second
                    if (lr in 0..9 && lc in 0..8 && b[lr][lc] == null) addIfOk(r + dr, c + dc)
                }
            }
            XqType.CHARIOT -> {
                for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                    var nr = r + dr; var nc = c + dc
                    while (nr in 0..9 && nc in 0..8) {
                        if (b[nr][nc] == null) out.add(nr to nc)
                        else {
                            if (b[nr][nc]?.side != p.side) out.add(nr to nc)
                            break
                        }
                        nr += dr; nc += dc
                    }
                }
            }
            XqType.CANNON -> {
                for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                    var nr = r + dr; var nc = c + dc; var jumped = false
                    while (nr in 0..9 && nc in 0..8) {
                        val t = b[nr][nc]
                        if (!jumped) {
                            if (t == null) out.add(nr to nc) else jumped = true
                        } else if (t != null) {
                            if (t.side != p.side) out.add(nr to nc)
                            break
                        }
                        nr += dr; nc += dc
                    }
                }
            }
            XqType.SOLDIER -> {
                val fwd = if (p.side == XqSide.RED) -1 else 1
                addIfOk(r + fwd, c)
                val crossedRiver = if (p.side == XqSide.RED) r <= 4 else r >= 5
                if (crossedRiver) { addIfOk(r, c - 1); addIfOk(r, c + 1) }
            }
        }
        return out
    }

    fun isGeneralChecked(b: List<List<XqPiece?>>, side: XqSide): Boolean {
        var genPos: Pair<Int, Int>? = null
        for (r in 0..9) for (c in 0..8) {
            val p = b[r][c]
            if (p != null && p.side == side && p.type == XqType.GENERAL) {
                genPos = r to c
                break
            }
        }
        val gp = genPos ?: return false
        for (r in 0..9) for (c in 0..8) {
            val p = b[r][c]
            if (p != null && p.side != side && gp in legalMovesFrom(b, r, c)) return true
        }
        return false
    }

    fun applyMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val next = board.map { it.toMutableList() }
        val moving = next[from.first][from.second] ?: return
        val captured = next[to.first][to.second]
        next[from.first][from.second] = null
        next[to.first][to.second] = moving
        undoStack.add(board to turn)
        val immutable = next.map { it.toList() }
        board = immutable
        selected = null
        if (captured?.type == XqType.GENERAL) {
            onPlayCue(SoundCue.CAPTURE)
            winner = moving.side
            onPlayCue(SoundCue.WIN)
        } else {
            val oppSide = if (turn == XqSide.RED) XqSide.BLACK else XqSide.RED
            val givesCheck = isGeneralChecked(immutable, oppSide)
            onPlayCue(
                when {
                    givesCheck -> SoundCue.CHECK_ALERT
                    captured != null -> SoundCue.CAPTURE
                    else -> SoundCue.WOOD_TAP
                }
            )
            turn = oppSide
        }
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && turn == XqSide.BLACK && winner == null
    LaunchedEffect(board, turn, isAiTurn) {
        if (isAiTurn) {
            delay(360L)
            val all = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
            for (r in 0..9) for (c in 0..8) {
                if (board[r][c]?.side == XqSide.BLACK) {
                    legalMovesFrom(board, r, c).forEach { dst -> all.add((r to c) to dst) }
                }
            }
            val pick = all.maxByOrNull { (_, dst) ->
                (board[dst.first][dst.second]?.type?.value ?: 0) + (0..12).random()
            }
            pick?.let { applyMove(it.first, it.second) }
        }
    }

    LaunchedEffect(winner) {
        val w = winner
        if (w != null && !recorded) {
            recorded = true
            val seat = if (w == XqSide.RED) 0 else 1
            val wp = sessionConfig.players[seat]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} captured the General", 100, 120)
        }
    }

    fun restart() {
        board = initBoard(); turn = XqSide.RED; undoStack.clear(); selected = null; winner = null; recorded = false
    }

    val legalDests = remember(selected, board) { selected?.let { legalMovesFrom(board, it.first, it.second).toSet() } ?: emptySet() }

    GameScaffold(
        game = game,
        subtitle = "9×10 River & Palace",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty() && !isAiTurn,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty() && !isAiTurn) {
                val p = undoStack.removeAt(undoStack.lastIndex)
                board = p.first; turn = p.second; selected = null; winner = null
                onPlayCue(SoundCue.KEY_TICK)
            }
        },
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(players = sessionConfig.players, activeIndex = if (turn == XqSide.RED) 0 else 1)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * 0.9f)
            val h = w * (10f / 9f)
            Surface(
                modifier = Modifier.size(width = w, height = h),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFDDC59B),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wPx = constraints.maxWidth.toFloat()
                    val hPx = constraints.maxHeight.toFloat()
                    val pxPad = wPx * 0.08f
                    val stepX = (wPx - 2 * pxPad) / 8f
                    val stepY = (hPx - 2 * pxPad) / 9f
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ink = Color(0xFF4A3728)
                        for (r in 0..9) drawLine(ink, Offset(pxPad, pxPad + r * stepY), Offset(wPx - pxPad, pxPad + r * stepY), 2f)
                        for (c in 0..8) {
                            if (c == 0 || c == 8) {
                                drawLine(ink, Offset(pxPad + c * stepX, pxPad), Offset(pxPad + c * stepX, hPx - pxPad), 2f)
                            } else {
                                drawLine(ink, Offset(pxPad + c * stepX, pxPad), Offset(pxPad + c * stepX, pxPad + 4 * stepY), 2f)
                                drawLine(ink, Offset(pxPad + c * stepX, pxPad + 5 * stepY), Offset(pxPad + c * stepX, hPx - pxPad), 2f)
                            }
                        }
                        // Palace X diagonals
                        drawLine(ink, Offset(pxPad + 3 * stepX, pxPad), Offset(pxPad + 5 * stepX, pxPad + 2 * stepY), 2f)
                        drawLine(ink, Offset(pxPad + 5 * stepX, pxPad), Offset(pxPad + 3 * stepX, pxPad + 2 * stepY), 2f)
                        drawLine(ink, Offset(pxPad + 3 * stepX, pxPad + 7 * stepY), Offset(pxPad + 5 * stepX, pxPad + 9 * stepY), 2f)
                        drawLine(ink, Offset(pxPad + 5 * stepX, pxPad + 7 * stepY), Offset(pxPad + 3 * stepX, pxPad + 9 * stepY), 2f)
                    }

                    val cellDp = (w / 9.2f)
                    val checkedRed = remember(board) { isGeneralChecked(board, XqSide.RED) }
                    val checkedBlack = remember(board) { isGeneralChecked(board, XqSide.BLACK) }
                    for (r in 0..9) for (c in 0..8) {
                        val p = board[r][c]
                        val isDest = (r to c) in legalDests
                        val isSel = selected == (r to c)
                        val isCheckedGen = p?.type == XqType.GENERAL &&
                            ((p.side == XqSide.RED && checkedRed) || (p.side == XqSide.BLACK && checkedBlack))
                        val cx = pxPad + c * stepX
                        val cy = pxPad + r * stepY
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((cx - cellDp.toPx() / 2f).roundToInt(), (cy - cellDp.toPx() / 2f).roundToInt()) }
                                .size(cellDp)
                                .bouncyPress(enabled = !isAiTurn && winner == null && (isDest || p?.side == turn))
                                .clickable(enabled = !isAiTurn && winner == null) {
                                    if (isDest && selected != null) applyMove(selected!!, r to c)
                                    else if (p?.side == turn) {
                                        selected = r to c
                                        onPlayCue(SoundCue.KEY_TICK)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (p != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.88f)
                                        .springPop(p, popScale = 1.20f)
                                        .pulseEffect(isSel || isCheckedGen, maxScale = 1.14f)
                                        .clip(CircleShape)
                                        .background(if (isCheckedGen) Color(0xFFFFE0E0) else Color(0xFFFAF0DC))
                                        .border(
                                            width = if (isSel || isCheckedGen) 2.5.dp else 1.5.dp,
                                            color = when {
                                                isCheckedGen -> Color(0xFFD32F2F)
                                                isSel -> Color.Black
                                                p.side == XqSide.RED -> Color(0xFFB71C1C)
                                                else -> Color(0xFF1A1A1A)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (p.side == XqSide.RED) p.type.redLabel else p.type.blackLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.side == XqSide.RED) Color(0xFFB71C1C) else Color(0xFF1A1A1A)
                                    )
                                }
                            } else if (isDest) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .pulseEffect(!isAiTurn && winner == null, maxScale = 1.25f)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.75f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (winner != null) {
        val wp = sessionConfig.players[if (winner == XqSide.RED) 0 else 1]
        GameCompletionDialog(title = "${wp.name} Wins Xiangqi!", subtitle = "Enemy General captured.", onPlayAgain = ::restart, onExit = onBack)
    }
}
