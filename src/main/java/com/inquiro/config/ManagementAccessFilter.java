package com.inquiro.config;

import com.inquiro.communication.messenger.MetaSignatureValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Temporary operator access for the Messenger pilot, pending business-user authentication. */
@Component
public class ManagementAccessFilter extends OncePerRequestFilter {
    private final String key;
    private final MetaSignatureValidator verifier;

    public ManagementAccessFilter(@Value("${inquiro.management-api-key:}") String key, MetaSignatureValidator verifier) {
        this.key = key;
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI();
        return !(path.equals("/api/business") || path.startsWith("/api/business/")
                || path.equals("/api/knowledge") || path.startsWith("/api/knowledge/")
                || path.equals("/api/test") || path.startsWith("/api/test/")
                || path.equals("/h2-console") || path.startsWith("/h2-console/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!verifier.tokenMatches(key, request.getHeader("X-Inquiro-Management-Key"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication required\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}