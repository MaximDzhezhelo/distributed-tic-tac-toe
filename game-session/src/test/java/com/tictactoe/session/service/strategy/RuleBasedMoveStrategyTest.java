package com.tictactoe.session.service.strategy;

import com.tictactoe.common.model.Player;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedMoveStrategyTest {

    private final RuleBasedMoveStrategy strategy = new RuleBasedMoveStrategy();

    @Test
    void takesWinningMoveWhenAvailable() {
        String[] board = {
                "X", "X", null,
                "O", "O", null,
                null, null, null
        };

        assertThat(strategy.nextMove(board, Player.X)).isEqualTo(2);
    }

    @Test
    void blocksOpponentWinningMove() {
        String[] board = {
                "O", "O", null,
                "X", null, null,
                null, null, null
        };

        assertThat(strategy.nextMove(board, Player.X)).isEqualTo(2);
    }

    @Test
    void prefersWinningOverBlocking() {
        String[] board = {
                "X", "X", null,
                "O", "O", null,
                null, null, null
        };

        // O can both win at 5 and block at 2; winning must take priority
        assertThat(strategy.nextMove(board, Player.O)).isEqualTo(5);
    }

    @Test
    void takesCenterOnEmptyBoard() {
        String[] board = new String[9];

        assertThat(strategy.nextMove(board, Player.X)).isEqualTo(4);
    }

    @Test
    void alwaysReturnsLegalMove() {
        String[] board = {
                "X", "O", "X",
                "O", "X", "O",
                null, null, null
        };

        for (int i = 0; i < 50; i++) {
            var move = strategy.nextMove(board, Player.O);
            assertThat(move).isBetween(6, 8);
        }
    }

    @Test
    void rejectsFullBoard() {
        String[] board = {
                "X", "O", "X",
                "O", "X", "O",
                "O", "X", "O"
        };

        assertThatThrownBy(() -> strategy.nextMove(board, Player.X))
                .isInstanceOf(IllegalStateException.class);
    }
}
