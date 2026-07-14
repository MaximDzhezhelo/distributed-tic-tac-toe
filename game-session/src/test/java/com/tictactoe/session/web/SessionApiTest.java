package com.tictactoe.session.web;

import com.tictactoe.session.FakeGameEngine;
import com.tictactoe.session.client.GameEngineClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "simulation.move-delay-ms=0",
                "eureka.client.enabled=false",
                "engine.load-balanced=false"
        })
@AutoConfigureTestRestTemplate
class SessionApiTest {

    @TestConfiguration
    static class FakeEngineConfig {
        @Bean
        @Primary
        FakeGameEngine fakeGameEngine() {
            return new FakeGameEngine();
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private GameEngineClient engineClient;

    @BeforeEach
    void resetFakeEngine() {
        ((FakeGameEngine) engineClient).reset();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsSessionAndSimulatesFullGame() {
        var created = rest.postForEntity("/sessions", null, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var sessionId = (String) created.getBody().get("sessionId");
        assertThat(sessionId).isNotBlank();

        var simulate =
                rest.postForEntity("/sessions/" + sessionId + "/simulate", null, Map.class);
        assertThat(simulate.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        var session = awaitTerminalState(sessionId);

        assertThat(session.get("status")).isEqualTo("FINISHED");
        assertThat(session.get("gameStatus")).isIn("WON", "DRAW");

        var moves = (List<Map<String, Object>>) session.get("moves");
        assertThat(moves).hasSizeBetween(5, 9);
        for (int i = 0; i < moves.size(); i++) {
            assertThat(moves.get(i).get("player")).isEqualTo(i % 2 == 0 ? "X" : "O");
        }
    }

    @Test
    void marksSessionFailedWhenEngineIsUnavailable() {
        var sessionId = (String) rest.postForEntity("/sessions", null, Map.class)
                .getBody().get("sessionId");

        ((FakeGameEngine) engineClient).failAllMoves();
        rest.postForEntity("/sessions/" + sessionId + "/simulate", null, Map.class);

        var session = awaitTerminalState(sessionId);

        assertThat(session.get("status")).isEqualTo("FAILED");
        assertThat((String) session.get("error")).contains("unreachable");
    }

    @Test
    void rejectsInvalidSessionIdWithBadRequest() {
        var response = rest.getForEntity("/sessions/{sessionId}", Map.class, Map.of("sessionId", "not valid!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("detail")).contains("sessionId");
    }

    @Test
    void returnsNotFoundForUnknownSession() {
        var response = rest.getForEntity("/sessions/unknown", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsSimulationForUnknownSession() {
        var response =
                rest.postForEntity("/sessions/unknown/simulate", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Map awaitTerminalState(String sessionId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(
                        () -> rest.getForEntity("/sessions/" + sessionId, Map.class).getBody(),
                        body -> "FINISHED".equals(body.get("status")) || "FAILED".equals(body.get("status")));
    }
}
