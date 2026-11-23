package com.example.ui.games.chess

import com.example.domain.AiDifficulty
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

enum class PieceColor {
    WHITE, BLACK;
    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: Char, val value: Int) {
    PAWN('P', 100),
    KNIGHT('N', 320),
    BISHOP('B', 330),
    ROOK('R', 500),
    QUEEN('Q', 900),
    KING('K', 20000)
}

data class ChessPiece(val color: PieceColor, val type: PieceType)

data class ChessMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val promotion: PieceType? = null,
    val isCastleKingSide: Boolean = false,
    val isCastleQueenSide: Boolean = false,
    val isEnPassant: Boolean = false
)

enum class ChessGameStatus {
    ONGOING,
    CHECK,
    CHECKMATE_WHITE,
    CHECKMATE_BLACK,
    STALEMATE,
    DRAW_REPETITION,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT
}

data class ChessState(
    val board: List<List<ChessPiece?>>,
    val turn: PieceColor = PieceColor.WHITE,
    val whiteKingCastle: Boolean = true,
    val whiteQueenCastle: Boolean = true,
    val blackKingCastle: Boolean = true,
    val blackQueenCastle: Boolean = true,
    val enPassantTarget: Pair<Int, Int>? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val moveHistory: List<String> = emptyList(),
    val lastMove: ChessMove? = null,
    val positionCounts: Map<String, Int> = emptyMap(),
    val status: ChessGameStatus = ChessGameStatus.ONGOING,
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList()
)

object ChessEngine {

    fun initialState(): ChessState {
        val grid = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
        val backRank = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (c in 0..7) {
            grid[0][c] = ChessPiece(PieceColor.BLACK, backRank[c])
            grid[1][c] = ChessPiece(PieceColor.BLACK, PieceType.PAWN)
            grid[6][c] = ChessPiece(PieceColor.WHITE, PieceType.PAWN)
            grid[7][c] = ChessPiece(PieceColor.WHITE, backRank[c])
        }
        val base = ChessState(board = grid.map { it.toList() })
        val key = positionKey(base)
        return base.copy(positionCounts = mapOf(key to 1))
    }

    fun legalMovesForSquare(state: ChessState, row: Int, col: Int): List<ChessMove> {
        val piece = state.board[row][col] ?: return emptyList()
        if (piece.color != state.turn) return emptyList()
        return legalMoves(state).filter { it.fromRow == row && it.fromCol == col }
    }

    fun legalMoves(state: ChessState): List<ChessMove> {
        val pseudo = pseudoLegalMoves(state, state.turn)
        return pseudo.filter { move ->
            val nextBoard = applyMoveToBoard(state.board, move)
            !isKingInCheck(nextBoard, state.turn)
        }
    }

    fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val movingPiece = state.board[move.fromRow][move.fromCol] ?: return state
        val capturedPiece = if (move.isEnPassant) {
            state.board[move.fromRow][move.toCol]
        } else {
            state.board[move.toRow][move.toCol]
        }

        val nextBoard = applyMoveToBoard(state.board, move)

