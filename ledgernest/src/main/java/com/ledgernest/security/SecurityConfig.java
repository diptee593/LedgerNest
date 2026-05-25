package com.ledgernest.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// Enables @PreAuthorize("hasRole('ADMIN')") on individual methods later
// Without this annotation those annotations do nothing
@EnableMethodSecurity
@RequiredArgsConstructor

public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean

    public SecurityFilterChain  securityFilterChain(HttpSecurity http) throws Exception {
    http
                // Disable CSRF protection
                // CSRF is only needed for browser-based session apps (cookies)
                // We use stateless JWT — no cookies, no CSRF risk
                .csrf(AbstractHttpConfigurer::disable)

                // Route authorization rules — order matters, top to bottom
                .authorizeHttpRequests(auth -> auth

                    // /auth/register and /auth/login need no token
                    // Anyone can hit these — they are how you GET a token
                    .requestMatchers("/auth/**").permitAll()

                    // Every other route requires a valid JWT token
                    // No token or bad token = 401 Unauthorized
                    .anyRequest().authenticated()
                )

                // STATELESS = Spring creates zero sessions, zero cookies
                // Every single request must carry its own JWT — no memory between requests
                // This is what makes the app horizontally scalable (multiple servers)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Tell Spring which UserDetailsService + PasswordEncoder to use
                // when verifying login credentials
                .authenticationProvider(authenticationProvider())

                // Insert JwtFilter into the filter chain
                // BEFORE UsernamePasswordAuthenticationFilter means JWT runs first
                // So by the time Spring's default filter runs, user is already authenticated
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

        // Wires your UserDetailsService + PasswordEncoder together
        // Spring calls this internally when processing a login attempt
        @Bean
        public AuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
            provider.setUserDetailsService(userDetailsService);
            provider.setPasswordEncoder(passwordEncoder());
            return provider;
        }

        // BCrypt is industry standard for password hashing
        // It automatically salts passwords — same password hashes differently each time
        // Cost factor default is 10 — means 2^10 rounds of hashing (slow enough to resist brute force)
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        // AuthService needs this bean to trigger authentication programmatically during login
        // Without exposing this as a @Bean, Spring won't inject it where you need it
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
                throws Exception {
            return config.getAuthenticationManager();
        }
    }