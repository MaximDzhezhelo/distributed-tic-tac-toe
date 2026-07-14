package com.tictactoe.session.model;

public enum SessionStatus {
    CREATED, RUNNING, FINISHED, FAILED;

    public boolean isTerminal() {
        return this == FINISHED || this == FAILED;
    }
}
