package com.tictactoe.session.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings for talking to the Game Engine Service.
 */
@ConfigurationProperties(prefix = "engine")
public record EngineClientProperties(
        String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout
) {
}
