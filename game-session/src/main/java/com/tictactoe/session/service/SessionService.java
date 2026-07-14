package com.tictactoe.session.service;

import com.tictactoe.common.logging.Correlation;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.events.SessionEventPublisher;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.model.Session;
import com.tictactoe.session.model.SessionSnapshot;
import com.tictactoe.session.repository.SessionRepository;
import com.tictactoe.session.service.strategy.MoveStrategy;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final ExecutorService simulationExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final SessionRepository sessionRepository;
    private final GameEngineClient engineClient;
    private final MoveStrategy moveStrategy;
    private final SessionEventPublisher eventPublisher;
    private final long moveDelayMs;

    public SessionService(SessionRepository sessionRepository,
                          GameEngineClient engineClient,
                          MoveStrategy moveStrategy,
                          SessionEventPublisher eventPublisher,
                          @Value("${simulation.move-delay-ms:400}") long moveDelayMs) {
        this.sessionRepository = sessionRepository;
        this.engineClient = engineClient;
        this.moveStrategy = moveStrategy;
        this.eventPublisher = eventPublisher;
        this.moveDelayMs = moveDelayMs;
    }

    /**
     * Creates a session and initializes the matching game in the engine.
     * The sessionId doubles as the engine's gameId.
     */
    public SessionSnapshot createSession() {
        var sessionId = UUID.randomUUID().toString();
        MDC.put(Correlation.GAME_ID, sessionId);
        engineClient.createGame(sessionId);
        var session = new Session(sessionId);
        sessionRepository.save(session);
        log.info("Created session");
        return session.snapshot();
    }

    public SessionSnapshot getSession(String sessionId) {
        return find(sessionId).snapshot();
    }

    /**
     * Starts the automated simulation on a background thread and returns
     * immediately; progress is observable via GET / SSE.
     */
    public SessionSnapshot startSimulation(String sessionId) {
        var session = find(sessionId);
        session.markRunning();
        simulationExecutor.submit(() -> runSimulation(session));
        log.info("Started simulation");
        return session.snapshot();
    }

    private void runSimulation(Session session) {
        var sessionId = session.getId();
        // the simulation is not an HTTP request, so it gets its own correlation id;
        // the engine client propagates it to every move request it sends
        MDC.put(Correlation.CID, Correlation.newId());
        MDC.put(Correlation.GAME_ID, sessionId);
        try {
            var state = engineClient.getGame(sessionId);
            while (state.status() == GameStatus.IN_PROGRESS) {
                var player = state.nextPlayer();
                var position = moveStrategy.nextMove(state.board(), player);
                state = engineClient.move(sessionId, player, position);
                session.recordMove(player, position, state);
                log.debug("{} took cell {} ({})", player, position, state.status());
                eventPublisher.publish(session.snapshot());
                if (state.status() == GameStatus.IN_PROGRESS && moveDelayMs > 0) {
                    Thread.sleep(moveDelayMs);
                }
            }
            session.finish(state);
            log.info("Simulation finished: {} (winner: {})", state.status(), state.winner());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            session.fail("Simulation was interrupted");
        } catch (Exception ex) {
            log.error("Simulation failed", ex);
            session.fail(ex.getMessage());
        } finally {
            eventPublisher.publish(session.snapshot());
            eventPublisher.complete(sessionId);
            MDC.clear();
        }
    }

    private Session find(String sessionId) {
        MDC.put(Correlation.GAME_ID, sessionId);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @PreDestroy
    void shutdown() {
        simulationExecutor.shutdown();
        try {
            if (!simulationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Simulations still running after 10s, forcing shutdown");
                simulationExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            simulationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
