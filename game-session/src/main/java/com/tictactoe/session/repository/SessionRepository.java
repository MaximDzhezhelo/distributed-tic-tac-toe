package com.tictactoe.session.repository;

import com.tictactoe.session.model.Session;

import java.time.Instant;
import java.util.Optional;

/**
 * Storage abstraction for sessions. The in-memory implementation can be swapped
 * for a persistent one (JPA, Redis, ...) without touching the service layer.
 */
public interface SessionRepository {

    void save(Session session);

    Optional<Session> findById(String sessionId);

    /**
     * Removes sessions finished before the cutoff so the store cannot grow without bound.
     *
     * @return the number of removed sessions
     */
    int deleteFinishedBefore(Instant cutoff);

}
