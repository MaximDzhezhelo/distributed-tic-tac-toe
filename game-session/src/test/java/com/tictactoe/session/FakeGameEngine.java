package com.tictactoe.session;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.session.client.EngineClientException;
import com.tictactoe.session.client.GameEngineClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory stand-in for the Game Engine Service. Applies moves to a real
 * board using the shared game rules, so the session service can be tested
 * without HTTP.
 */
public class FakeGameEngine implements GameEngineClient {

    private final Map<String, String[]> boards = new ConcurrentHashMap<>();
    private final Map<String, GameState> lastStates = new ConcurrentHashMap<>();
    private final AtomicBoolean failMoves = new AtomicBoolean(false);

    public void failAllMoves() {
        failMoves.set(true);
    }

    public void reset() {
        failMoves.set(false);
    }

    @Override
    public void createGame(String gameId) {
        boards.put(gameId, new String[GameRules.BOARD_SIZE]);
        var state = new GameState(gameId, new String[GameRules.BOARD_SIZE],
                GameStatus.IN_PROGRESS, null, Player.X);
        lastStates.put(gameId, state);
    }

    @Override
    public GameState getGame(String gameId) {
        return lastStates.get(gameId);
    }

    @Override
    public GameState move(String gameId, Player player, int position) {
        if (failMoves.get()) {
            throw new EngineClientException("Game engine is unreachable", null);
        }
        var board = boards.get(gameId);
        if (board[position] != null) {
            throw new EngineClientException("Cell " + position + " is already occupied", null);
        }
        board[position] = player.name();

        var status = GameStatus.IN_PROGRESS;
        Player winner = null;
        var nextPlayer = player.other();
        if (GameRules.hasWon(board, player)) {
            status = GameStatus.WON;
            winner = player;
            nextPlayer = null;
        } else if (GameRules.isBoardFull(board)) {
            status = GameStatus.DRAW;
            nextPlayer = null;
        }

        var state = new GameState(gameId, board.clone(), status, winner, nextPlayer);
        lastStates.put(gameId, state);
        return state;
    }

}
