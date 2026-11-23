package com.example.ui.games.chess

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.AppSettings
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.domain.GameCatalog
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.ui.components.AvatarBadge
import com.example.ui.components.GameCompletionDialog
import com.example.ui.components.GameScaffold
import com.example.ui.components.MonoCard
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChessScreen(
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
    val game = remember { GameCatalog.byId("chess") }
    val scope = rememberCoroutineScope()

    var currentState by remember {
        mutableStateOf(
            initialSerializedState?.let { ChessEngine.deserialize(it) } ?: ChessEngine.initialState()
        )
    }
    val undoStack = remember { mutableStateListOf<ChessState>() }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var legalTargets by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var pendingPromotionMoves by remember { mutableStateOf<List<ChessMove>?>(null) }
    var activeBoardTheme by remember { mutableStateOf(settings.boardTheme) }
    var activePieceTheme by remember { mutableStateOf(settings.pieceTheme) }
    var aiThinking by remember { mutableStateOf(false) }

    val clockMinutes = when {
        sessionConfig.timeLimitMinutes > 0 -> sessionConfig.timeLimitMinutes
        sessionConfig.ruleVariant.contains("3m") -> 3
        sessionConfig.ruleVariant.contains("10m") -> 10
        else -> 0
    }
    var whiteSeconds by remember(clockMinutes) { mutableIntStateOf(clockMinutes * 60) }
    var blackSeconds by remember(clockMinutes) { mutableIntStateOf(clockMinutes * 60) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var flaggedWinner by remember { mutableStateOf<PieceColor?>(null) }
    var outcomeRecorded by remember { mutableStateOf(false) }

    val isTerminal = flaggedWinner != null || (currentState.status != ChessGameStatus.ONGOING && currentState.status != ChessGameStatus.CHECK)

    // Clock ticker
    LaunchedEffect(currentState.turn, isTerminal, clockMinutes) {
        while (!isTerminal) {
            delay(1000L)
            elapsedSeconds++
            if (clockMinutes > 0) {
                if (currentState.turn == PieceColor.WHITE) {
                    if (whiteSeconds > 0) whiteSeconds-- else flaggedWinner = PieceColor.BLACK
                } else {
                    if (blackSeconds > 0) blackSeconds-- else flaggedWinner = PieceColor.WHITE
                }
            }
        }
    }

    // AI Turn Handler
    val isAiTurn = sessionConfig.playMode == PlayMode.VS_AI && currentState.turn == PieceColor.BLACK && !isTerminal
    LaunchedEffect(currentState, isAiTurn) {
        if (isAiTurn) {
            aiThinking = true
            delay(280)
            val best = withContext(Dispatchers.Default) {
                ChessEngine.chooseBestMove(currentState, sessionConfig.aiDifficulty)
            }
            if (best != null) {
                val hadCap = currentState.board[best.toRow][best.toCol] != null || best.isEnPassant
                undoStack.add(currentState)
                currentState = ChessEngine.applyMove(currentState, best)
                onPlayCue(if (hadCap) SoundCue.CAPTURE else SoundCue.MOVE)
                onSaveState(ChessEngine.serialize(currentState), "${currentState.moveHistory.size} moves")
            }
            aiThinking = false
        }
    }

    // Record result on completion
    LaunchedEffect(isTerminal) {
        if (isTerminal && !outcomeRecorded) {
            outcomeRecorded = true
            onClearSaved()
            onPlayCue(SoundCue.WIN)
            val p1 = sessionConfig.players.getOrNull(0)?.name ?: "White"
            val p2 = sessionConfig.players.getOrNull(1)?.name ?: "Black"
            val (outcome, winner, summary) = when {
                flaggedWinner == PieceColor.WHITE || currentState.status == ChessGameStatus.CHECKMATE_WHITE ->
                    Triple(MatchOutcome.WIN, p1, "White won in ${currentState.moveHistory.size} ply")
                flaggedWinner == PieceColor.BLACK || currentState.status == ChessGameStatus.CHECKMATE_BLACK ->
                    Triple(
                        if (sessionConfig.playMode == PlayMode.VS_AI) MatchOutcome.LOSS else MatchOutcome.WIN,
                        p2,
                        "Black won in ${currentState.moveHistory.size} ply"
                    )
                else -> Triple(MatchOutcome.DRAW, "Draw", "Drawn (${currentState.status.name.lowercase().replace('_', ' ')})")
            }
            onRecordOutcome(outcome, winner, summary, 100, elapsedSeconds)
        }
    }

    fun executeHumanMove(move: ChessMove) {
        val hadCap = currentState.board[move.toRow][move.toCol] != null || move.isEnPassant
        undoStack.add(currentState)
        currentState = ChessEngine.applyMove(currentState, move)
        selectedSquare = null
        legalTargets = emptyList()
        onPlayCue(if (hadCap) SoundCue.CAPTURE else SoundCue.MOVE)
        onSaveState(ChessEngine.serialize(currentState), "${currentState.moveHistory.size} moves")
    }

    fun restart() {
        undoStack.clear()
        currentState = ChessEngine.initialState()
        selectedSquare = null
        legalTargets = emptyList()
        whiteSeconds = clockMinutes * 60
        blackSeconds = clockMinutes * 60
        elapsedSeconds = 0
        flaggedWinner = null
        outcomeRecorded = false
        onClearSaved()
    }

    GameScaffold(
        game = game,
        subtitle = if (sessionConfig.playMode == PlayMode.VS_AI) "vs ${sessionConfig.aiDifficulty.label} AI" else "Pass & Play",
        isFavorite = isFavorite,
        canUndo = undoStack.isNotEmpty() && !aiThinking,
        onBack = {
            if (!isTerminal && currentState.moveHistory.isNotEmpty()) {
                onSaveState(ChessEngine.serialize(currentState), "${currentState.moveHistory.size} moves")
            }
            onBack()
        },
        onToggleFavorite = onToggleFavorite,
        onUndo = {
            if (undoStack.isNotEmpty() && !aiThinking) {
                val stepsBack = if (sessionConfig.playMode == PlayMode.VS_AI && undoStack.size >= 2) 2 else 1
                repeat(stepsBack) {
                    if (undoStack.isNotEmpty()) currentState = undoStack.removeAt(undoStack.lastIndex)
                }
                selectedSquare = null
                legalTargets = emptyList()
                flaggedWinner = null
                outcomeRecorded = false
            }
        },
        onRestart = ::restart
    ) {
        // Player Bar - Black (Top)
        val p1 = sessionConfig.players.getOrNull(0)
        val p2 = sessionConfig.players.getOrNull(1)
        ChessPlayerBar(
            name = p2?.name ?: "Black",
            avatar = p2?.avatar,
            isActive = currentState.turn == PieceColor.BLACK && !isTerminal,
            captured = currentState.capturedByBlack,
            clockSeconds = if (clockMinutes > 0) blackSeconds else null,
            statusNote = if (aiThinking) "Thinking..." else if (currentState.status == ChessGameStatus.CHECK && currentState.turn == PieceColor.BLACK) "IN CHECK" else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chessboard
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight)
            Surface(
                modifier = Modifier
                    .size(boardSize)
                    .testTag("chess_board"),
                shape = RoundedCornerShape(10.dp),
                color = activeBoardTheme.boardFrame,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    for (r in 0..7) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..7) {
                                val isLight = (r + c) % 2 == 0
                                val isSelected = selectedSquare == (r to c)
                                val targetMoves = legalTargets.filter { it.toRow == r && it.toCol == c }
                                val isLegalDest = targetMoves.isNotEmpty()
                                val isLastMove = currentState.lastMove?.let {
                                    (it.fromRow == r && it.fromCol == c) || (it.toRow == r && it.toCol == c)
                                } ?: false
                                val piece = currentState.board[r][c]
                                val isKingCheck = piece?.type == PieceType.KING &&
                                    piece.color == currentState.turn &&
                                    (currentState.status == ChessGameStatus.CHECK ||
                                        currentState.status == ChessGameStatus.CHECKMATE_WHITE ||
                                        currentState.status == ChessGameStatus.CHECKMATE_BLACK)

                                val bgColor = when {
                                    isKingCheck -> Color(0xFFB71C1C)
                                    isSelected -> activeBoardTheme.highlight
                                    isLastMove -> if (isLight) activeBoardTheme.lightSquare.copy(alpha = 0.82f) else activeBoardTheme.darkSquare.copy(alpha = 0.82f)
                                    isLight -> activeBoardTheme.lightSquare
                                    else -> activeBoardTheme.darkSquare
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(bgColor)
                                        .then(
                                            if (isLastMove) Modifier.border(1.dp, activeBoardTheme.highlight) else Modifier
                                        )
                                        .clickable(enabled = !isTerminal && !isAiTurn) {
                                            if (isLegalDest) {
                                                if (targetMoves.size > 1) {
                                                    pendingPromotionMoves = targetMoves
                                                } else {
                                                    executeHumanMove(targetMoves.first())
                                                }
                                            } else if (piece != null && piece.color == currentState.turn) {
                                                selectedSquare = r to c
                                                legalTargets = ChessEngine.legalMovesForSquare(currentState, r, c)
                                            } else {
                                                selectedSquare = null
                                                legalTargets = emptyList()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (piece != null) {
                                        Text(
                                            text = pieceGlyph(piece, activePieceTheme),
                                            fontSize = (boardSize.value / 11.5f).sp,
                                            fontWeight = if (activePieceTheme == PieceTheme.HIGH_CONTRAST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (piece.color == PieceColor.WHITE) Color(0xFFFFFFFF) else Color(0xFF111111),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (isLegalDest && settings.showLegalMoves) {
                                        if (piece == null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.35f))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(2.dp)
                                                    .border(2.5.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Player Bar - White (Bottom)
        ChessPlayerBar(
            name = p1?.name ?: "White",
            avatar = p1?.avatar,
            isActive = currentState.turn == PieceColor.WHITE && !isTerminal,
            captured = currentState.capturedByWhite,
            clockSeconds = if (clockMinutes > 0) whiteSeconds else null,
            statusNote = if (currentState.status == ChessGameStatus.CHECK && currentState.turn == PieceColor.WHITE) "IN CHECK" else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Move History & Quick Theme Strip
        MonoCard(elevated = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (currentState.moveHistory.isEmpty()) "White to move • Tap piece to begin"
                    else currentState.moveHistory.chunked(2).mapIndexed { idx, pair ->
                        "${idx + 1}. ${pair.joinToString(" ")}"
                    }.joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Board & Piece Theme Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BoardTheme.entries.forEach { bt ->
                FilterChip(
                    selected = activeBoardTheme == bt,
                    onClick = { activeBoardTheme = bt },
                    label = { Text(bt.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
            PieceTheme.entries.forEach { pt ->
                FilterChip(
                    selected = activePieceTheme == pt,
                    onClick = { activePieceTheme = pt },
                    label = { Text(pt.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }

    // Pawn Promotion Dialog
    if (pendingPromotionMoves != null) {
        val options = pendingPromotionMoves!!
        AlertDialog(
            onDismissRequest = { pendingPromotionMoves = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Promote Pawn") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.forEach { move ->
                        val type = move.promotion ?: PieceType.QUEEN
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    pendingPromotionMoves = null
                                    executeHumanMove(move)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = pieceGlyph(ChessPiece(currentState.turn, type), activePieceTheme),
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingPromotionMoves = null }) { Text("Cancel") }
            }
        )
    }

    if (isTerminal) {
        val title = when {
            flaggedWinner == PieceColor.WHITE -> "White Wins on Time"
            flaggedWinner == PieceColor.BLACK -> "Black Wins on Time"
            currentState.status == ChessGameStatus.CHECKMATE_WHITE -> "Checkmate — White Wins"
            currentState.status == ChessGameStatus.CHECKMATE_BLACK -> "Checkmate — Black Wins"
            currentState.status == ChessGameStatus.STALEMATE -> "Draw by Stalemate"
            currentState.status == ChessGameStatus.DRAW_REPETITION -> "Draw by Repetition"
            currentState.status == ChessGameStatus.DRAW_FIFTY_MOVE -> "Draw (50-Move Rule)"
            else -> "Draw (Insufficient Material)"
        }
        GameCompletionDialog(
            title = title,
            subtitle = "Completed in ${currentState.moveHistory.size} half-moves.",
            onPlayAgain = ::restart,
            onExit = onBack
        )
    }
}

@Composable
private fun ChessPlayerBar(
    name: String,
    avatar: com.example.domain.AvatarSymbol?,
    isActive: Boolean,
    captured: List<ChessPiece>,
    clockSeconds: Int?,
    statusNote: String?
) {
    MonoCard(elevated = isActive, borderHighlight = isActive) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatar != null) {
                AvatarBadge(symbol = avatar, isActive = isActive, size = 30.dp)
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                    if (statusNote != null) {
                        Text(
                            text = statusNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (captured.isNotEmpty()) {
                    Text(
                        text = captured.joinToString("") { pieceGlyph(it, PieceTheme.CLASSIC) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (clockSeconds != null) {
                val mins = clockSeconds / 60
                val secs = clockSeconds % 60
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun pieceGlyph(piece: ChessPiece, theme: PieceTheme): String {
    if (theme == PieceTheme.GEOMETRIC) {
        return when (piece.type) {
            PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
            PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
            PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
            PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
            PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
            PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟"
        }
    }
    if (theme == PieceTheme.HIGH_CONTRAST) {
        return piece.type.symbol.toString()
    }
    return when (piece.type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    }
}
