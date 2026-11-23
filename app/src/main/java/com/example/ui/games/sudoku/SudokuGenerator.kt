package com.example.ui.games.sudoku

import kotlin.random.Random

data class KillerCage(
    val id: Int,
    val cells: List<Pair<Int, Int>>,
    val targetSum: Int
)

data class GeneratedSudoku(
    val puzzle: List<List<Int>>, // 0 = empty
    val solution: List<List<Int>>,
    val cages: List<KillerCage> = emptyList()
)

object SudokuGenerator {

    fun generate(difficulty: String, isKiller: Boolean = false): GeneratedSudoku {
        val solved = Array(9) { IntArray(9) }
        fillGrid(solved, Random(System.currentTimeMillis()))

        val solution = solved.map { it.toList() }
        if (isKiller) {
            val cages = buildKillerCages(solution)
            val prefillCount = if (difficulty.contains("Challenge", ignoreCase = true)) 6 else 18
            val puzzle = MutableList(9) { MutableList(9) { 0 } }
            val coords = (0..80).shuffled().take(prefillCount)
            coords.forEach { idx ->
                val r = idx / 9; val c = idx % 9
                puzzle[r][c] = solution[r][c]
            }
            return GeneratedSudoku(puzzle.map { it.toList() }, solution, cages)
        }

        val targetClues = when {
            difficulty.equals("Easy", ignoreCase = true) -> 44
            difficulty.equals("Medium", ignoreCase = true) -> 35
            difficulty.equals("Hard", ignoreCase = true) -> 28
            else -> 23 // Expert
        }

        val puzzle = Array(9) { r -> solved[r].copyOf() }
        val positions = (0..80).shuffled().toMutableList()
        var remainingClues = 81

        for (pos in positions) {
            if (remainingClues <= targetClues) break
            val r = pos / 9
            val c = pos % 9
            val backup = puzzle[r][c]
            puzzle[r][c] = 0
            if (countSolutions(puzzle, limit = 2) == 1) {
                remainingClues--
            } else {
                puzzle[r][c] = backup
            }
        }
        return GeneratedSudoku(
            puzzle = puzzle.map { it.toList() },
            solution = solution
        )
    }

    private fun fillGrid(grid: Array<IntArray>, rng: Random): Boolean {
        for (r in 0..8) {
            for (c in 0..8) {
                if (grid[r][c] == 0) {
                    val nums = (1..9).shuffled(rng)
                    for (n in nums) {
                        if (isValid(grid, r, c, n)) {
                            grid[r][c] = n
                            if (fillGrid(grid, rng)) return true
                            grid[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun countSolutions(grid: Array<IntArray>, limit: Int): Int {
        var bestR = -1
        var bestC = -1
        var minCandidates = 10
        for (r in 0..8) {
            for (c in 0..8) {
                if (grid[r][c] == 0) {
                    var count = 0
                    for (n in 1..9) if (isValid(grid, r, c, n)) count++
                    if (count < minCandidates) {
                        minCandidates = count
                        bestR = r; bestC = c
                    }
                }
            }
        }
        if (bestR == -1) return 1
        if (minCandidates == 0) return 0

        var solutions = 0
        for (n in 1..9) {
            if (isValid(grid, bestR, bestC, n)) {
                grid[bestR][bestC] = n
                solutions += countSolutions(grid, limit - solutions)
                grid[bestR][bestC] = 0
                if (solutions >= limit) break
            }
        }
        return solutions
    }

    private fun isValid(grid: Array<IntArray>, r: Int, c: Int, n: Int): Boolean {
        for (i in 0..8) {
            if (grid[r][i] == n || grid[i][c] == n) return false
        }
        val br = (r / 3) * 3
        val bc = (c / 3) * 3
        for (dr in 0..2) {
            for (dc in 0..2) {
                if (grid[br + dr][bc + dc] == n) return false
            }
        }
        return true
    }

    private fun buildKillerCages(solution: List<List<Int>>): List<KillerCage> {
        val visited = Array(9) { BooleanArray(9) }
        val cages = mutableListOf<KillerCage>()
        var id = 0
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for (r in 0..8) {
            for (c in 0..8) {
                if (visited[r][c]) continue
                val targetSize = (2..4).random()
                val cells = mutableListOf(r to c)
                visited[r][c] = true
                val usedDigits = mutableSetOf(solution[r][c])
                while (cells.size < targetSize) {
                    val neighbors = cells.flatMap { (cr, cc) ->
                        dirs.map { (dr, dc) -> (cr + dr) to (cc + dc) }
                    }.filter { (nr, nc) ->
                        nr in 0..8 && nc in 0..8 && !visited[nr][nc] && solution[nr][nc] !in usedDigits
                    }.distinct()
                    if (neighbors.isEmpty()) break
                    val pick = neighbors.random()
                    visited[pick.first][pick.second] = true
                    usedDigits.add(solution[pick.first][pick.second])
                    cells.add(pick)
                }
                val sum = cells.sumOf { (cr, cc) -> solution[cr][cc] }
                cages.add(KillerCage(id = id++, cells = cells, targetSum = sum))
            }
        }
        return cages
    }
}
