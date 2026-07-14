package com.tictactoe.engine.service;

import com.tictactoe.common.logging.Correlation;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.engine.exception.GameAlreadyExistsException;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.model.Game;
import com.tictactoe.engine.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Creates a new game with an empty board and X to move first.
     */
    public GameState create(String requestedGameId) {
        var gameId = StringUtils.hasText(requestedGameId)
                ? requestedGameId
                : UUID.randomUUID().toString();
        MDC.put(Correlation.GAME_ID, gameId);
        var game = new Game(gameId);
        if (!gameRepository.saveIfAbsent(game)) {
            throw new GameAlreadyExistsException(gameId);
        }
        log.info("Created game");
        return game.snapshot();
    }

    /**
     * Returns the current state of a game.
     */
    public GameState get(String gameId) {
        return find(gameId)
                .snapshot();
    }

    /**
     * Applies a move to a game: validates it, updates the board, and evaluates the outcome.
     */
    public GameState move(String gameId, Player player, int position) {
        var state = find(gameId)
                .applyMove(player, position);
        if (state.status() == GameStatus.IN_PROGRESS) {
            log.debug("{} took cell {}, {} to move", player, position, state.nextPlayer());
        } else {
            log.info("{} took cell {}, game over: {} (winner: {})", player, position, state.status(), state.winner());
        }
        return state;
    }

    private Game find(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

}
