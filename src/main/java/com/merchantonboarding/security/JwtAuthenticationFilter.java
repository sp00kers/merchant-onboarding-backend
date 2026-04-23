package com.merchantonboarding.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.UserRepository;
import com.merchantonboarding.service.JwtService;
import com.merchantonboarding.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from header
        jwt = authHeader.substring(7);

        try {
            // Extract username (email) from JWT
            userEmail = jwtService.extractUsername(jwt);

            // If user email is valid and no authentication exists in context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details from database
                UserDetails userDetails = userService.loadUserByUsername(userEmail);

                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    // Validate session ID matches the active session in the DB
                    String tokenSessionId = jwtService.extractSessionId(jwt);
                    User user = userRepository.findByEmail(userEmail).orElse(null);

                    if (user == null || user.getActiveSessionToken() == null
                            || !user.getActiveSessionToken().equals(tokenSessionId)) {
                        // Session has been invalidated (user logged in elsewhere)
                        logger.warn("Session invalidated for user: " + userEmail);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\": \"Session expired. You have been logged in from another location.\"}");
                        response.setContentType("application/json");
                        return;
                    }

                    // Create authentication token with user details and authorities
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                    // Set additional details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log the error but don't stop the filter chain
            logger.error("JWT Authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

