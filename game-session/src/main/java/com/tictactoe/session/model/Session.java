package com.tictactoe.session.model;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.session.exception.SimulationConflictException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable session aggregate. State transitions are synchronized because the
 * simulation runs on a background thread while the API reads snapshots.
 */
public class Session {

    private final String id;
    private final Instant createdAt = Instant.now();
    private final List<Move> moves = new ArrayList<>();

    private SessionStatus status = SessionStatus.CREATED;
    private Instant finishedAt;
    private String[] board = new String[GameRules.BOARD_SIZE];
    private GameStatus gameStatus = GameStatus.IN_PROGRESS;
    private Player winner;
    private Player nextPlayer = Player.X;
    private String error;

    public Session(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public synchronized void markRunning() {
        if (status == SessionStatus.RUNNING) {
            throw new SimulationConflictException(
                    "Simulation is already running for session '%s'".formatted(id));
        }
        if (status != SessionStatus.CREATED) {
            throw new SimulationConflictException(
                    "Session '%s' has already finished with status %s".formatted(id, status));
        }
        status = SessionStatus.RUNNING;
    }

    public synchronized void recordMove(Player player, int position, GameState state) {
        moves.add(new Move(moves.size() + 1, player, position, state.status(), Instant.now()));
        applyEngineState(state);
    }

    public synchronized void finish(GameState finalState) {
        applyEngineState(finalState);
        status = SessionStatus.FINISHED;
        finishedAt = Instant.now();
    }

    public synchronized void fail(String message) {
        status = SessionStatus.FAILED;
        error = message;
        finishedAt = Instant.now();
    }

    /**
     * True when the session reached a terminal state before the given cutoff —
     * used by the retention cleanup.
     */
    public synchronized boolean isFinishedBefore(Instant cutoff) {
        return finishedAt != null && finishedAt.isBefore(cutoff);
    }

    public synchronized SessionSnapshot snapshot() {
        return new SessionSnapshot(id, status, gameStatus, winner, nextPlayer,
                board.clone(), List.copyOf(moves), error, createdAt);
    }

    private void applyEngineState(GameState state) {
        board = state.board().clone();
        gameStatus = state.status();
        winner = state.winner();
        nextPlayer = state.nextPlayer();
    }
}
