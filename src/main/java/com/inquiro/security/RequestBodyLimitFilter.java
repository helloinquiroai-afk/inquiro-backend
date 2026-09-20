package com.inquiro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestBodyLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;

    public RequestBodyLimitFilter(RateLimitProperties properties) { this.properties = properties; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length > properties.getMaxRequestBodyBytes()) {
            response.setStatus(413);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Request body is too large.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
