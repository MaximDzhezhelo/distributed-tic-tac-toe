package com.tictactoe.engine.web;

import com.tictactoe.common.api.CreateGameRequest;
import com.tictactoe.common.api.MoveRequest;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.validation.ValidId;
import com.tictactoe.engine.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST API of the Game Engine Service — the single source of truth for game state.
 *
 * <p>Maintains the board of every game, validates each move and reports the game
 * outcome.</p>
 */
@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Create a new game with an empty 3x3 board, X to move first.
     */
    @PostMapping
    public ResponseEntity<GameState> create(@Valid @RequestBody(required = false) CreateGameRequest request) {
        var state = gameService.create(request == null ? null : request.gameId());
        return ResponseEntity
                .created(URI.create("/games/" + state.gameId()))
                .body(state);
    }

    /**
     * Retrieve the current state of the game (board and status).
     */
    @GetMapping("/{gameId}")
    public GameState get(@PathVariable @ValidId String gameId) {
        return gameService.get(gameId);
    }

    /**
     * Receive a move request containing the player symbol and board position.
     */
    @PostMapping("/{gameId}/move")
    public GameState move(@PathVariable @ValidId String gameId, @Valid @RequestBody MoveRequest request) {
        return gameService.move(gameId, request.player(), request.position());
    }

}
