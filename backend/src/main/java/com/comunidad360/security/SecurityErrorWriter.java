package com.comunidad360.security;

import com.comunidad360.common.api.ApiError;
import com.comunidad360.common.api.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public SecurityErrorWriter(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                      String code, String messageKey) throws IOException {
        String detail = messageSource.getMessage(messageKey, null, messageKey, request.getLocale());
        ApiError error = new ApiError(
                URI.create("about:blank"),
                code,
                status.value(),
                code,
                messageKey,
                Map.of(),
                detail,
                MDC.get(TraceIdFilter.TRACE_ID)
        );
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
