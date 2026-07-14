package com.tictactoe.session.client;

import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.Player;

/**
 * Abstraction over the Game Engine Service REST API.
 */
public interface GameEngineClient {

    void createGame(String gameId);

    GameState getGame(String gameId);

    GameState move(String gameId, Player player, int position);
}
