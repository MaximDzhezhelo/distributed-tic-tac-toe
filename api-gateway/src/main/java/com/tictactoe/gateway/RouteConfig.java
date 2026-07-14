package com.tictactoe.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single entry point for the whole system: API paths are routed to their
 * owning service, everything else (the SPA) to the web-ui. The lb:// targets
 * are service ids resolved dynamically through Eureka, so instances can move
 * or scale without touching the gateway. Correlation and other headers pass
 * through untouched.
 */
@Configuration
public class RouteConfig {

    private static final Logger log = LoggerFactory.getLogger(RouteConfig.class);

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${routes.engine-url:lb://game-engine}") String engineUrl,
                        @Value("${routes.session-url:lb://game-session}") String sessionUrl,
                        @Value("${routes.web-ui-url:lb://web-ui}") String webUiUrl) {
        log.info("Routing /games/** -> {}, /sessions/** -> {}, /** -> {}",
                engineUrl, sessionUrl, webUiUrl);
        return builder.routes()
                .route("game-engine", r -> r.path("/games/**").uri(engineUrl))
                .route("game-session", r -> r.path("/sessions/**").uri(sessionUrl))
                .route("web-ui", r -> r.path("/**").uri(webUiUrl))
                .build();
    }
}
