package com.corems.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Simple correlation id filter that exposes a header and stores the value in MDC under `correlationId`.
 * This implementation is intentionally lightweight and safe for shared usage across modules.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_X_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_X_REQUEST_ID = "X-Request-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_X_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(HEADER_X_REQUEST_ID);
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_X_CORRELATION_ID, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}

