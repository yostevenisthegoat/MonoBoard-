package com.example.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.components.bouncyPress
import com.example.ui.components.pulseEffect
import com.example.ui.components.shakeOnTrigger
import com.example.ui.components.springPop
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

private data class MineCell(
    val isMine: Boolean = false,
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("minesweeper") }
    val (gridSize, mineTotal) = remember(sessionConfig.ruleVariant) {
        when {
            sessionConfig.ruleVariant.contains("12") -> 12 to 22
            sessionConfig.ruleVariant.contains("14") -> 14 to 32
            else -> 9 to 10
        }
    }

    var board by remember(gridSize) { mutableStateOf(List(gridSize) { List(gridSize) { MineCell() } }) }
    var initialized by remember(gridSize) { mutableStateOf(false) }
    var flagMode by remember { mutableStateOf(false) }
    var exploded by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    var explosionShakeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialized, exploded, won) {
        while (initialized && !exploded && !won) {
            delay(1000L)
            elapsed++
        }
    }

    fun generateBoard(safeR: Int, safeC: Int): List<List<MineCell>> {
        val next = MutableList(gridSize) { MutableList(gridSize) { MineCell() } }
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (kotlin.math.abs(r - safeR) > 1 || kotlin.math.abs(c - safeC) > 1) {
                    candidates.add(r to c)
                }
            }
        }
        candidates.shuffled().take(mineTotal).forEach { (r, c) ->
            next[r][c] = next[r][c].copy(isMine = true)
        }
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!next[r][c].isMine) {
                    var count = 0
                    for (dr in -1..1) for (dc in -1..1) {
                        val nr = r + dr; val nc = c + dc
                        if (nr in 0 until gridSize && nc in 0 until gridSize && next[nr][nc].isMine) count++
                    }
                    next[r][c] = next[r][c].copy(adjacent = count)
                }
            }
        }
        return next.map { it.toList() }
    }

    fun floodReveal(src: List<List<MineCell>>, startR: Int, startC: Int): List<List<MineCell>> {
        val next = src.map { it.toMutableList() }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(startR to startC)
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            val cell = next[r][c]
            if (cell.revealed || cell.flagged) continue
            next[r][c] = cell.copy(revealed = true)
            if (cell.adjacent == 0 && !cell.isMine) {
                for (dr in -1..1) for (dc in -1..1) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until gridSize && nc in 0 until gridSize && !next[nr][nc].revealed) {
                        queue.add(nr to nc)
                    }
                }
            }
        }
        return next.map { it.toList() }
    }

    fun checkVictory(b: List<List<MineCell>>) {
        val allSafeRevealed = b.all { row -> row.all { it.isMine || it.revealed } }
        if (allSafeRevealed) {
            won = true
            onPlayCue(SoundCue.WIN)
        }
    }

    fun tapCell(r: Int, c: Int, forceFlag: Boolean = false) {
        if (exploded || won) return
        if (flagMode || forceFlag) {
            if (board[r][c].revealed) return
            val next = board.map { it.toMutableList() }
            next[r][c] = next[r][c].copy(flagged = !next[r][c].flagged)
            board = next.map { it.toList() }
            onPlayCue(SoundCue.KEY_TICK)
            return
        }

        var working = board
        if (!initialized) {
            working = generateBoard(r, c)
            initialized = true
        }
        val target = working[r][c]
        if (target.flagged) return

        // Chord reveal when tapping already-revealed number with matching flags
        if (target.revealed && target.adjacent > 0) {
            var flags = 0
            for (dr in -1..1) for (dc in -1..1) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until gridSize && nc in 0 until gridSize && working[nr][nc].flagged) flags++
            }
            if (flags == target.adjacent) {
                var hitMine = false
                var cur = working
                val beforeRevealed = cur.sumOf { row -> row.count { it.revealed } }
                for (dr in -1..1) for (dc in -1..1) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until gridSize && nc in 0 until gridSize && !cur[nr][nc].flagged && !cur[nr][nc].revealed) {
                        if (cur[nr][nc].isMine) hitMine = true
                        else cur = floodReveal(cur, nr, nc)
                    }
                }
                if (hitMine) {
                    exploded = true
                    explosionShakeCount++
                    onPlayCue(SoundCue.EXPLOSION)
                    board = cur.map { row -> row.map { if (it.isMine) it.copy(revealed = true) else it } }
                } else {
                    board = cur
                    val afterRevealed = cur.sumOf { row -> row.count { it.revealed } }
                    onPlayCue(if (afterRevealed - beforeRevealed > 3) SoundCue.TILE_MERGE else SoundCue.WOOD_TAP)
                    checkVictory(board)
                }
            }
            return
        }

        if (target.isMine) {
            exploded = true
            explosionShakeCount++
            onPlayCue(SoundCue.EXPLOSION)
            board = working.map { row -> row.map { if (it.isMine) it.copy(revealed = true) else it } }
            return
        }

        val beforeRevealed = working.sumOf { row -> row.count { it.revealed } }
        val updated = floodReveal(working, r, c)
        val afterRevealed = updated.sumOf { row -> row.count { it.revealed } }
        board = updated
        onPlayCue(if (afterRevealed - beforeRevealed > 3) SoundCue.TILE_MERGE else SoundCue.WOOD_TAP)
        checkVictory(board)
    }

    LaunchedEffect(won, exploded) {
        if ((won || exploded) && !recorded) {
            recorded = true
            if (won) {
                val score = (1000 - elapsed * 2).coerceAtLeast(100)
                onRecordOutcome(MatchOutcome.WIN, "Player 1", "Cleared $gridSize×$gridSize in ${elapsed}s", score, elapsed)
            } else {
                onRecordOutcome(MatchOutcome.LOSS, "Mines", "Detonated a mine at ${elapsed}s", 0, elapsed)
            }
        }
    }

    fun restart() {
        board = List(gridSize) { List(gridSize) { MineCell() } }
        initialized = false
        exploded = false
        won = false
        elapsed = 0
        recorded = false
    }

    val flaggedCount = board.sumOf { r -> r.count { it.flagged } }

    GameScaffold(
        game = game,
        subtitle = sessionConfig.ruleVariant,
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        MonoCard(elevated = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCounterText(
                    text = "MINES: ${mineTotal - flaggedCount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                )
                OutlinedButton(
                    onClick = {
                        flagMode = !flagMode
                        onPlayCue(SoundCue.KEY_TICK)
                    },
                    modifier = Modifier
                        .bouncyPress()
                        .pulseEffect(flagMode, maxScale = 1.04f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(if (flagMode) 1.5.dp else 1.dp, if (flagMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (flagMode) "Flag Mode" else "Reveal Mode")
                }
                Text(
                    text = "%02d:%02d".format(elapsed / 60, elapsed % 60),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val size = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier.size(size).shakeOnTrigger(explosionShakeCount),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    for (r in 0 until gridSize) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0 until gridSize) {
                                val cell = board[r][c]
                                val bg = when {
                                    cell.revealed && cell.isMine -> Color(0xFFB71C1C)
                                    cell.revealed -> MaterialTheme.colorScheme.background
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .springPop(cell.revealed to cell.flagged, popScale = 1.15f)
                                        .bouncyPress(!cell.revealed && !exploded && !won)
                                        .background(bg, RoundedCornerShape(4.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                        .combinedClickable(
                                            onClick = { tapCell(r, c, forceFlag = false) },
                                            onLongClick = { tapCell(r, c, forceFlag = true) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        cell.revealed && cell.isMine -> Icon(
                                            Icons.Rounded.Dangerous,
                                            contentDescription = "Mine",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        cell.revealed && cell.adjacent > 0 -> Text(
                                            text = cell.adjacent.toString(),
                                            fontSize = if (gridSize <= 9) 14.sp else 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        cell.flagged -> Icon(
                                            Icons.Rounded.Flag,
                                            contentDescription = "Flagged",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (won || exploded) {
        GameCompletionDialog(
            title = if (won) "Field Cleared!" else "Mine Detonated",
            subtitle = if (won) "Completed in %02d:%02d.".format(elapsed / 60, elapsed % 60) else "Better luck on the next sweep.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
