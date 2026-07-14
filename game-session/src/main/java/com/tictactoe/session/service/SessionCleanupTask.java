package com.tictactoe.session.service;

import com.tictactoe.session.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Evicts finished sessions after a retention period so the in-memory store
 * cannot grow without bound.
 */
@Component
public class SessionCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupTask.class);

    private final SessionRepository sessionRepository;
    private final Duration retention;

    public SessionCleanupTask(SessionRepository sessionRepository,
                              @Value("${cleanup.retention:PT1H}") Duration retention) {
        this.sessionRepository = sessionRepository;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${cleanup.interval:PT5M}")
    public void removeExpiredSessions() {
        var removed = sessionRepository.deleteFinishedBefore(Instant.now().minus(retention));
        if (removed > 0) {
            log.info("Evicted {} finished sessions older than {}", removed, retention);
        }
    }
}
