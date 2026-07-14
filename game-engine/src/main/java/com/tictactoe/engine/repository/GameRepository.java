package com.tictactoe.engine.repository;

import com.tictactoe.engine.model.Game;

import java.time.Instant;
import java.util.Optional;

public interface GameRepository {

    /**
     * Stores the game unless one with the same id already exists.
     *
     * @return true when the game was stored, false when the id is already taken
     */
    boolean saveIfAbsent(Game game);

    Optional<Game> findById(String gameId);

    /**
     * Removes games finished before the cutoff so the store cannot grow without bound.
     *
     * @return the number of removed games
     */
    int deleteFinishedBefore(Instant cutoff);

}
