package com.controledegastos.backend.config;

import com.controledegastos.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration : class configuration - Spring reads @Bean definite
// @EnableWebSecurity : Active configuration customize Spring Security
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Define the HTTP security rules — the 'map' of who can access what
    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disables CSRF: REST APIs use token (JWT) for security, not session cookie
            // CSRF is only necessary for apps with traditional HTML forms
            .csrf(AbstractHttpConfigurer::disable)

            // Define the authorization rules by route
            .authorizeHttpRequests(auth -> auth
                //
                .requestMatchers(
                        "/api/auth/**", // register and login
                        "/swagger-ui/**", // API documentation
                        "/swagger-ui.html", // HTML
                        "/v3/api-docs/**", // schema OpenAPI
                        "/swagger-resources/**", // Resources
                        "/webjars/**" // Webjars
                ).permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Stateless: the server does NOT store session — each request is independent
            // The JWT carries everything the server needs to know about the user
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // BCrypt Bean — password hashing algorithm
    // "10" is the cost factor: 2^10 = 1024 hash iterations
    // The higher, the more secure but slower — 10 is the recommended default
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Expose AuthenticationManager as a Bean — needed for authentication in AuthService
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
