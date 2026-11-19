package com.comunidad360.security;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestInfo {

    private RequestInfo() {
    }

    public static String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String ipHash(HttpServletRequest request) {
        String ip = ip(request);
        return ip == null ? null : TokenSupport.sha256Hex(ip);
    }

    public static String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }
}
