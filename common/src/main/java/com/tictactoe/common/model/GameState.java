package com.tictactoe.common.model;

/**
 * Game state as exposed by the Game Engine Service — the wire contract
 * between the services.
 *
 * @param gameId     unique id of the game
 * @param board      9 cells, "X", "O" or null (empty), indexed 0-8 left-to-right, top-to-bottom
 * @param status     whether the game is in progress, won or drawn
 * @param winner     the winning player, or null unless status is {@link GameStatus#WON}
 * @param nextPlayer the player whose turn it is, or null once the game is over
 */
public record GameState(
        String gameId,
        String[] board,
        GameStatus status,
        Player winner,
        Player nextPlayer
) {
}
