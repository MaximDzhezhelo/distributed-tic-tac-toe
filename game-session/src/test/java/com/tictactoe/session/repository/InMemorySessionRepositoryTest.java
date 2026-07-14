package com.tictactoe.session.repository;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.GameState;
import com.tictactoe.common.model.GameStatus;
import com.tictactoe.session.model.Session;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionRepositoryTest {

    @Test
    void deletesOnlySessionsFinishedBeforeCutoff() {
        var repository = new InMemorySessionRepository();

        var finished = new Session("finished");
        finished.markRunning();
        finished.finish(new GameState("finished", new String[GameRules.BOARD_SIZE],
                GameStatus.DRAW, null, null));
        repository.save(finished);

        var running = new Session("running");
        running.markRunning();
        repository.save(running);

        assertThat(repository.deleteFinishedBefore(Instant.now().minusSeconds(60)))
                .as("nothing is old enough yet")
                .isZero();

        assertThat(repository.deleteFinishedBefore(Instant.now().plusSeconds(1)))
                .as("only the finished session is evicted")
                .isEqualTo(1);
        assertThat(repository.findById("finished")).isEmpty();
        assertThat(repository.findById("running")).isPresent();
    }
}
