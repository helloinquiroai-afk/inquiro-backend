package com.inquiro.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RequestLoggerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-Id", requestId);
        MDC.put("requestId", requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            // No query string, headers, body, or customer/session identifiers.
            log.info("event=http_request request_id={} method={} status={} duration_ms={}",
                    requestId, request.getMethod(), response.getStatus(), (System.nanoTime() - start) / 1000000);
            MDC.remove("requestId");
        }
    }
}