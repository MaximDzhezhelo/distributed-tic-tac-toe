package com.tictactoe.engine.service;

import com.tictactoe.engine.exception.GameAlreadyExistsException;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.exception.IllegalMoveException;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.engine.repository.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService(new InMemoryGameRepository());
        service.create("game-1");
    }

    @Test
    void createsGameWithEmptyBoardAndXToMove() {
        var state = service.get("game-1");

        assertThat(state.board()).containsOnlyNulls();
        assertThat(state.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(state.nextPlayer()).isEqualTo(Player.X);
        assertThat(state.winner()).isNull();
    }

    @Test
    void createGeneratesIdWhenNoneProvided() {
        var state = service.create(null);

        assertThat(state.gameId()).isNotBlank();
    }

    @Test
    void rejectsDuplicateGameId() {
        assertThatThrownBy(() -> service.create("game-1"))
                .isInstanceOf(GameAlreadyExistsException.class);
    }

    @Test
    void moveUpdatesBoardAndAlternatesTurn() {
        var state = service.move("game-1", Player.X, 4);

        assertThat(state.board()[4]).isEqualTo("X");
        assertThat(state.nextPlayer()).isEqualTo(Player.O);
        assertThat(state.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void rejectsMoveToOccupiedCell() {
        service.move("game-1", Player.X, 4);

        assertThatThrownBy(() -> service.move("game-1", Player.O, 4))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("occupied");
    }

    @Test
    void rejectsMoveOutOfTurn() {
        assertThatThrownBy(() -> service.move("game-1", Player.O, 0))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("turn");
    }

    @Test
    void rejectsOutOfBoundsPosition() {
        assertThatThrownBy(() -> service.move("game-1", Player.X, 9))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("out of bounds");
        assertThatThrownBy(() -> service.move("game-1", Player.X, -1))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("out of bounds");
    }

    @Test
    void rejectsMoveOnUnknownGame() {
        assertThatThrownBy(() -> service.move("missing", Player.X, 0))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void detectsRowWin() {
        service.move("game-1", Player.X, 0);
        service.move("game-1", Player.O, 3);
        service.move("game-1", Player.X, 1);
        service.move("game-1", Player.O, 4);
        var state = service.move("game-1", Player.X, 2);

        assertThat(state.status()).isEqualTo(GameStatus.WON);
        assertThat(state.winner()).isEqualTo(Player.X);
        assertThat(state.nextPlayer()).isNull();
    }

    @Test
    void detectsColumnWin() {
        service.move("game-1", Player.X, 0);
        service.move("game-1", Player.O, 1);
        service.move("game-1", Player.X, 3);
        service.move("game-1", Player.O, 2);
        var state = service.move("game-1", Player.X, 6);

        assertThat(state.status()).isEqualTo(GameStatus.WON);
        assertThat(state.winner()).isEqualTo(Player.X);
    }

    @Test
    void detectsDiagonalWinForO() {
        service.move("game-1", Player.X, 1);
        service.move("game-1", Player.O, 0);
        service.move("game-1", Player.X, 3);
        service.move("game-1", Player.O, 4);
        service.move("game-1", Player.X, 5);
        var state = service.move("game-1", Player.O, 8);

        assertThat(state.status()).isEqualTo(GameStatus.WON);
        assertThat(state.winner()).isEqualTo(Player.O);
    }

    @Test
    void detectsDraw() {
        int[] xMoves = {0, 2, 3, 7, 8};
        int[] oMoves = {1, 4, 5, 6};
        for (int i = 0; i < oMoves.length; i++) {
            service.move("game-1", Player.X, xMoves[i]);
            service.move("game-1", Player.O, oMoves[i]);
        }
        var state = service.move("game-1", Player.X, xMoves[4]);

        assertThat(state.status()).isEqualTo(GameStatus.DRAW);
        assertThat(state.winner()).isNull();
        assertThat(state.board()).doesNotContainNull();
    }

    @Test
    void rejectsMoveAfterGameIsFinished() {
        service.move("game-1", Player.X, 0);
        service.move("game-1", Player.O, 3);
        service.move("game-1", Player.X, 1);
        service.move("game-1", Player.O, 4);
        service.move("game-1", Player.X, 2);

        assertThatThrownBy(() -> service.move("game-1", Player.O, 5))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("finished");
    }
}
