package com.example.ui.games.strategy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private enum class ShogiSide { SENTE, GOTE; fun opp() = if (this == SENTE) GOTE else SENTE }
private enum class ShogiType(val kanji: String, val promKanji: String, val value: Int) {
    KING("玉", "玉", 10000),
    ROOK("飛", "龍", 900),
    BISHOP("角", "馬", 800),
    GOLD("金", "金", 500),
    SILVER("銀", "全", 450),
    KNIGHT("桂", "圭", 300),
    LANCE("香", "杏", 280),
    PAWN("歩", "と", 100)
}
private data class ShogiPiece(val side: ShogiSide, val type: ShogiType, val promoted: Boolean = false)

@Composable
fun ShogiScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("shogi") }

    fun initBoard(): List<List<ShogiPiece?>> {
        val g = MutableList(9) { MutableList<ShogiPiece?>(9) { null } }
        val back = listOf(ShogiType.LANCE, ShogiType.KNIGHT, ShogiType.SILVER, ShogiType.GOLD, ShogiType.KING, ShogiType.GOLD, ShogiType.SILVER, ShogiType.KNIGHT, ShogiType.LANCE)
        for (c in 0..8) {
            g[0][c] = ShogiPiece(ShogiSide.GOTE, back[c])
            g[8][c] = ShogiPiece(ShogiSide.SENTE, back[c])
            g[2][c] = ShogiPiece(ShogiSide.GOTE, ShogiType.PAWN)
            g[6][c] = ShogiPiece(ShogiSide.SENTE, ShogiType.PAWN)
        }
        g[1][1] = ShogiPiece(ShogiSide.GOTE, ShogiType.ROOK); g[1][7] = ShogiPiece(ShogiSide.GOTE, ShogiType.BISHOP)
        g[7][7] = ShogiPiece(ShogiSide.SENTE, ShogiType.ROOK); g[7][1] = ShogiPiece(ShogiSide.SENTE, ShogiType.BISHOP)
        return g.map { it.toList() }
    }

    var board by remember { mutableStateOf(initBoard()) }
    var turn by remember { mutableStateOf(ShogiSide.SENTE) }
    var senteHand by remember { mutableStateOf<List<ShogiType>>(emptyList()) }
    var goteHand by remember { mutableStateOf<List<ShogiType>>(emptyList()) }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedDrop by remember { mutableStateOf<ShogiType?>(null) }
    var winner by remember { mutableStateOf<ShogiSide?>(null) }
    var recorded by remember { mutableStateOf(false) }

    fun legalMovesFrom(b: List<List<ShogiPiece?>>, r: Int, c: Int): List<Pair<Int, Int>> {
        val p = b[r][c] ?: return emptyList()
        val fwd = if (p.side == ShogiSide.SENTE) -1 else 1
        val out = mutableListOf<Pair<Int, Int>>()
        fun addStep(dr: Int, dc: Int) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0..8 && nc in 0..8 && b[nr][nc]?.side != p.side) out.add(nr to nc)
        }
        fun addGold() {
            listOf(fwd to -1, fwd to 0, fwd to 1, 0 to -1, 0 to 1, -fwd to 0).forEach { addStep(it.first, it.second) }
        }
        fun slide(dirs: List<Pair<Int, Int>>) {
            for ((dr, dc) in dirs) {
                var nr = r + dr; var nc = c + dc
                while (nr in 0..8 && nc in 0..8) {
                    val t = b[nr][nc]
                    if (t == null) out.add(nr to nc)
                    else {
                        if (t.side != p.side) out.add(nr to nc)
                        break
                    }
                    nr += dr; nc += dc
                }
            }
        }
        when {
            p.type == ShogiType.KING -> for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) addStep(dr, dc)
            p.type == ShogiType.GOLD || (p.promoted && p.type in listOf(ShogiType.SILVER, ShogiType.KNIGHT, ShogiType.LANCE, ShogiType.PAWN)) -> addGold()
            p.type == ShogiType.PAWN -> addStep(fwd, 0)
            p.type == ShogiType.SILVER -> listOf(fwd to -1, fwd to 0, fwd to 1, -fwd to -1, -fwd to 1).forEach { addStep(it.first, it.second) }
            p.type == ShogiType.KNIGHT -> { addStep(2 * fwd, -1); addStep(2 * fwd, 1) }
            p.type == ShogiType.LANCE -> slide(listOf(fwd to 0))
            p.type == ShogiType.ROOK -> {
                slide(listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
                if (p.promoted) listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1).forEach { addStep(it.first, it.second) }
            }
            p.type == ShogiType.BISHOP -> {
                slide(listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
                if (p.promoted) listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { addStep(it.first, it.second) }
            }
        }
        return out
    }

    fun applyMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val next = board.map { it.toMutableList() }
        val moving = next[from.first][from.second] ?: return
        val cap = next[to.first][to.second]
        val inPromoZone = if (moving.side == ShogiSide.SENTE) (to.first <= 2 || from.first <= 2) else (to.first >= 6 || from.first >= 6)
        val canPromo = !moving.promoted && moving.type != ShogiType.KING && moving.type != ShogiType.GOLD && inPromoZone
        next[from.first][from.second] = null
        next[to.first][to.second] = moving.copy(promoted = moving.promoted || canPromo)
        board = next.map { it.toList() }
        if (cap != null && cap.type != ShogiType.KING) {
            if (moving.side == ShogiSide.SENTE) senteHand = senteHand + cap.type else goteHand = goteHand + cap.type
        }
        selectedSquare = null
        onPlayCue(if (cap != null) SoundCue.CAPTURE else SoundCue.MOVE)
        if (cap?.type == ShogiType.KING) {
            winner = moving.side
            onPlayCue(SoundCue.WIN)
        } else {
            turn = turn.opp()
        }
    }

    fun dropPiece(type: ShogiType, r: Int, c: Int) {
        if (board[r][c] != null) return
        val next = board.map { it.toMutableList() }
        next[r][c] = ShogiPiece(turn, type, false)
        board = next.map { it.toList() }
        if (turn == ShogiSide.SENTE) senteHand = senteHand - type else goteHand = goteHand - type
        selectedDrop = null
        onPlayCue(SoundCue.MOVE)
        turn = turn.opp()
    }

    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && turn == ShogiSide.GOTE && winner == null
    LaunchedEffect(board, turn, isAiTurn) {
        if (isAiTurn) {
            delay(360L)
            val all = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
            for (r in 0..8) for (c in 0..8) {
                if (board[r][c]?.side == ShogiSide.GOTE) {
                    legalMovesFrom(board, r, c).forEach { dst -> all.add((r to c) to dst) }
                }
            }
            val pick = all.maxByOrNull { (_, dst) -> (board[dst.first][dst.second]?.type?.value ?: 0) + (0..10).random() }
            if (pick != null) applyMove(pick.first, pick.second)
        }
    }

    LaunchedEffect(winner) {
        val w = winner
        if (w != null && !recorded) {
            recorded = true
            val wp = sessionConfig.players[if (w == ShogiSide.SENTE) 0 else 1]
            onRecordOutcome(if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN, wp.name, "${wp.name} captured the King", 100, 120)
        }
    }

    fun restart() {
        board = initBoard(); turn = ShogiSide.SENTE; senteHand = emptyList(); goteHand = emptyList()
        selectedSquare = null; selectedDrop = null; winner = null; recorded = false
    }

    val legalDests = remember(selectedSquare, board) { selectedSquare?.let { legalMovesFrom(board, it.first, it.second).toSet() } ?: emptySet() }

    GameScaffold(
        game = game,
        subtitle = "9×9 Shogi • Drops Enabled",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(players = sessionConfig.players, activeIndex = if (turn == ShogiSide.SENTE) 0 else 1)
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE5C896),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    for (r in 0..8) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..8) {
                                val p = board[r][c]
                                val isSel = selectedSquare == (r to c)
                                val isDest = (r to c) in legalDests || (selectedDrop != null && p == null)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(if (isSel) Color(0xFFC8A951) else if (isDest) Color(0xFFA5D6A7).copy(alpha = 0.5f) else Color.Transparent)
                                        .border(0.6.dp, Color(0xFF5D4037))
                                        .clickable(enabled = !isAiTurn && winner == null) {
                                            if (selectedDrop != null && p == null) {
                                                dropPiece(selectedDrop!!, r, c)
                                            } else if ((r to c) in legalDests && selectedSquare != null) {
                                                applyMove(selectedSquare!!, r to c)
                                            } else if (p?.side == turn) {
                                                selectedDrop = null
                                                selectedSquare = r to c
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (p != null) {
                                        Text(
                                            text = if (p.promoted) p.type.promKanji else p.type.kanji,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (p.promoted) Color(0xFFB71C1C) else Color(0xFF1A1A1A),
                                            modifier = Modifier.rotate(if (p.side == ShogiSide.GOTE) 180f else 0f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        val activeHand = if (turn == ShogiSide.SENTE) senteHand else goteHand
        MonoCard(elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Drops:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (activeHand.isEmpty()) {
                    Text("None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    activeHand.distinct().forEach { t ->
                        Surface(
                            modifier = Modifier.clickable(enabled = !isAiTurn) {
                                selectedSquare = null
                                selectedDrop = if (selectedDrop == t) null else t
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDrop == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${t.kanji}×${activeHand.count { it == t }}",
                                color = if (selectedDrop == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (winner != null) {
        val wp = sessionConfig.players[if (winner == ShogiSide.SENTE) 0 else 1]
        GameCompletionDialog(title = "${wp.name} Wins Shogi!", subtitle = "Enemy King captured.", onPlayAgain = ::restart, onExit = onBack)
    }
}
