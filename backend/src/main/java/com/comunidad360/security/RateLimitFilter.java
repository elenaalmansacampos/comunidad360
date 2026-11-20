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

    private static final String AUTH_PREFIX = "/api/v1/auth/";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> generalWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> authWindows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.getRateLimit().isEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long minute = System.currentTimeMillis() / 60_000;
        String client = RequestInfo.ip(request);

        boolean limited = consume(generalWindows, client, properties.getRateLimit().getGeneralPerMinute(), minute);
        if (!limited && request.getRequestURI().startsWith(AUTH_PREFIX)) {
            limited = consume(authWindows, client, properties.getRateLimit().getAuthPerMinute(), minute);
        }
        if (limited) {
            writeRateLimited(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean consume(Map<String, Window> windows, String client, int limit, long minute) {
        if (limit <= 0) {
            return false;
        }
        Window updated;
        synchronized (windows) {
            if (windows.size() > MAX_TRACKED_CLIENTS) {
                windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
            }
            Window current = windows.get(client);
            if (current == null || current.minute() != minute) {
                updated = new Window(minute, 1);
            } else {
                updated = new Window(current.minute(), current.count() + 1);
            }
            windows.put(client, updated);
        }
        return updated.count() > limit;
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
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
    }

    private record Window(long minute, int count) {
    }
}
