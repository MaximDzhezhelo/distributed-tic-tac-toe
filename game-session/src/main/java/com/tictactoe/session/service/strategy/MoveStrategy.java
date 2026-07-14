package com.tictactoe.session.service.strategy;

import com.tictactoe.common.model.Player;

/**
 * Picks the next move for a player given the current board
 * (cells "X", "O" or null, indexed 0-8).
 */
public interface MoveStrategy {

    int nextMove(String[] board, Player player);
}
