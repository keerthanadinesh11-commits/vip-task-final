package com.taskflow.taskservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the current request's Authorization header to outgoing Feign calls.
 * Kept for backward compatibility; not used for Kafka-based notifications.
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignClientInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            String token = attr.getRequest().getHeader("Authorization");
            if (token != null) {
                template.header("Authorization", token);
            }
        } else {
            log.debug("No request context available for Feign interceptor");
        }
    }
}
