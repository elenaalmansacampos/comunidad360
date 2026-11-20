package com.comunidad360.security;

import com.comunidad360.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void generalLimitBlocksAfterConfiguredRequests() throws Exception {
        RateLimitFilter filter = filter(5, 100);
        MockHttpServletResponse response = null;
        for (int i = 0; i < 5; i++) {
            response = perform(filter, "GET", "/api/v1/communities");
            assertThat(response.getStatus()).isEqualTo(200);
        }
        response = perform(filter, "GET", "/api/v1/communities");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void authLimitIsStricterThanGeneral() throws Exception {
        RateLimitFilter filter = filter(100, 2);
        assertThat(perform(filter, "POST", "/api/v1/auth/login").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "POST", "/api/v1/auth/login").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "POST", "/api/v1/auth/login").getStatus()).isEqualTo(429);
    }

    @Test
    void nonApiAndOptionsRequestsAreNotLimited() throws Exception {
        RateLimitFilter filter = filter(1, 1);
        assertThat(perform(filter, "GET", "/actuator/health").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "OPTIONS", "/api/v1/communities").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "OPTIONS", "/api/v1/communities").getStatus()).isEqualTo(200);
    }

    @Test
    void disabledRateLimitDoesNotBlock() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getRateLimit().setEnabled(false);
        RateLimitFilter filter = new RateLimitFilter(properties, new ObjectMapper());
        for (int i = 0; i < 10; i++) {
            assertThat(perform(filter, "GET", "/api/v1/communities").getStatus()).isEqualTo(200);
        }
    }

    private RateLimitFilter filter(int generalPerMinute, int authPerMinute) {
        AppProperties properties = new AppProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setGeneralPerMinute(generalPerMinute);
        properties.getRateLimit().setAuthPerMinute(authPerMinute);
        return new RateLimitFilter(properties, new ObjectMapper());
    }

    private MockHttpServletResponse perform(RateLimitFilter filter, String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
