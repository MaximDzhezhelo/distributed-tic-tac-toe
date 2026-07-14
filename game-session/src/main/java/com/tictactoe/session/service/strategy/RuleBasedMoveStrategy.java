package com.tictactoe.session.service.strategy;

import com.tictactoe.common.model.GameRules;
import com.tictactoe.common.model.Player;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple heuristic: take a winning move, otherwise block the opponent's
 * winning move, otherwise prefer center, then a random corner, then any cell.
 * The random tie-breaking keeps simulated games varied.
 */
@Component
public class RuleBasedMoveStrategy implements MoveStrategy {

    private static final int CENTER = 4;
    private static final int[] CORNERS = {0, 2, 6, 8};

    @Override
    public int nextMove(String[] board, Player player) {
        var empty = emptyCells(board);
        if (empty.isEmpty()) {
            throw new IllegalStateException("No moves left on the board");
        }

        var winning = findLineCompletingMove(board, player.name());
        if (winning.isPresent()) {
            return winning.getAsInt();
        }

        var blocking = findLineCompletingMove(board, player.other().name());
        if (blocking.isPresent()) {
            return blocking.getAsInt();
        }

        if (board[CENTER] == null) {
            return CENTER;
        }

        var freeCorners = new ArrayList<Integer>();
        for (var corner : CORNERS) {
            if (board[corner] == null) {
                freeCorners.add(corner);
            }
        }
        if (!freeCorners.isEmpty()) {
            return pickRandom(freeCorners);
        }

        return pickRandom(empty);
    }

    private OptionalInt findLineCompletingMove(String[] board, String symbol) {
        var candidates = new ArrayList<Integer>();
        for (var line : GameRules.WINNING_LINES) {
            var emptyCell = -1;
            var owned = 0;
            for (var cell : line) {
                if (symbol.equals(board[cell])) {
                    owned++;
                } else if (board[cell] == null) {
                    emptyCell = cell;
                }
            }
            if (owned == 2 && emptyCell >= 0) {
                candidates.add(emptyCell);
            }
        }
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        return OptionalInt.of(candidates.getFirst());
    }

    private List<Integer> emptyCells(String[] board) {
        var empty = new ArrayList<Integer>();
        for (var i = 0; i < board.length; i++) {
            if (board[i] == null) {
                empty.add(i);
            }
        }
        return empty;
    }

    private int pickRandom(List<Integer> cells) {
        return cells.get(ThreadLocalRandom.current().nextInt(cells.size()));
    }

}
