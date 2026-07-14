package com.tictactoe.engine.service;

import com.tictactoe.engine.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Evicts finished games after a retention period, so the in-memory store
 * cannot grow without a bound.
 */
@Component
public class GameCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(GameCleanupTask.class);

    private final GameRepository gameRepository;
    private final Duration retention;

    public GameCleanupTask(GameRepository gameRepository,
                           @Value("${cleanup.retention:PT1H}") Duration retention) {
        this.gameRepository = gameRepository;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${cleanup.interval:PT5M}")
    public void removeExpiredGames() {
        var removed = gameRepository.deleteFinishedBefore(Instant.now().minus(retention));
        if (removed > 0) {
            log.info("Evicted {} finished games older than {}", removed, retention);
        }
    }
}
