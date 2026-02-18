package com.vedant.eurds.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Get the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: If no header or doesn't start with Bearer, skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the token (remove "Bearer " prefix)
        final String token = authHeader.substring(7);

        try {
            // Step 4: Extract username from token
            final String username = jwtUtil.extractUsername(token);

            // Step 5: If username exists and user not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 7: Validate token
                if (jwtUtil.validateToken(token, userDetails)) {

                    // Step 7.5: Reject locked accounts even if token is valid
                    if (!userDetails.isAccountNonLocked()) {
                        log.warn("Blocked JWT request for locked account: {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Step 8: Create authentication object and set in SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Step 9: Tell Spring Security this user is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        // Step 10: Continue to the next filter / controller
        filterChain.doFilter(request, response);
    }
}