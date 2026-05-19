package com.codeit.monew.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCLoggingFilter extends OncePerRequestFilter {

    public final static String HEADER_USER_ID = "MoNew-Request-User-ID";
    private final static String REQUEST_ID = "requestId";
    private final static String CLIENT_IP = "clientIp";
    private final static String USER_ID = "userId";
    private final static String REQUEST_URL = "requestUrl";
    private final static String REQUEST_METHOD = "requestMethod";
    private final static String HEADER_REQUEST_ID = "Trace-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            String userId = request.getHeader(HEADER_USER_ID);

            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, extractClientIp(request));

            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID, userId);
            }

            MDC.put(REQUEST_URL, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());

            response.addHeader(HEADER_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    // 클라우드 연결 시 사용할 것
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }
}
