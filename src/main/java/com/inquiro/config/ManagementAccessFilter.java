package com.inquiro.config;

import com.inquiro.communication.messenger.MetaSignatureValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Temporary operator access for internal/management operations.
 * Business users authenticate separately using bearer access tokens.
 */
public class ManagementAccessFilter extends OncePerRequestFilter {

    private final String key;
    private final MetaSignatureValidator verifier;

    public ManagementAccessFilter(
            String key,
            MetaSignatureValidator verifier) {
        this.key = key;
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if (path.isEmpty()) {
            path = request.getRequestURI();
        }

        return !(path.equals("/api/business")
                || path.startsWith("/api/business/")
                || path.equals("/api/knowledge")
                || path.startsWith("/api/knowledge/")
                || path.equals("/api/test")
                || path.startsWith("/api/test/")
                || path.equals("/h2-console")
                || path.startsWith("/h2-console/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        // Internal/operator authentication
        if (verifier.tokenMatches(
                key,
                request.getHeader("X-Inquiro-Management-Key"))) {

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            "operator",
                            null,
                            java.util.List.of(
                                    new SimpleGrantedAuthority("ROLE_OPERATOR")));

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }

            return;
        }

        // Business-user authentication is handled by BearerTokenAuthenticationFilter.
        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.regionMatches(
                true, 0, "Bearer ", 0, 7)) {

            chain.doFilter(request, response);
            return;
        }

        // Neither operator key nor business-user token.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Authentication required\"}");
    }
}