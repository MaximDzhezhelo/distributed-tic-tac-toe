package com.tictactoe.session.client;

import com.tictactoe.common.logging.Correlation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EngineClientProperties.class)
public class EngineClientConfig {

    private static final Logger log = LoggerFactory.getLogger(EngineClientConfig.class);

    @Bean
    @ConditionalOnProperty(name = "engine.load-balanced", havingValue = "true", matchIfMissing = true)
    RestClient engineRestClient(RestClient.Builder builder,
                                LoadBalancerInterceptor loadBalancerInterceptor,
                                EngineClientProperties properties) {
        log.info("Game engine client: discovering '{}' via Eureka, connect timeout {}, read timeout {}",
                properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
        return configure(builder, properties, loadBalancerInterceptor);
    }

    /**
     * Direct mode (engine.load-balanced=false): engine.base-url is a concrete
     * address — used by the tests and available as an operational fallback.
     */
    @Bean
    @ConditionalOnProperty(name = "engine.load-balanced", havingValue = "false")
    RestClient directEngineRestClient(RestClient.Builder builder,
                                      EngineClientProperties properties) {
        log.info("Game engine client: direct base URL {}, connect timeout {}, read timeout {}",
                properties.baseUrl(), properties.connectTimeout(), properties.readTimeout());
        return configure(builder, properties, null);
    }

    private RestClient configure(RestClient.Builder builder,
                                 EngineClientProperties properties,
                                 ClientHttpRequestInterceptor loadBalancerInterceptor) {
        var requestFactory = ClientHttpRequestFactoryBuilder.detect()
                .build(HttpClientSettings.defaults()
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout()));

        builder = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                // propagate the current correlation id so both services log the same cid
                .requestInterceptor((request, body, execution) -> {
                    var cid = MDC.get(Correlation.CID);
                    if (cid != null) {
                        request.getHeaders().set(Correlation.HEADER, cid);
                    }
                    return execution.execute(request, body);
                });
        if (loadBalancerInterceptor != null) {
            builder = builder.requestInterceptor(loadBalancerInterceptor);
        }
        return builder.build();
    }

}
