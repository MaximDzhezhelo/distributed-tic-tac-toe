package com.tictactoe.session.model;

import com.tictactoe.common.model.GameStatus;
import com.tictactoe.common.model.Player;

import java.time.Instant;

public record Move(
        int number,
        Player player,
        int position,
        GameStatus resultingStatus,
        Instant timestamp
) {
}
