package com.tictactoe.session.client;

import com.tictactoe.common.api.CreateGameRequest;
import com.tictactoe.common.api.MoveRequest;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class RestGameEngineClient implements GameEngineClient {

    private static final Logger log = LoggerFactory.getLogger(RestGameEngineClient.class);

    private final RestClient restClient;

    public RestGameEngineClient(RestClient engineRestClient) {
        this.restClient = engineRestClient;
    }

    @Override
    public void createGame(String gameId) {
        execute(
                "create game",
                () -> restClient.post()
                        .uri("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new CreateGameRequest(gameId))
                        .retrieve()
                        .body(GameState.class)
        );
    }

    @Override
    public GameState getGame(String gameId) {
        return execute(
                "fetch game",
                () -> restClient.get()
                        .uri("/games/{gameId}", gameId)
                        .retrieve()
                        .body(GameState.class)
        );
    }

    @Override
    public GameState move(String gameId, Player player, int position) {
        return execute(
                "move %s to cell %d".formatted(player, position),
                () -> restClient.post()
                        .uri("/games/{gameId}/move", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new MoveRequest(player, position))
                        .retrieve()
                        .body(GameState.class)
        );
    }

    private <T> T execute(String action, Supplier<T> call) {
        log.debug("Calling game engine to {}", action);
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            log.warn("Game engine rejected request to {}: HTTP {}", action, ex.getStatusCode().value());
            throw new EngineClientException(
                    "Game engine rejected request to %s: %s".formatted(action, extractDetail(ex)), ex);
        } catch (RestClientException ex) {
            log.warn("Game engine unreachable while trying to {}: {}", action, ex.getMessage());
            throw new EngineClientException(
                    "Game engine is unreachable while trying to %s".formatted(action), ex);
        }
    }

    private String extractDetail(RestClientResponseException ex) {
        try {
            var problem = ex.getResponseBodyAs(org.springframework.http.ProblemDetail.class);
            if (problem != null && problem.getDetail() != null) {
                return problem.getDetail();
            }
        } catch (Exception ignored) {
            // fall through to the raw status text
        }
        return ex.getStatusText();
    }

}
