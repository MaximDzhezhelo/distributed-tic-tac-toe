package com.tictactoe.common.model;

/**
 * The rules of Tic Tac Toe shared by the engine (authoritative checks),
 * the session's move strategy and test fakes.
 */
public final class GameRules {

    public static final int BOARD_SIZE = 9;

    public static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private GameRules() {
    }

    public static boolean hasWon(String[] board, Player player) {
        var symbol = player.name();
        for (var line : WINNING_LINES) {
            if (symbol.equals(board[line[0]])
                    && symbol.equals(board[line[1]])
                    && symbol.equals(board[line[2]])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBoardFull(String[] board) {
        for (var cell : board) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }
}
