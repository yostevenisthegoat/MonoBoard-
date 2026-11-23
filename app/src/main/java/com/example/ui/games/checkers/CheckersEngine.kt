package com.example.ui.games.checkers

import com.example.domain.AiDifficulty
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

enum class CheckerSide {
    LIGHT, DARK;
    fun opposite() = if (this == LIGHT) DARK else LIGHT
}

data class CheckerPiece(val side: CheckerSide, val isKing: Boolean = false)

data class CheckerMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val capturedRow: Int? = null,
    val capturedCol: Int? = null
) {
    val isCapture: Boolean get() = capturedRow != null
}

data class CheckersState(
    val board: List<List<CheckerPiece?>>,
    val turn: CheckerSide = CheckerSide.LIGHT,
    val mustContinueFrom: Pair<Int, Int>? = null,
    val forcedCapture: Boolean = true,
    val winner: CheckerSide? = null,
    val moveCount: Int = 0
)

object CheckersEngine {
    fun initialState(forcedCapture: Boolean = true): CheckersState {
        val grid = MutableList(8) { MutableList<CheckerPiece?>(8) { null } }
        for (r in 0..7) {
            for (c in 0..7) {
                if ((r + c) % 2 == 1) {
                    if (r <= 2) grid[r][c] = CheckerPiece(CheckerSide.DARK)
                    else if (r >= 5) grid[r][c] = CheckerPiece(CheckerSide.LIGHT)
                }
            }
        }
        return CheckersState(board = grid.map { it.toList() }, forcedCapture = forcedCapture)
    }

