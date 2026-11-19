package com.comunidad360.security;

import com.comunidad360.common.api.TraceIdFilter;
import com.comunidad360.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.getRateLimit().isEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long minute = System.currentTimeMillis() / 60_000;
        String key = RequestInfo.ip(request);
        Window window;
        synchronized (windows) {
            if (windows.size() > 10_000) {
                windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
            }
            Window current = windows.get(key);
            if (current == null || current.minute != minute) {
                window = new Window(minute, 1);
                windows.put(key, window);
            } else {
                window = new Window(current.minute, current.count + 1);
                windows.put(key, window);
            }
        }
        if (window.count > properties.getRateLimit().getAuthPerMinute()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", "about:blank");
            body.put("title", "RATE_LIMITED");
            body.put("status", 429);
            body.put("code", "RATE_LIMITED");
            body.put("messageKey", "errors.rateLimited");
            body.put("params", Map.of());
            body.put("detail", "Demasiadas solicitudes");
            body.put("traceId", MDC.get(TraceIdFilter.TRACE_ID));
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Window(long minute, int count) {
    }
}
