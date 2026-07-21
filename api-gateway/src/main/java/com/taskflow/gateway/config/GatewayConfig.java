package com.taskflow.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Increases the codec buffer limit so large file uploads (multipart)
 * can pass through Spring Cloud Gateway without being truncated.
 * 20 MB = 20 * 1024 * 1024 bytes
 */
@Configuration
public class GatewayConfig implements WebFluxConfigurer {

    private static final int MAX_IN_MEMORY_SIZE = 20 * 1024 * 1024; // 20 MB in bytes

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE);
    }
}
