package com.inquiro.security;

import com.inquiro.auth.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService service;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimitService service, RateLimitProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || isPreflight(request) || isStatic(request)) {
            chain.doFilter(request, response);
            return;
        }

        String key;
        int limit;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            key = "user:" + user.userId();
            limit = properties.getAuthenticatedRequestsPerMinute();
        } else {
            key = "ip:" + clientAddress(request);
            if (isWebhook(request)) limit = properties.getWebhookRequestsPerMinute();
            else if (isAuthEndpoint(request)) limit = properties.getAuthRequestsPerMinute();
            else if (isAiEndpoint(request)) limit = properties.getPublicAiRequestsPerMinute();
            else limit = properties.getRequestsPerMinute();
        }

        RateLimitService.Decision result = acquire(key, limit);
        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please retry later.\"}");
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > properties.getMaxRequestBodyBytes()) {
            response.setStatus(413);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Request body is too large.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private RateLimitService.Decision acquire(String key, int limit) {
        return service.tryAcquire(key, Math.max(1, limit));
    }

    private static boolean isAuthEndpoint(HttpServletRequest r) {
        String p = r.getRequestURI();
        return p.equals("/api/auth/login") || p.equals("/api/auth/register");
    }

    private static boolean isAiEndpoint(HttpServletRequest r) {
        String p = r.getRequestURI();
        return p.equals("/api/chat") || p.startsWith("/api/conversations/") || p.startsWith("/api/public/conversations/");
    }

    private static boolean isWebhook(HttpServletRequest r) {
        String p = r.getRequestURI();
        return p.equals("/webhook") || p.equals("/messenger/webhook") || p.equals("/whatsapp/webhook");
    }

    private static boolean isPreflight(HttpServletRequest r) { return "OPTIONS".equalsIgnoreCase(r.getMethod()); }
    private static boolean isStatic(HttpServletRequest r) { return r.getRequestURI().startsWith("/favicon"); }

    private static String clientAddress(HttpServletRequest r) {
        return r.getRemoteAddr() == null ? "unknown" : r.getRemoteAddr();
    }


}
