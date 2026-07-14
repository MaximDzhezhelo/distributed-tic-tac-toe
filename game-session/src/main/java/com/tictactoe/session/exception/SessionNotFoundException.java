package com.tictactoe.session.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String sessionId) {
        super("Session '%s' was not found".formatted(sessionId));
    }
}
