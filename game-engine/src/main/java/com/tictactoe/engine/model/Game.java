package com.tictactoe.engine.model;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.engine.exception.IllegalMoveException;

import java.time.Instant;

/**
 * Mutable game aggregate. All state transitions are synchronized, so concurrent
 * move requests against the same game are applied one at a time.
 */
public class Game {

    private final String id;
    private final String[] board = new String[GameRules.BOARD_SIZE];
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Player nextPlayer = Player.X;
    private Instant finishedAt;
    private Player winner;

    public Game(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public synchronized GameState applyMove(Player player, int position) {
        if (position < 0 || position >= GameRules.BOARD_SIZE) {
            throw new IllegalMoveException(
                    "Position %d is out of bounds (must be 0-%d)".formatted(position, GameRules.BOARD_SIZE - 1));
        }
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalMoveException(
                    "Game '%s' is already finished with status %s".formatted(id, status));
        }
        if (player != nextPlayer) {
            throw new IllegalMoveException(
                    "It is %s's turn, but %s tried to move".formatted(nextPlayer, player));
        }
        if (board[position] != null) {
            throw new IllegalMoveException(
                    "Cell %d is already occupied by %s".formatted(position, board[position]));
        }

        board[position] = player.name();

        if (GameRules.hasWon(board, player)) {
            status = GameStatus.WON;
            winner = player;
            nextPlayer = null;
        } else if (GameRules.isBoardFull(board)) {
            status = GameStatus.DRAW;
            nextPlayer = null;
        } else {
            nextPlayer = player.other();
        }
        if (status != GameStatus.IN_PROGRESS) {
            finishedAt = Instant.now();
        }
        return snapshot();
    }

    public synchronized GameState snapshot() {
        return new GameState(id, board.clone(), status, winner, nextPlayer);
    }

    /**
     * True when the game reached a terminal state before the given cutoff —
     * used by the retention cleanup.
     */
    public synchronized boolean isFinishedBefore(Instant cutoff) {
        return finishedAt != null && finishedAt.isBefore(cutoff);
    }

}
