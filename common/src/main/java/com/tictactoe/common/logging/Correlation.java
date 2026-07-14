package com.tictactoe.common.logging;

import java.util.UUID;

/**
 * MDC keys and header name used to correlate log lines within one request,
 * across services and across a whole simulated game.
 */
public final class Correlation {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CID = "cid";
    public static final String GAME_ID = "gameId";

    private Correlation() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
