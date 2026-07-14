package com.tictactoe.engine.web;

import com.tictactoe.common.logging.Correlation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@AutoConfigureTestRestTemplate
class GameApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void playsFullGameThroughRestApi() {
        var gameId = createGame();

        move(gameId, "X", 0);
        move(gameId, "O", 3);
        move(gameId, "X", 1);
        move(gameId, "O", 4);
        var winning = move(gameId, "X", 2);

        assertThat(winning.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(winning.getBody().get("status")).isEqualTo("WON");
        assertThat(winning.getBody().get("winner")).isEqualTo("X");

        var state = rest.getForEntity("/games/" + gameId, Map.class);
        assertThat(state.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(state.getBody().get("status")).isEqualTo("WON");
        assertThat((java.util.List<String>) state.getBody().get("board"))
                .containsExactly("X", "X", "X", "O", "O", null, null, null, null);
    }

    @Test
    void rejectsMoveToOccupiedCellWithConflict() {
        var gameId = createGame();
        move(gameId, "X", 4);

        var response = move(gameId, "O", 4);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("detail")).contains("occupied");
    }

    @Test
    void rejectsMoveOutOfTurnWithConflict() {
        var gameId = createGame();

        var response = move(gameId, "O", 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsInvalidPositionWithBadRequest() {
        var gameId = createGame();

        var response = move(gameId, "X", 9);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvalidPlayerSymbolWithBadRequest() {
        var gameId = createGame();

        var response = rest.postForEntity(
                "/games/" + gameId + "/move",
                Map.of("player", "Z", "position", 0),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvalidGameIdOnCreateWithBadRequest() {
        var response = rest.postForEntity("/games", Map.of("gameId", "bad id with spaces!"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("detail")).contains("gameId");
    }

    @Test
    void rejectsOverlongGameIdWithBadRequest() {
        var response = rest.postForEntity("/games", Map.of("gameId", "x".repeat(65)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvalidGameIdInPathWithBadRequest() {
        var response = rest.getForEntity("/games/{gameId}", Map.class, Map.of("gameId", "not valid!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void echoesProvidedCorrelationIdAndGeneratesOneOtherwise() {
        var headers = new HttpHeaders();
        headers.set(Correlation.HEADER, "test-cid-123");
        var withCid = rest.exchange("/games", HttpMethod.POST,
                new HttpEntity<>(Map.of("gameId", UUID.randomUUID().toString()), headers), Map.class);
        assertThat(withCid.getHeaders().getFirst(Correlation.HEADER)).isEqualTo("test-cid-123");

        var withoutCid = rest.postForEntity("/games",
                Map.of("gameId", UUID.randomUUID().toString()), Map.class);
        assertThat(withoutCid.getHeaders().getFirst(Correlation.HEADER)).isNotBlank();
    }

    @Test
    void returnsNotFoundForUnknownGame() {
        var response = rest.getForEntity("/games/does-not-exist", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsMoveAfterGameEnded() {
        var gameId = createGame();
        move(gameId, "X", 0);
        move(gameId, "O", 3);
        move(gameId, "X", 1);
        move(gameId, "O", 4);
        move(gameId, "X", 2);

        var response = move(gameId, "O", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("detail")).contains("finished");
    }

    private String createGame() {
        var gameId = UUID.randomUUID().toString();
        var response = rest.postForEntity("/games", Map.of("gameId", gameId), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return gameId;
    }

    private ResponseEntity<Map> move(String gameId, String player, int position) {
        return rest.postForEntity(
                "/games/" + gameId + "/move",
                Map.of("player", player, "position", position),
                Map.class);
    }
}
