package com.tictactoe.integration;

import com.tictactoe.engine.EngineApplication;
import com.tictactoe.session.SessionApplication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real Game Engine Service and Game Session Service in the same JVM
 * (on random ports) and exercises the full automated game flow over HTTP,
 * exactly as the two processes talk to each other in production.
 */
class FullGameFlowTest {

    private static ConfigurableApplicationContext engineContext;
    private static ConfigurableApplicationContext sessionContext;
    private static String engineUrl;
    private static String sessionUrl;
    private static final TestRestTemplate rest = new TestRestTemplate();

    @BeforeAll
    static void startServices() {
        engineContext = new SpringApplicationBuilder(EngineApplication.class)
                .run("--server.port=0",
                        "--spring.application.name=game-engine",
                        "--eureka.client.enabled=false");
        engineUrl = "http://localhost:" + port(engineContext);

        sessionContext = new SpringApplicationBuilder(SessionApplication.class)
                .run("--server.port=0",
                        "--spring.application.name=game-session",
                        "--eureka.client.enabled=false",
                        "--engine.load-balanced=false",
                        "--engine.base-url=" + engineUrl,
                        "--simulation.move-delay-ms=0");
        sessionUrl = "http://localhost:" + port(sessionContext);
    }

    @AfterAll
    static void stopServices() {
        if (sessionContext != null) {
            sessionContext.close();
        }
        if (engineContext != null) {
            engineContext.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulatesFullGameAcrossBothServices() {
        var created = rest.postForEntity(sessionUrl + "/sessions", null, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var sessionId = (String) created.getBody().get("sessionId");

        var simulate =
                rest.postForEntity(sessionUrl + "/sessions/" + sessionId + "/simulate", null, Map.class);
        assertThat(simulate.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        var session = Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(
                        () -> rest.getForEntity(sessionUrl + "/sessions/" + sessionId, Map.class).getBody(),
                        body -> "FINISHED".equals(body.get("status")) || "FAILED".equals(body.get("status")));

        assertThat(session.get("status")).isEqualTo("FINISHED");
        assertThat(session.get("gameStatus")).isIn("WON", "DRAW");

        var moves = (List<Map<String, Object>>) session.get("moves");
        assertThat(moves).hasSizeBetween(5, 9);
        for (int i = 0; i < moves.size(); i++) {
            assertThat(moves.get(i).get("player"))
                    .as("move %d should alternate players starting with X", i + 1)
                    .isEqualTo(i % 2 == 0 ? "X" : "O");
        }

        // the engine (source of truth) must agree with the session's view
        var engineGame =
                rest.getForEntity(engineUrl + "/games/" + sessionId, Map.class);
        assertThat(engineGame.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(engineGame.getBody().get("status")).isEqualTo(session.get("gameStatus"));
        assertThat(engineGame.getBody().get("board")).isEqualTo(session.get("board"));

        // the board must equal an exact replay of the recorded move history
        var replayed = new String[9];
        for (var move : moves) {
            replayed[(Integer) move.get("position")] = (String) move.get("player");
        }
        assertThat((List<String>) session.get("board")).containsExactly(replayed);

        if ("WON".equals(session.get("gameStatus"))) {
            var lastMover = (String) moves.get(moves.size() - 1).get("player");
            assertThat(session.get("winner")).isEqualTo(lastMover);
        } else {
            assertThat(session.get("winner")).isNull();
            assertThat(moves).hasSize(9);
        }
    }

    @Test
    void engineRejectsInvalidMovesDuringConcurrentUse() {
        var created = rest.postForEntity(engineUrl + "/games", null, Map.class);
        var gameId = (String) created.getBody().get("gameId");

        rest.postForEntity(engineUrl + "/games/" + gameId + "/move",
                Map.of("player", "X", "position", 4), Map.class);

        var occupied = rest.postForEntity(engineUrl + "/games/" + gameId + "/move",
                Map.of("player", "O", "position", 4), Map.class);
        assertThat(occupied.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var wrongTurn = rest.postForEntity(engineUrl + "/games/" + gameId + "/move",
                Map.of("player", "X", "position", 0), Map.class);
        assertThat(wrongTurn.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sessionServiceReportsUnknownSessionAndDuplicateSimulation() {
        var unknown =
                rest.getForEntity(sessionUrl + "/sessions/does-not-exist", Map.class);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var sessionId = (String) rest.postForEntity(sessionUrl + "/sessions", null, Map.class)
                .getBody().get("sessionId");
        rest.postForEntity(sessionUrl + "/sessions/" + sessionId + "/simulate", null, Map.class);

        Awaitility.await().atMost(Duration.ofSeconds(15)).until(() ->
                "FINISHED".equals(rest.getForEntity(sessionUrl + "/sessions/" + sessionId, Map.class)
                        .getBody().get("status")));

        var again =
                rest.postForEntity(sessionUrl + "/sessions/" + sessionId + "/simulate", null, Map.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private static int port(ConfigurableApplicationContext context) {
        return Integer.parseInt(context.getEnvironment().getRequiredProperty("local.server.port"));
    }

}
