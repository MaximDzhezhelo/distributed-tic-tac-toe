package com.tictactoe.common.api;

import com.tictactoe.common.model.Player;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull(message = "player is required (X or O)")
        Player player,

        @NotNull(message = "position is required")
        @Min(value = 0, message = "position must be between 0 and 8")
        @Max(value = 8, message = "position must be between 0 and 8")
        Integer position
) {
}
