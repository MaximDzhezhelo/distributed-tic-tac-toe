package com.tictactoe.engine.exception;

public class GameAlreadyExistsException extends RuntimeException {

    public GameAlreadyExistsException(String gameId) {
        super("Game '%s' already exists".formatted(gameId));
    }
}
