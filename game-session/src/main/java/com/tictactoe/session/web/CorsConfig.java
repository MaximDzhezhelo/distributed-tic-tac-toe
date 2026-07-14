package com.tictactoe.session.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The web-ui is served from a different origin (port 8080), so the browser
 * needs CORS approval to call the session API and subscribe to SSE.
 * The default is permissive to keep the demo easy to run (it even works with
 * index.html opened straight from disk); restrict via cors.allowed-origins
 * in a real deployment.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public CorsConfig(@Value("${cors.allowed-origins:*}") String[] allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/sessions/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST");
    }
}
