package com.inquiro.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("================================================");
        System.out.println("METHOD : " + request.getMethod());
        System.out.println("URI    : " + request.getRequestURI());
        System.out.println("QUERY  : " + request.getQueryString());
        System.out.println("================================================");

        filterChain.doFilter(request, response);
    }
}
