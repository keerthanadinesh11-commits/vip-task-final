package com.taskflow.taskservice.config;

import feign.RequestTemplate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FeignClientInterceptorTest {

    private final FeignClientInterceptor interceptor = new FeignClientInterceptor();

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apply_withAuthorizationHeader_forwardsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertEquals("Bearer test-token", template.headers().get("Authorization").iterator().next());
    }

    @Test
    void apply_withoutAuthorizationHeader_doesNotAddHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertNull(template.headers().get("Authorization"));
    }

    @Test
    void apply_noRequestContext_doesNotThrow() {
        RequestContextHolder.resetRequestAttributes(); // ensure no context
        RequestTemplate template = new RequestTemplate();
        // Should not throw
        interceptor.apply(template);
        assertNull(template.headers().get("Authorization"));
    }
}
