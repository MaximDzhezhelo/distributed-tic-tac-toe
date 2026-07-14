package com.tictactoe.engine.exception;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String gameId) {
        super("Game '%s' was not found".formatted(gameId));
    }
}
