package com.tictactoe.engine.repository;

import com.tictactoe.engine.model.Game;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(Game game) {
        return games.putIfAbsent(game.getId(), game) == null;
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public int deleteFinishedBefore(Instant cutoff) {
        var removed = new AtomicInteger();
        games.values().removeIf(game -> {
            if (game.isFinishedBefore(cutoff)) {
                removed.incrementAndGet();
                return true;
            }
            return false;
        });
        return removed.get();
    }

}
