package com.tictactoe.engine.repository;

import com.tictactoe.common.model.Player;
import com.tictactoe.engine.model.Game;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGameRepositoryTest {

    @Test
    void deletesOnlyGamesFinishedBeforeCutoff() {
        var repository = new InMemoryGameRepository();

        var finished = new Game("finished");
        finished.applyMove(Player.X, 0);
        finished.applyMove(Player.O, 3);
        finished.applyMove(Player.X, 1);
        finished.applyMove(Player.O, 4);
        finished.applyMove(Player.X, 2);
        repository.saveIfAbsent(finished);

        var running = new Game("running");
        running.applyMove(Player.X, 0);
        repository.saveIfAbsent(running);

        assertThat(repository.deleteFinishedBefore(Instant.now().minusSeconds(60)))
                .as("nothing is old enough yet")
                .isZero();

        assertThat(repository.deleteFinishedBefore(Instant.now().plusSeconds(1)))
                .as("only the finished game is evicted")
                .isEqualTo(1);
        assertThat(repository.findById("finished")).isEmpty();
        assertThat(repository.findById("running")).isPresent();
    }
}
