package com.vedant.eurds.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security headers to every HTTP response.
 * Protects against clickjacking, MIME sniffing, XSS, and other common attacks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Content Security Policy
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");

        // Referrer policy — don't leak URLs
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Prevent caching of authenticated responses
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}
