package com.tictactoe.webui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes runtime configuration to the browser so the SPA does not need
 * the session service URL baked in at build time.
 */
@RestController
public class UiConfigController {

    private static final Logger log = LoggerFactory.getLogger(UiConfigController.class);

    private final String sessionBaseUrl;

    public UiConfigController(@Value("${session.base-url}") String sessionBaseUrl) {
        log.info("UI will talk to the session service at {}", sessionBaseUrl);
        this.sessionBaseUrl = sessionBaseUrl;
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("sessionBaseUrl", sessionBaseUrl);
    }
}