    fun legalMoves(state: CheckersState): List<CheckerMove> {
        if (state.winner != null) return emptyList()
        val locked = state.mustContinueFrom
        if (locked != null) {
            return capturesForPiece(state.board, locked.first, locked.second, state.turn)
        }
        val allCaptures = mutableListOf<CheckerMove>()
        val allSteps = mutableListOf<CheckerMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.side != state.turn) continue
                allCaptures.addAll(capturesForPiece(state.board, r, c, state.turn))
                allSteps.addAll(stepsForPiece(state.board, r, c, p))
            }
        }
        return if (state.forcedCapture && allCaptures.isNotEmpty()) allCaptures else allCaptures + allSteps
    }

    private fun stepsForPiece(board: List<List<CheckerPiece?>>, r: Int, c: Int, p: CheckerPiece): List<CheckerMove> {
        val rowDirs = when {
            p.isKing -> listOf(-1, 1)
            p.side == CheckerSide.LIGHT -> listOf(-1)
            else -> listOf(1)
        }
        val out = mutableListOf<CheckerMove>()
        for (dr in rowDirs) {
            for (dc in listOf(-1, 1)) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0..7 && nc in 0..7 && board[nr][nc] == null) {
                    out.add(CheckerMove(r, c, nr, nc))
                }
            }
        }
        return out
    }

    private fun capturesForPiece(board: List<List<CheckerPiece?>>, r: Int, c: Int, side: CheckerSide): List<CheckerMove> {
        val p = board[r][c] ?: return emptyList()
        if (p.side != side) return emptyList()
        val rowDirs = when {
            p.isKing -> listOf(-1, 1)
            side == CheckerSide.LIGHT -> listOf(-1)
            else -> listOf(1)
        }
        val out = mutableListOf<CheckerMove>()
        for (dr in rowDirs) {
            for (dc in listOf(-1, 1)) {
                val mr = r + dr; val mc = c + dc
                val jr = r + 2 * dr; val jc = c + 2 * dc
                if (jr in 0..7 && jc in 0..7) {
                    val mid = board[mr][mc]
                    if (mid != null && mid.side != side && board[jr][jc] == null) {
                        out.add(CheckerMove(r, c, jr, jc, capturedRow = mr, capturedCol = mc))
                    }
                }
            }
        }
        return out
    }

    fun applyMove(state: CheckersState, move: CheckerMove): CheckersState {
        val next = state.board.map { it.toMutableList() }
        val moving = next[move.fromRow][move.fromCol] ?: return state
        next[move.fromRow][move.fromCol] = null
        if (move.capturedRow != null && move.capturedCol != null) {
            next[move.capturedRow][move.capturedCol] = null
        }
        val promoted = moving.isKing ||
            (moving.side == CheckerSide.LIGHT && move.toRow == 0) ||
            (moving.side == CheckerSide.DARK && move.toRow == 7)
        val landed = moving.copy(isKing = promoted)
        next[move.toRow][move.toCol] = landed
        val immutableBoard = next.map { it.toList() }

        // Check chain jump if capture happened and not just promoted
        if (move.isCapture && !(!moving.isKing && promoted)) {
            val further = capturesForPiece(immutableBoard, move.toRow, move.toCol, state.turn)
            if (further.isNotEmpty()) {
                return state.copy(
                    board = immutableBoard,
                    mustContinueFrom = move.toRow to move.toCol,
                    moveCount = state.moveCount + 1
                )
            }
        }

        val nextTurn = state.turn.opposite()
        val candidate = state.copy(
            board = immutableBoard,
            turn = nextTurn,
            mustContinueFrom = null,
            moveCount = state.moveCount + 1
        )
        val oppMoves = legalMoves(candidate)
        val winner = if (oppMoves.isEmpty()) state.turn else null
        return candidate.copy(winner = winner)
    }

    fun chooseAiMove(state: CheckersState, difficulty: AiDifficulty): CheckerMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == AiDifficulty.EASY) {
            return moves.random()
        }
        val depth = if (difficulty == AiDifficulty.MEDIUM) 2 else 4
        var bestMove = moves.first()
        var bestScore = if (state.turn == CheckerSide.DARK) Int.MIN_VALUE else Int.MAX_VALUE
        for (m in moves) {
            val next = applyMove(state, m)
            val eval = minimax(next, depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1)
            if (state.turn == CheckerSide.DARK) {
                if (eval > bestScore) { bestScore = eval; bestMove = m }
            } else {
                if (eval < bestScore) { bestScore = eval; bestMove = m }
            }
        }
        return bestMove
    }

    private fun minimax(state: CheckersState, depth: Int, alphaInit: Int, betaInit: Int): Int {
        if (state.winner != null) {
            return if (state.winner == CheckerSide.DARK) 10_000 + depth else -10_000 - depth
        }
        if (depth == 0) return evaluate(state.board)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return if (state.turn == CheckerSide.DARK) -10_000 else 10_000
        var alpha = alphaInit
        var beta = betaInit
        return if (state.turn == CheckerSide.DARK) {
            var best = Int.MIN_VALUE
            for (m in moves) {
                best = maxOf(best, minimax(applyMove(state, m), depth - 1, alpha, beta))
                alpha = maxOf(alpha, best)
                if (beta <= alpha) break
            }
            best
        } else {
            var best = Int.MAX_VALUE
            for (m in moves) {
                best = minOf(best, minimax(applyMove(state, m), depth - 1, alpha, beta))
                beta = minOf(beta, best)
                if (beta <= alpha) break
            }
            best
        }
    }

    private fun evaluate(board: List<List<CheckerPiece?>>): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                val base = if (p.isKing) 180 else 100
                val advance = if (p.side == CheckerSide.DARK) r * 4 else (7 - r) * 4
                val center = if (c in 2..5 && r in 2..5) 10 else 0
                val total = base + advance + center
                score += if (p.side == CheckerSide.DARK) total else -total
            }
        }
        return score
    }

    fun serialize(state: CheckersState): String {
        val obj = JSONObject()
        val rows = JSONArray()
        for (r in 0..7) {
            val sb = StringBuilder()
            for (c in 0..7) {
                val p = state.board[r][c]
                sb.append(
                    when {
                        p == null -> '.'
                        p.side == CheckerSide.LIGHT && !p.isKing -> 'l'
                        p.side == CheckerSide.LIGHT && p.isKing -> 'L'
                        p.side == CheckerSide.DARK && !p.isKing -> 'd'
                        else -> 'D'
                    }
                )
            }
            rows.put(sb.toString())
        }
        obj.put("board", rows)
        obj.put("turn", state.turn.name)
        obj.put("forced", state.forcedCapture)
        obj.put("moves", state.moveCount)
        return obj.toString()
    }

    fun deserialize(json: String): CheckersState? = runCatching {
        val obj = JSONObject(json)
        val rows = obj.getJSONArray("board")
        val board = List(8) { r ->
            val s = rows.getString(r)
            List(8) { c ->
                when (s[c]) {
                    'l' -> CheckerPiece(CheckerSide.LIGHT, false)
                    'L' -> CheckerPiece(CheckerSide.LIGHT, true)
                    'd' -> CheckerPiece(CheckerSide.DARK, false)
                    'D' -> CheckerPiece(CheckerSide.DARK, true)
                    else -> null
                }
            }
        }
        CheckersState(
            board = board,
            turn = CheckerSide.valueOf(obj.getString("turn")),
            forcedCapture = obj.getBoolean("forced"),
            moveCount = obj.getInt("moves")
        )
    }.getOrNull()
}
