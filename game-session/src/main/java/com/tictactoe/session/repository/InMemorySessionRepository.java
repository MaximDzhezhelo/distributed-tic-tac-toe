package com.tictactoe.session.repository;

import com.tictactoe.session.model.Session;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(Session session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public int deleteFinishedBefore(Instant cutoff) {
        var removed = new AtomicInteger();
        sessions.values().removeIf(session -> {
            if (session.isFinishedBefore(cutoff)) {
                removed.incrementAndGet();
                return true;
            }
            return false;
        });
        return removed.get();
    }

}
