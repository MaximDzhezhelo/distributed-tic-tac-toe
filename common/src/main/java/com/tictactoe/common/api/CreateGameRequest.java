package com.tictactoe.common.api;

import com.tictactoe.common.validation.ValidId;

public record CreateGameRequest(@ValidId String gameId) {
}
