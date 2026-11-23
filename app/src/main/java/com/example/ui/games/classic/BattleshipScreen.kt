package com.example.ui.games.classic

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
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.PrivacyCurtainOverlay
import kotlinx.coroutines.delay

private enum class BattlePhase {
    PLACE_P1, PLACE_P2, BATTLE
}

private enum class ShotState { NONE, MISS, HIT }

@Composable
fun BattleshipScreen(
    sessionConfig: GameSessionConfig,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("battleship") }
    val shipSizes = remember { listOf(5, 4, 3, 3, 2) }
    val size = 8

    fun randomFleet(): List<List<Int>> {
        val board = MutableList(size) { MutableList(size) { -1 } }
        shipSizes.forEachIndexed { shipId, len ->
            var placed = false
            var tries = 0
            while (!placed && tries < 200) {
                tries++
                val horiz = Math.random() < 0.5
                val r = (0 until size).random()
                val c = (0 until size).random()
                val cells = (0 until len).map { k -> if (horiz) r to (c + k) else (r + k) to c }
                if (cells.all { (nr, nc) -> nr in 0 until size && nc in 0 until size && board[nr][nc] == -1 }) {
                    cells.forEach { (nr, nc) -> board[nr][nc] = shipId }
                    placed = true
                }
            }
        }
        return board.map { it.toList() }
    }

    var p1Fleet by remember { mutableStateOf(List(size) { List(size) { -1 } }) }
    var p2Fleet by remember {
        mutableStateOf(if (sessionConfig.playMode == PlayMode.VS_AI) randomFleet() else List(size) { List(size) { -1 } })
    }
    var p1Shots by remember { mutableStateOf(List(size) { List(size) { ShotState.NONE } }) }
    var p2Shots by remember { mutableStateOf(List(size) { List(size) { ShotState.NONE } }) }

    var phase by remember { mutableStateOf(BattlePhase.PLACE_P1) }
    var placingShipIdx by remember { mutableIntStateOf(0) }
    var horizontal by remember { mutableStateOf(true) }
    var activeSeat by remember { mutableIntStateOf(0) }
    var showPrivacyCurtain by remember { mutableStateOf(false) }
    var lastStatus by remember { mutableStateOf("Place Carrier (5 cells)") }
    var turnShotFired by remember { mutableStateOf(false) }
    var winnerSeat by remember { mutableStateOf<Int?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }

    fun countHits(shots: List<List<ShotState>>): Int = shots.sumOf { row -> row.count { it == ShotState.HIT } }
    val totalShipCells = shipSizes.sum()

    LaunchedEffect(winnerSeat) {
        while (winnerSeat == null) {
            delay(1000L)
            elapsed++
        }
    }

    LaunchedEffect(winnerSeat) {
        val w = winnerSeat
        if (w != null && !recorded) {
            recorded = true
            onPlayCue(SoundCue.WIN)
            val wp = sessionConfig.players[w]
            val outcome = if (wp.isAi) MatchOutcome.LOSS else MatchOutcome.WIN
            onRecordOutcome(outcome, wp.name, "${wp.name} sank the enemy fleet!", 100, elapsed)
        }
    }

    // AI turn in Battle
    LaunchedEffect(phase, activeSeat, turnShotFired, winnerSeat) {
        if (phase == BattlePhase.BATTLE && sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1 && winnerSeat == null) {
            delay(500L)
            val targets = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until size) for (c in 0 until size) if (p2Shots[r][c] == ShotState.NONE) targets.add(r to c)
            if (targets.isNotEmpty()) {
                // Smart hunt around existing hits
                val hitNeighbors = mutableListOf<Pair<Int, Int>>()
                for (r in 0 until size) for (c in 0 until size) {
                    if (p2Shots[r][c] == ShotState.HIT) {
                        listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dr, dc) ->
                            val nr = r + dr; val nc = c + dc
                            if (nr in 0 until size && nc in 0 until size && p2Shots[nr][nc] == ShotState.NONE) {
                                hitNeighbors.add(nr to nc)
                            }
                        }
                    }
                }
                val (tr, tc) = if (hitNeighbors.isNotEmpty()) hitNeighbors.random() else targets.random()
                val hit = p1Fleet[tr][tc] != -1
                val nextShots = p2Shots.map { it.toMutableList() }
                nextShots[tr][tc] = if (hit) ShotState.HIT else ShotState.MISS
                p2Shots = nextShots.map { it.toList() }
                onPlayCue(if (hit) SoundCue.CAPTURE else SoundCue.MOVE)
                lastStatus = if (hit) "Mono AI scored a HIT at ${'A' + tc}${tr + 1}!" else "Mono AI missed at ${'A' + tc}${tr + 1}."
                if (countHits(p2Shots) >= totalShipCells) {
                    winnerSeat = 1
                } else {
                    activeSeat = 0
                }
            }
        }
    }

    fun placeShipAt(r: Int, c: Int) {
        if (placingShipIdx >= shipSizes.size) return
        val len = shipSizes[placingShipIdx]
        val cells = (0 until len).map { k -> if (horizontal) r to (c + k) else (r + k) to c }
        val fleet = if (phase == BattlePhase.PLACE_P1) p1Fleet else p2Fleet
        if (cells.any { (nr, nc) -> nr !in 0 until size || nc !in 0 until size || fleet[nr][nc] != -1 }) {
            onPlayCue(SoundCue.ERROR)
            return
        }
        val next = fleet.map { it.toMutableList() }
        cells.forEach { (nr, nc) -> next[nr][nc] = placingShipIdx }
        if (phase == BattlePhase.PLACE_P1) p1Fleet = next.map { it.toList() } else p2Fleet = next.map { it.toList() }
        onPlayCue(SoundCue.MOVE)
        placingShipIdx++
        if (placingShipIdx >= shipSizes.size) {
            if (phase == BattlePhase.PLACE_P1) {
                if (sessionConfig.playMode == PlayMode.VS_AI) {
                    phase = BattlePhase.BATTLE
                    activeSeat = 0
                    lastStatus = "Tap Enemy Radar to fire!"
                } else {
                    phase = BattlePhase.PLACE_P2
                    placingShipIdx = 0
                    showPrivacyCurtain = true
                    lastStatus = "Place ${sessionConfig.players[1].name}'s fleet"
                }
            } else {
                phase = BattlePhase.BATTLE
                activeSeat = 0
                showPrivacyCurtain = sessionConfig.privacyMode
                lastStatus = "Tap Enemy Radar to fire!"
            }
        } else {
            lastStatus = "Place ship (${shipSizes[placingShipIdx]} cells)"
        }
    }

    fun fireAt(r: Int, c: Int) {
        if (phase != BattlePhase.BATTLE || turnShotFired || winnerSeat != null) return
        val myShots = if (activeSeat == 0) p1Shots else p2Shots
        if (myShots[r][c] != ShotState.NONE) return
        val targetFleet = if (activeSeat == 0) p2Fleet else p1Fleet
        val hit = targetFleet[r][c] != -1
        val next = myShots.map { it.toMutableList() }
        next[r][c] = if (hit) ShotState.HIT else ShotState.MISS
        if (activeSeat == 0) p1Shots = next.map { it.toList() } else p2Shots = next.map { it.toList() }
        onPlayCue(if (hit) SoundCue.CAPTURE else SoundCue.MOVE)
        lastStatus = if (hit) "DIRECT HIT at ${'A' + c}${r + 1}!" else "Splash! Miss at ${'A' + c}${r + 1}."

        val updatedShots = if (activeSeat == 0) p1Shots else p2Shots
        if (countHits(updatedShots) >= totalShipCells) {
            winnerSeat = activeSeat
        } else if (sessionConfig.playMode == PlayMode.PASS_AND_PLAY) {
            turnShotFired = true
        } else {
            activeSeat = 1
        }
    }

    fun restart() {
        p1Fleet = List(size) { List(size) { -1 } }
        p2Fleet = if (sessionConfig.playMode == PlayMode.VS_AI) randomFleet() else List(size) { List(size) { -1 } }
        p1Shots = List(size) { List(size) { ShotState.NONE } }
        p2Shots = List(size) { List(size) { ShotState.NONE } }
        phase = BattlePhase.PLACE_P1
        placingShipIdx = 0
        activeSeat = 0
        showPrivacyCurtain = false
        turnShotFired = false
        winnerSeat = null
        elapsed = 0
        recorded = false
        lastStatus = "Place Carrier (5 cells)"
    }

    GameScaffold(
        game = game,
        subtitle = if (phase == BattlePhase.BATTLE) "Naval Combat" else "Fleet Deployment",
        isFavorite = isFavorite,
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onRestart = ::restart
    ) {
        if (showPrivacyCurtain && phase != BattlePhase.PLACE_P1) {
            val nextIdx = if (phase == BattlePhase.PLACE_P2) 1 else activeSeat
            PrivacyCurtainOverlay(
                nextPlayer = sessionConfig.players[nextIdx],
                reason = "Your fleet layout is hidden until you confirm.",
                onRevealClick = { showPrivacyCurtain = false }
            )
        } else {
            PassAndPlayTurnHeader(
                players = sessionConfig.players,
                activeIndex = if (phase == BattlePhase.PLACE_P2) 1 else activeSeat,
                scores = listOf(countHits(p1Shots), countHits(p2Shots)),
                statusMessage = lastStatus
            )
            Spacer(Modifier.height(10.dp))

            if (phase != BattlePhase.BATTLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { horizontal = !horizontal },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (horizontal) "Horizontal" else "Vertical")
                    }
                    OutlinedButton(
                        onClick = {
                            val rf = randomFleet()
                            if (phase == BattlePhase.PLACE_P1) {
                                p1Fleet = rf
                                if (sessionConfig.playMode == PlayMode.VS_AI) {
                                    phase = BattlePhase.BATTLE
                                    activeSeat = 0
                                    lastStatus = "Tap Enemy Radar to fire!"
                                } else {
                                    phase = BattlePhase.PLACE_P2
                                    placingShipIdx = 0
                                    showPrivacyCurtain = true
                                    lastStatus = "Place ${sessionConfig.players[1].name}'s fleet"
                                }
                            } else {
                                p2Fleet = rf
                                phase = BattlePhase.BATTLE
                                activeSeat = 0
                                showPrivacyCurtain = sessionConfig.privacyMode
                                lastStatus = "Tap Enemy Radar to fire!"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto-Place")
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            val displayFleet = when {
                phase == BattlePhase.PLACE_P1 -> p1Fleet
                phase == BattlePhase.PLACE_P2 -> p2Fleet
                activeSeat == 0 -> p1Fleet
                else -> p2Fleet
            }
            val displayShots = if (activeSeat == 0) p1Shots else p2Shots

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val gridW = minOf(maxWidth, maxHeight, 360.dp)
                Surface(
                    modifier = Modifier.size(gridW),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        for (r in 0 until size) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0 until size) {
                                    val isPlacement = phase != BattlePhase.BATTLE
                                    val hasShip = displayFleet[r][c] != -1
                                    val shot = displayShots[r][c]
                                    val bg = if (isPlacement) {
                                        if (hasShip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        when (shot) {
                                            ShotState.HIT -> Color(0xFFC62828)
                                            ShotState.MISS -> MaterialTheme.colorScheme.background
                                            ShotState.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(1.5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bg)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                            .clickable(enabled = (isPlacement || (!turnShotFired && shot == ShotState.NONE && !(sessionConfig.playMode == PlayMode.VS_AI && activeSeat == 1)))) {
                                                if (isPlacement) placeShipAt(r, c) else fireAt(r, c)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isPlacement && shot != ShotState.NONE) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (shot == ShotState.HIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (phase == BattlePhase.BATTLE && turnShotFired && winnerSeat == null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        turnShotFired = false
                        activeSeat = 1 - activeSeat
                        if (sessionConfig.privacyMode) showPrivacyCurtain = true
                        lastStatus = "${sessionConfig.players[activeSeat].name}'s turn to fire!"
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Next Player (${sessionConfig.players[1 - activeSeat].name})", fontWeight = FontWeight.Bold)
                }
            } else if (phase == BattlePhase.BATTLE) {
                MonoCard(elevated = true) {
                    Text(
                        text = "Enemy Radar • ${totalShipCells - countHits(displayShots)} enemy hull segments remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (winnerSeat != null) {
        GameCompletionDialog(
            title = "${sessionConfig.players[winnerSeat!!].name} Sunk the Fleet!",
            subtitle = "All $totalShipCells enemy ship segments destroyed.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