        var wkc = state.whiteKingCastle
        var wqc = state.whiteQueenCastle
        var bkc = state.blackKingCastle
        var bqc = state.blackQueenCastle

        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) { wkc = false; wqc = false }
            else { bkc = false; bqc = false }
        }
        if (movingPiece.type == PieceType.ROOK) {
            if (move.fromRow == 7 && move.fromCol == 7) wkc = false
            if (move.fromRow == 7 && move.fromCol == 0) wqc = false
            if (move.fromRow == 0 && move.fromCol == 7) bkc = false
            if (move.fromRow == 0 && move.fromCol == 0) bqc = false
        }
        if (move.toRow == 7 && move.toCol == 7) wkc = false
        if (move.toRow == 7 && move.toCol == 0) wqc = false
        if (move.toRow == 0 && move.toCol == 7) bkc = false
        if (move.toRow == 0 && move.toCol == 0) bqc = false

        val nextEp = if (movingPiece.type == PieceType.PAWN && abs(move.toRow - move.fromRow) == 2) {
            ((move.fromRow + move.toRow) / 2) to move.fromCol
        } else null

        val resetClock = movingPiece.type == PieceType.PAWN || capturedPiece != null
        val nextHalfClock = if (resetClock) 0 else state.halfMoveClock + 1
        val nextFull = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber
        val nextTurn = state.turn.opposite()

        val capW = if (capturedPiece != null && state.turn == PieceColor.WHITE) state.capturedByWhite + capturedPiece else state.capturedByWhite
        val capB = if (capturedPiece != null && state.turn == PieceColor.BLACK) state.capturedByBlack + capturedPiece else state.capturedByBlack

        val tempState = state.copy(
            board = nextBoard,
            turn = nextTurn,
            whiteKingCastle = wkc,
            whiteQueenCastle = wqc,
            blackKingCastle = bkc,
            blackQueenCastle = bqc,
            enPassantTarget = nextEp,
            halfMoveClock = nextHalfClock,
            fullMoveNumber = nextFull,
            lastMove = move,
            capturedByWhite = capW,
            capturedByBlack = capB
        )

        val key = positionKey(tempState)
        val newCounts = state.positionCounts.toMutableMap()
        val count = (newCounts[key] ?: 0) + 1
        newCounts[key] = count

        val nextLegal = legalMoves(tempState)
        val inCheck = isKingInCheck(nextBoard, nextTurn)

        val nextStatus = when {
            nextLegal.isEmpty() && inCheck -> {
                if (state.turn == PieceColor.WHITE) ChessGameStatus.CHECKMATE_WHITE else ChessGameStatus.CHECKMATE_BLACK
            }
            nextLegal.isEmpty() -> ChessGameStatus.STALEMATE
            count >= 3 -> ChessGameStatus.DRAW_REPETITION
            nextHalfClock >= 100 -> ChessGameStatus.DRAW_FIFTY_MOVE
            isInsufficientMaterial(nextBoard) -> ChessGameStatus.DRAW_INSUFFICIENT
            inCheck -> ChessGameStatus.CHECK
            else -> ChessGameStatus.ONGOING
        }

        val notation = formatNotation(movingPiece, move, capturedPiece != null, nextStatus)
        return tempState.copy(
            moveHistory = state.moveHistory + notation,
            positionCounts = newCounts,
            status = nextStatus
        )
    }

    private fun formatNotation(
        piece: ChessPiece,
        move: ChessMove,
        isCapture: Boolean,
        status: ChessGameStatus
    ): String {
        val base = when {
            move.isCastleKingSide -> "O-O"
            move.isCastleQueenSide -> "O-O-O"
            else -> {
                val fileChar = ('a' + move.fromCol)
                val dest = "${'a' + move.toCol}${8 - move.toRow}"
                val pChar = if (piece.type == PieceType.PAWN) "" else piece.type.symbol.toString()
                val cap = if (isCapture) {
                    if (piece.type == PieceType.PAWN) "${fileChar}x" else "x"
                } else ""
                val promo = if (move.promotion != null) "=${move.promotion.symbol}" else ""
                "$pChar$cap$dest$promo"
            }
        }
        val suffix = when (status) {
            ChessGameStatus.CHECKMATE_WHITE, ChessGameStatus.CHECKMATE_BLACK -> "#"
            ChessGameStatus.CHECK -> "+"
            else -> ""
        }
        return base + suffix
    }

    private fun applyMoveToBoard(board: List<List<ChessPiece?>>, move: ChessMove): List<List<ChessPiece?>> {
        val next = board.map { it.toMutableList() }
        val moving = next[move.fromRow][move.fromCol] ?: return board
        next[move.fromRow][move.fromCol] = null

        if (move.isEnPassant) {
            next[move.fromRow][move.toCol] = null
        }

        val placed = if (move.promotion != null) ChessPiece(moving.color, move.promotion) else moving
        next[move.toRow][move.toCol] = placed

        if (move.isCastleKingSide) {
            next[move.toRow][5] = next[move.toRow][7]
            next[move.toRow][7] = null
        } else if (move.isCastleQueenSide) {
            next[move.toRow][3] = next[move.toRow][0]
            next[move.toRow][0] = null
        }
        return next.map { it.toList() }
    }

    private fun pseudoLegalMoves(state: ChessState, color: PieceColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val board = state.board
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c] ?: continue
                if (piece.color != color) continue
                when (piece.type) {
                    PieceType.PAWN -> {
                        val dir = if (color == PieceColor.WHITE) -1 else 1
                        val startRow = if (color == PieceColor.WHITE) 6 else 1
                        val promoRow = if (color == PieceColor.WHITE) 0 else 7
                        val oneR = r + dir
                        if (oneR in 0..7 && board[oneR][c] == null) {
                            if (oneR == promoRow) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                    moves.add(ChessMove(r, c, oneR, c, promotion = it))
                                }
                            } else {
                                moves.add(ChessMove(r, c, oneR, c))
                                val twoR = r + 2 * dir
                                if (r == startRow && board[twoR][c] == null) {
                                    moves.add(ChessMove(r, c, twoR, c))
                                }
                            }
                        }
                        for (dc in listOf(-1, 1)) {
                            val nc = c + dc
                            if (oneR in 0..7 && nc in 0..7) {
                                val target = board[oneR][nc]
                                if (target != null && target.color != color) {
                                    if (oneR == promoRow) {
                                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                            moves.add(ChessMove(r, c, oneR, nc, promotion = it))
                                        }
                                    } else {
                                        moves.add(ChessMove(r, c, oneR, nc))
                                    }
                                } else if (state.enPassantTarget == (oneR to nc)) {
                                    moves.add(ChessMove(r, c, oneR, nc, isEnPassant = true))
                                }
                            }
                        }
                    }
                    PieceType.KNIGHT -> {
                        val jumps = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
                        for ((dr, dc) in jumps) {
                            val nr = r + dr; val nc = c + dc
                            if (nr in 0..7 && nc in 0..7) {
                                val t = board[nr][nc]
                                if (t == null || t.color != color) moves.add(ChessMove(r, c, nr, nc))
                            }
                        }
                    }
                    PieceType.BISHOP -> addSlidingMoves(board, r, c, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
                    PieceType.ROOK -> addSlidingMoves(board, r, c, color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    PieceType.QUEEN -> addSlidingMoves(board, r, c, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    PieceType.KING -> {
                        for (dr in -1..1) {
                            for (dc in -1..1) {
                                if (dr == 0 && dc == 0) continue
                                val nr = r + dr; val nc = c + dc
                                if (nr in 0..7 && nc in 0..7) {
                                    val t = board[nr][nc]
                                    if (t == null || t.color != color) moves.add(ChessMove(r, c, nr, nc))
                                }
                            }
                        }
                        val homeRow = if (color == PieceColor.WHITE) 7 else 0
                        if (r == homeRow && c == 4 && !isKingInCheck(board, color)) {
                            val canKS = if (color == PieceColor.WHITE) state.whiteKingCastle else state.blackKingCastle
                            val canQS = if (color == PieceColor.WHITE) state.whiteQueenCastle else state.blackQueenCastle
                            if (canKS && board[homeRow][5] == null && board[homeRow][6] == null &&
                                board[homeRow][7]?.type == PieceType.ROOK && board[homeRow][7]?.color == color &&
                                !isSquareAttacked(board, homeRow, 5, color.opposite()) &&
                                !isSquareAttacked(board, homeRow, 6, color.opposite())
                            ) {
                                moves.add(ChessMove(homeRow, 4, homeRow, 6, isCastleKingSide = true))
                            }
                            if (canQS && board[homeRow][3] == null && board[homeRow][2] == null && board[homeRow][1] == null &&
                                board[homeRow][0]?.type == PieceType.ROOK && board[homeRow][0]?.color == color &&
                                !isSquareAttacked(board, homeRow, 3, color.opposite()) &&
                                !isSquareAttacked(board, homeRow, 2, color.opposite())
                            ) {
                                moves.add(ChessMove(homeRow, 4, homeRow, 2, isCastleQueenSide = true))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(
        board: List<List<ChessPiece?>>,
        r: Int,
        c: Int,
        color: PieceColor,
        dirs: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for ((dr, dc) in dirs) {
            var nr = r + dr
            var nc = c + dc
            while (nr in 0..7 && nc in 0..7) {
                val target = board[nr][nc]
                if (target == null) {
                    out.add(ChessMove(r, c, nr, nc))
                } else {
                    if (target.color != color) out.add(ChessMove(r, c, nr, nc))
                    break
                }
                nr += dr
                nc += dc
            }
        }
    }

    fun isKingInCheck(board: List<List<ChessPiece?>>, color: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p?.color == color && p.type == PieceType.KING) {
                    return isSquareAttacked(board, r, c, color.opposite())
                }
            }
        }
        return false
    }

    private fun isSquareAttacked(board: List<List<ChessPiece?>>, row: Int, col: Int, byColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnSourceDir = if (byColor == PieceColor.WHITE) 1 else -1
        val pr = row + pawnSourceDir
        if (pr in 0..7) {
            for (dc in listOf(-1, 1)) {
                val pc = col + dc
                if (pc in 0..7) {
                    val p = board[pr][pc]
                    if (p?.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }
        // Knight attacks
        val knightJumps = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in knightJumps) {
            val nr = row + dr; val nc = col + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p?.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }
        // King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr; val nc = col + dc
                if (nr in 0..7 && nc in 0..7) {
                    val p = board[nr][nc]
                    if (p?.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }
        // Rook/Queen orth
        for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            var nr = row + dr; var nc = col + dc
            while (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += dr; nc += dc
            }
        }
        // Bishop/Queen diag
        for ((dr, dc) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
            var nr = row + dr; var nc = col + dc
            while (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += dr; nc += dc
            }
        }
        return false
    }

    private fun isInsufficientMaterial(board: List<List<ChessPiece?>>): Boolean {
        val minorOrMajor = mutableListOf<Triple<PieceColor, PieceType, Int>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) return false
                if (p.type != PieceType.KING) {
                    minorOrMajor.add(Triple(p.color, p.type, (r + c) % 2))
                }
            }
        }
        if (minorOrMajor.size <= 1) return true
        if (minorOrMajor.size == 2) {
            val (c1, t1, sq1) = minorOrMajor[0]
            val (c2, t2, sq2) = minorOrMajor[1]
            if (c1 != c2 && t1 == PieceType.BISHOP && t2 == PieceType.BISHOP && sq1 == sq2) return true
        }
        return false
    }

    private fun positionKey(state: ChessState): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c]
                sb.append(
                    if (p == null) '.'
                    else if (p.color == PieceColor.WHITE) p.type.symbol.uppercaseChar()
                    else p.type.symbol.lowercaseChar()
                )
            }
        }
        sb.append(if (state.turn == PieceColor.WHITE) 'w' else 'b')
        sb.append(state.whiteKingCastle).append(state.whiteQueenCastle)
        sb.append(state.blackKingCastle).append(state.blackQueenCastle)
        sb.append(state.enPassantTarget?.let { "${it.first}${it.second}" } ?: "-")
        return sb.toString()
    }

    // ==================== AI ENGINE ====================
    fun chooseBestMove(state: ChessState, difficulty: AiDifficulty): ChessMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == AiDifficulty.EASY) {
            val captures = moves.filter { state.board[it.toRow][it.toCol] != null }
            return if (captures.isNotEmpty() && Math.random() < 0.55) captures.random() else moves.random()
        }
        val depth = when (difficulty) {
            AiDifficulty.EASY -> 1
            AiDifficulty.MEDIUM -> 2
            AiDifficulty.HARD, AiDifficulty.EXPERT -> 3
        }
        val maximizing = state.turn == PieceColor.WHITE
        val ordered = orderMoves(state, moves)
        var bestMove = ordered.first()
        var bestEval = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var alpha = Int.MIN_VALUE + 1
        var beta = Int.MAX_VALUE - 1

        for (move in ordered) {
            val child = applyMoveLight(state, move)
            val eval = minimax(child, depth - 1, alpha, beta, !maximizing)
            if (maximizing) {
                if (eval > bestEval) {
                    bestEval = eval
                    bestMove = move
                }
                alpha = maxOf(alpha, eval)
            } else {
                if (eval < bestEval) {
                    bestEval = eval
                    bestMove = move
                }
                beta = minOf(beta, eval)
            }
        }
        return bestMove
    }

    private fun orderMoves(state: ChessState, moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { m ->
            var score = 0
            val victim = state.board[m.toRow][m.toCol]
            val attacker = state.board[m.fromRow][m.fromCol]
            if (victim != null) {
                score += victim.type.value * 10 - (attacker?.type?.value ?: 0)
            }
            if (m.promotion != null) score += m.promotion.value
            if (m.toRow in 3..4 && m.toCol in 3..4) score += 25
            score
        }
    }

    private fun applyMoveLight(state: ChessState, move: ChessMove): ChessState {
        val moving = state.board[move.fromRow][move.fromCol] ?: return state
        val nextBoard = applyMoveToBoard(state.board, move)
        val nextEp = if (moving.type == PieceType.PAWN && abs(move.toRow - move.fromRow) == 2) {
            ((move.fromRow + move.toRow) / 2) to move.fromCol
        } else null
        return state.copy(
            board = nextBoard,
            turn = state.turn.opposite(),
            enPassantTarget = nextEp
        )
    }

    private fun minimax(
        state: ChessState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        maximizing: Boolean
    ): Int {
        if (depth == 0) return evaluateBoard(state.board)
        val moves = orderMoves(state, legalMoves(state))
        if (moves.isEmpty()) {
            return if (isKingInCheck(state.board, state.turn)) {
                if (maximizing) -100_000 - depth else 100_000 + depth
            } else 0
        }
        var alpha = alphaInit
        var beta = betaInit
        if (maximizing) {
            var maxEval = Int.MIN_VALUE
            for (m in moves) {
                val child = applyMoveLight(state, m)
                val ev = minimax(child, depth - 1, alpha, beta, false)
                maxEval = maxOf(maxEval, ev)
                alpha = maxOf(alpha, ev)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (m in moves) {
                val child = applyMoveLight(state, m)
                val ev = minimax(child, depth - 1, alpha, beta, true)
                minEval = minOf(minEval, ev)
                beta = minOf(beta, ev)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private val pawnTable = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 27, 27, 10, 5, 5),
        intArrayOf(0, 0, 0, 24, 24, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val knightTable = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private fun evaluateBoard(board: List<List<ChessPiece?>>): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                val tableRow = if (p.color == PieceColor.WHITE) r else 7 - r
                val positional = when (p.type) {
                    PieceType.PAWN -> pawnTable[tableRow][c]
                    PieceType.KNIGHT -> knightTable[tableRow][c]
                    PieceType.BISHOP, PieceType.QUEEN -> if (r in 2..5 && c in 2..5) 12 else 0
                    else -> 0
                }
                val total = p.type.value + positional
                score += if (p.color == PieceColor.WHITE) total else -total
            }
        }
        return score
    }

    fun serialize(state: ChessState): String {
        val obj = JSONObject()
        val rows = JSONArray()
        for (r in 0..7) {
            val sb = StringBuilder()
            for (c in 0..7) {
                val p = state.board[r][c]
                sb.append(
                    if (p == null) '.'
                    else if (p.color == PieceColor.WHITE) p.type.symbol.uppercaseChar()
                    else p.type.symbol.lowercaseChar()
                )
            }
            rows.put(sb.toString())
        }
        obj.put("board", rows)
        obj.put("turn", state.turn.name)
        obj.put("wkc", state.whiteKingCastle)
        obj.put("wqc", state.whiteQueenCastle)
        obj.put("bkc", state.blackKingCastle)
        obj.put("bqc", state.blackQueenCastle)
        obj.put("half", state.halfMoveClock)
        obj.put("full", state.fullMoveNumber)
        val hist = JSONArray()
        state.moveHistory.forEach { hist.put(it) }
        obj.put("hist", hist)
        return obj.toString()
    }

    fun deserialize(json: String): ChessState? {
        return runCatching {
            val obj = JSONObject(json)
            val rows = obj.getJSONArray("board")
            val board = List(8) { r ->
                val str = rows.getString(r)
                List(8) { c ->
                    val ch = str[c]
                    if (ch == '.') null
                    else {
                        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = PieceType.entries.first { it.symbol == ch.uppercaseChar() }
                        ChessPiece(color, type)
                    }
                }
            }
            val histArr = obj.getJSONArray("hist")
            val hist = List(histArr.length()) { histArr.getString(it) }
            val base = ChessState(
                board = board,
                turn = PieceColor.valueOf(obj.getString("turn")),
                whiteKingCastle = obj.getBoolean("wkc"),
                whiteQueenCastle = obj.getBoolean("wqc"),
                blackKingCastle = obj.getBoolean("bkc"),
                blackQueenCastle = obj.getBoolean("bqc"),
                halfMoveClock = obj.getInt("half"),
                fullMoveNumber = obj.getInt("full"),
                moveHistory = hist
            )
            base.copy(positionCounts = mapOf(positionKey(base) to 1))
        }.getOrNull()
    }
}
