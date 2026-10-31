package com.yara.ai.games
class TicTacToeGame {
    var board = MutableList(9) { 0 }
    var over = false

    fun move(i: Int, p: Int): Boolean {
        if (i !in 0..8 || board[i] != 0 || over) return false
        board[i] = p
        over = winner() != 0 || board.all { it != 0 }
        return true
    }

    fun winner(): Int {
        val lines = listOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (line in lines) {
            val a = line[0]; val b = line[1]; val c = line[2]
            if (board[a] != 0 && board[a] == board[b] && board[a] == board[c]) return board[a]
        }
        return 0
    }

    fun ai(): Int {
        val free = board.indices.filter { board[it] == 0 }
        var best = -1
        var score = Int.MIN_VALUE
        for (i in free) {
            board[i] = 2
            val s = minimax(false)
            board[i] = 0
            if (s > score) { score = s; best = i }
        }
        if (best >= 0) move(best, 2)
        return best
    }

    private fun minimax(max: Boolean): Int {
        val w = winner()
        if (w == 2) return 1
        if (w == 1) return -1
        if (board.all { it != 0 }) return 0
        var v = if (max) Int.MIN_VALUE else Int.MAX_VALUE
        for (i in board.indices) {
            if (board[i] == 0) {
                board[i] = if (max) 2 else 1
                v = if (max) kotlin.math.max(v, minimax(false)) else kotlin.math.min(v, minimax(true))
                board[i] = 0
            }
        }
        return v
    }
}
