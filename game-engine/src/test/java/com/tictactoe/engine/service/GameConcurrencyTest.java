package com.tictactoe.engine.service;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;
import com.tictactoe.engine.exception.IllegalMoveException;
import com.tictactoe.engine.repository.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Races real threads against the engine to prove that concurrent moves
 * cannot corrupt a board: per-game synchronization serializes them and
 * losers are rejected instead of double-applied.
 */
class GameConcurrencyTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService(new InMemoryGameRepository());
    }

    @Test
    void onlyOneOfManyConcurrentIdenticalMovesSucceeds() throws InterruptedException {
        service.create("race");
        var threads = 16;
        var ready = new CountDownLatch(threads);
        var start = new CountDownLatch(1);
        var successes = new AtomicInteger();
        var rejections = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(threads)) {
            for (var i = 0; i < threads; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.move("race", Player.X, 4);
                        successes.incrementAndGet();
                    } catch (IllegalMoveException ex) {
                        rejections.incrementAndGet();
                    }
                    return null;
                });
            }
            ready.await();
            start.countDown();
        }

        assertThat(successes).hasValue(1);
        assertThat(rejections).hasValue(threads - 1);

        var state = service.get("race");
        assertThat(state.board()[4]).isEqualTo("X");
        assertThat(countSymbols(state.board())).isEqualTo(1);
    }

    @Test
    void concurrentMoveStormKeepsGameConsistent() {
        service.create("storm");

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var worker = 0; worker < 8; worker++) {
                executor.submit(() -> {
                    for (var attempt = 0; attempt < 10_000; attempt++) {
                        var state = service.get("storm");
                        if (state.status() != GameStatus.IN_PROGRESS || state.nextPlayer() == null) {
                            return null;
                        }
                        try {
                            service.move("storm", state.nextPlayer(),
                                    ThreadLocalRandom.current().nextInt(GameRules.BOARD_SIZE));
                        } catch (IllegalMoveException ignored) {
                            // lost a race (cell taken, turn already played, game over) — expected
                        }
                    }
                    return null;
                });
            }
        }

        var state = service.get("storm");
        var xCount = countSymbol(state.board(), "X");
        var oCount = countSymbol(state.board(), "O");

        assertThat(state.status()).isIn(GameStatus.WON, GameStatus.DRAW);
        // X starts and turns must strictly alternate even under contention
        assertThat(xCount - oCount).isBetween(0, 1);
        if (state.status() == GameStatus.WON) {
            assertThat(GameRules.hasWon(state.board(), state.winner())).isTrue();
        } else {
            assertThat(xCount + oCount).isEqualTo(GameRules.BOARD_SIZE);
        }
    }

    private int countSymbols(String[] board) {
        return countSymbol(board, "X") + countSymbol(board, "O");
    }

    private int countSymbol(String[] board, String symbol) {
        var count = 0;
        for (var cell : board) {
            if (symbol.equals(cell)) {
                count++;
            }
        }
        return count;
    }
}
