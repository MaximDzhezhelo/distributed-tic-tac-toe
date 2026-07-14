package com.tictactoe.session.events;

import com.tictactoe.session.model.SessionSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Fans out session updates to UI clients over Server-Sent Events.
 */
@Component
public class SessionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SessionEventPublisher.class);
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sessionId, SessionSnapshot current) {
        log.debug("New SSE subscriber (session status {})", current.status());
        var emitter = new SseEmitter(TIMEOUT_MS);
        var terminal = current.status().isTerminal();

        if (!terminal) {
            var sessionEmitters = emitters.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>());
            sessionEmitters.add(emitter);
            emitter.onCompletion(() -> sessionEmitters.remove(emitter));
            emitter.onTimeout(() -> sessionEmitters.remove(emitter));
            emitter.onError(ex -> sessionEmitters.remove(emitter));
        }

        send(emitter, current);
        if (terminal) {
            emitter.complete();
        }
        return emitter;
    }

    public void publish(SessionSnapshot snapshot) {
        var sessionEmitters = emitters.get(snapshot.sessionId());
        if (sessionEmitters == null) {
            return;
        }
        for (var emitter : sessionEmitters) {
            if (!send(emitter, snapshot)) {
                sessionEmitters.remove(emitter);
            }
        }
    }

    public void complete(String sessionId) {
        var sessionEmitters = emitters.remove(sessionId);
        if (sessionEmitters == null) {
            return;
        }
        sessionEmitters.forEach(SseEmitter::complete);
    }

    private boolean send(SseEmitter emitter, SessionSnapshot snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .name("update")
                    .data(snapshot, MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception ex) {
            log.debug("Dropping SSE client for session {}: {}", snapshot.sessionId(), ex.getMessage());
            return false;
        }
    }

}
