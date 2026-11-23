package com.example.ui.games.checkers

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AppSettings
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.PassAndPlayTurnHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CheckersScreen(
    sessionConfig: GameSessionConfig,
    settings: AppSettings,
    initialSerializedState: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSaveState: (String, String) -> Unit,
    onClearSaved: () -> Unit,
    onRecordOutcome: (MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    val game = remember { GameCatalog.byId("checkers") }
    val forced = !sessionConfig.ruleVariant.contains("Casual", ignoreCase = true)

    var state by remember {
        mutableStateOf(
            initialSerializedState?.let { CheckersEngine.deserialize(it) } ?: CheckersEngine.initialState(forced)
        )
    }
    val undoStack = remember { mutableStateListOf<CheckersState>() }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var outcomeRecorded by remember { mutableStateOf(false) }

    val legalMoves = remember(state) { CheckersEngine.legalMoves(state) }
    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && state.turn == CheckerSide.DARK && state.winner == null

    LaunchedEffect(state.winner) {
        while (state.winner == null) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    LaunchedEffect(state, isAiTurn) {
        if (isAiTurn) {
            delay(300L)
            val best = withContext(Dispatchers.Default) {
                CheckersEngine.chooseAiMove(state, sessionConfig.aiDifficulty)
            }
            if (best != null) {
                undoStack.add(state)
                state = CheckersEngine.applyMove(state, best)
                selectedSquare = state.mustContinueFrom
                onPlayCue(if (best.isCapture) SoundCue.CAPTURE else SoundCue.MOVE)
                onSaveState(CheckersEngine.serialize(state), "${state.moveCount} moves")
            }
        }
    }

    LaunchedEffect(state.winner) {
        val w = state.winner
        if (w != null && !outcomeRecorded) {
            outcomeRecorded = true
            onClearSaved()
            onPlayCue(SoundCue.WIN)
            val p1 = sessionConfig.players.getOrNull(0)?.name ?: "Light"
            val p2 = sessionConfig.players.getOrNull(1)?.name ?: "Dark"
            val winnerName = if (w == CheckerSide.LIGHT) p1 else p2
            val outcome = if (sessionConfig.playMode == PlayMode.VS_AI && w == CheckerSide.DARK) MatchOutcome.LOSS else MatchOutcome.WIN
            onRecordOutcome(outcome, winnerName, "$winnerName won in ${state.moveCount} moves", 100, elapsedSeconds)
        }
    }

    fun lightCount(): Int = state.board.sumOf { r -> r.count { it?.side == CheckerSide.LIGHT } }
    fun darkCount(): Int = state.board.sumOf { r -> r.count { it?.side == CheckerSide.DARK } }

    fun restart() {
        undoStack.clear()
        state = CheckersEngine.initialState(forced)
        selectedSquare = null
        elapsedSeconds = 0
        outcomeRecorded = false
        onClearSaved()
    }

    GameScaffold(
        game = game,
        subtitle = "${if (forced) "Forced Capture" else "Casual"} • ${if (sessionConfig.playMode == PlayMode.VS_AI) "vs AI" else "Pass & Play"}",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty() && !isAiTurn,
        onBack = {
            if (state.winner == null && state.moveCount > 0) {
                onSaveState(CheckersEngine.serialize(state), "${state.moveCount} moves")
            }
            onBack()
        },
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty() && !isAiTurn) {
                state = undoStack.removeAt(undoStack.lastIndex)
                selectedSquare = state.mustContinueFrom
                outcomeRecorded = false
            }
        },
        onRestart = ::restart
    ) {
        PassAndPlayTurnHeader(
            players = sessionConfig.players,
            activeIndex = if (state.turn == CheckerSide.LIGHT) 0 else 1,
            scores = listOf(lightCount(), darkCount()),
            statusMessage = if (state.mustContinueFrom != null) "Multi-jump available! Continue jumping." else null
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                color = settings.boardTheme.boardFrame,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    for (r in 0..7) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..7) {
                                val isDarkSq = (r + c) % 2 == 1
                                val piece = state.board[r][c]
                                val isSelected = selectedSquare == (r to c) || state.mustContinueFrom == (r to c)
                                val matchingMove = legalMoves.firstOrNull {
                                    selectedSquare != null &&
                                        it.fromRow == selectedSquare!!.first &&
                                        it.fromCol == selectedSquare!!.second &&
                                        it.toRow == r && it.toCol == c
                                }
                                val canSelectSource = piece != null &&
                                    piece.side == state.turn &&
                                    legalMoves.any { it.fromRow == r && it.fromCol == c }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            when {
                                                isSelected -> settings.boardTheme.highlight
                                                isDarkSq -> settings.boardTheme.darkSquare
                                                else -> settings.boardTheme.lightSquare
                                            }
                                        )
                                        .clickable(enabled = !isAiTurn && state.winner == null && isDarkSq) {
                                            if (matchingMove != null) {
                                                undoStack.add(state)
                                                state = CheckersEngine.applyMove(state, matchingMove)
                                                selectedSquare = state.mustContinueFrom
                                                onPlayCue(if (matchingMove.isCapture) SoundCue.CAPTURE else SoundCue.MOVE)
                                                onSaveState(CheckersEngine.serialize(state), "${state.moveCount} moves")
                                            } else if (canSelectSource) {
                                                selectedSquare = r to c
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (piece != null) {
                                        val pieceColor = if (piece.side == CheckerSide.LIGHT) Color(0xFFF3F3F3) else Color(0xFF1A1A1A)
                                        val rimColor = if (piece.side == CheckerSide.LIGHT) Color(0xFF9E9E9E) else Color(0xFF555555)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.76f)
                                                .clip(CircleShape)
                                                .background(pieceColor)
                                                .border(2.dp, rimColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (piece.isKing) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = "King",
                                                    tint = if (piece.side == CheckerSide.LIGHT) Color(0xFF1A1A1A) else Color(0xFFF3F3F3),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else if (matchingMove != null && settings.showLegalMoves) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.6f))
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

    if (state.winner != null) {
        val p1 = sessionConfig.players.getOrNull(0)?.name ?: "Player 1"
        val p2 = sessionConfig.players.getOrNull(1)?.name ?: "Player 2"
        val wName = if (state.winner == CheckerSide.LIGHT) p1 else p2
        GameCompletionDialog(
            title = "$wName Wins!",
            subtitle = "Checkers match finished in ${state.moveCount} moves.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}
