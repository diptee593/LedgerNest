package com.ledgernest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor


public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
     protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Every authenticated request must carry this header:
        // Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
        String authHeader = request.getHeader("Authorization");

        // No header or wrong format → skip JWT processing entirely
        // This is perfectly normal for /auth/login and /auth/register
        // Those routes are public so they don't need a token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Chop off "Bearer " (7 characters) to get the raw token string
        String token = authHeader.substring(7);

        // Extract email from token payload
        // If token is malformed this throws → caught by GlobalExceptionHandler
        String email = jwtUtil.extractEmail(token);

        // Two conditions before we authenticate:
        // 1. We successfully got an email from the token
        // 2. This request isn't already authenticated (avoid doing work twice)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Hit the DB to load full user details
            // Needed to get authorities (roles) and check if account is still active
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Final check — is the token valid and not expired?
            if (jwtUtil.isTokenValid(token)) {

                // Build Spring Security's authentication object
                // This is what SecurityContextHolder stores to represent "logged in user"
                // null for credentials — we don't need password here, JWT already verified identity
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach IP address and session info to the auth token
                // Used internally by Spring and available for audit logging later
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Store in SecurityContext — this is the moment the user becomes
                // "authenticated" for the lifetime of this request
                // Every @PreAuthorize check and security rule reads from here
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Always call this — passes the request to the next filter or controller
        // Never skip this line or the request dies here
        filterChain.doFilter(request, response);
    }
}