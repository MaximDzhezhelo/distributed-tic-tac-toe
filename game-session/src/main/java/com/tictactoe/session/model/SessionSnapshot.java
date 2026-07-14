package com.tictactoe.session.model;

import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;

import java.time.Instant;
import java.util.List;

/**
 * Immutable view of a session, returned by the API and pushed over SSE.
 */
public record SessionSnapshot(
        String sessionId,
        SessionStatus status,
        GameStatus gameStatus,
        Player winner,
        Player nextPlayer,
        String[] board,
        List<Move> moves,
        String error,
        Instant createdAt
) {
}
