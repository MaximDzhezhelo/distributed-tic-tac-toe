package com.tictactoe.session.web;

import com.tictactoe.common.validation.ValidId;
import com.tictactoe.session.events.SessionEventPublisher;
import com.tictactoe.session.model.SessionSnapshot;
import com.tictactoe.session.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;

/**
 * REST API of the Game Session Service.
 *
 * <p>Manages game sessions and automates the gameplay: it generates moves for both
 * players and forwards them to the Game Engine Service, recording the move history
 * along the way. Errors are returned as RFC 7807 {@code application/problem+json}
 * responses; engine communication failures surface as {@code 502 Bad Gateway}.</p>
 */
@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final SessionEventPublisher eventPublisher;

    public SessionController(SessionService sessionService, SessionEventPublisher eventPublisher) {
        this.sessionService = sessionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new game session.
     * Generates a unique sessionId (which also serves as the gameId in the Game
     * Engine Service) and initializes the game state in the engine.
     */
    @PostMapping
    public ResponseEntity<SessionSnapshot> createSession() {
        var snapshot = sessionService.createSession();
        return ResponseEntity
                .created(URI.create("/sessions/" + snapshot.sessionId()))
                .body(snapshot);
    }

    /**
     * Trigger the automated simulation of a game.
     * Runs asynchronously: moves are generated for both players (alternating,
     * X first) and forwarded to the Game Engine Service until the game concludes
     * with a win or draw. Progress can be observed via {@code GET /sessions/{sessionId}}
     * or the SSE stream.
     */
    @PostMapping("/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SessionSnapshot simulate(@PathVariable @ValidId String sessionId) {
        return sessionService.startSimulation(sessionId);
    }

    /**
     * Retrieve session details, including the current game state and
     * the full move history.
     */
    @GetMapping("/{sessionId}")
    public SessionSnapshot getSession(@PathVariable @ValidId String sessionId) {
        return sessionService.getSession(sessionId);
    }

    /**
     * Stream live session updates to the UI over Server-Sent Events.
     * The current snapshot is sent immediately on subscription, then one event per
     * move until the game concludes.
     */
    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable @ValidId String sessionId) {
        var emitter = eventPublisher.subscribe(sessionId, sessionService.getSession(sessionId));
        // the simulation may finish concurrently with the subscription, in which
        // case its final publish/complete can miss the just-registered emitter;
        // re-checking after registration closes that window (both calls are no-ops
        // when the simulation's own completion already handled the stream)
        var latest = sessionService.getSession(sessionId);
        if (latest.status().isTerminal()) {
            eventPublisher.publish(latest);
            eventPublisher.complete(sessionId);
        }
        return emitter;
    }

}
