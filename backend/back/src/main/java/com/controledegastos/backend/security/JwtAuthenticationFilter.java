package com.controledegastos.backend.security;

import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,  //Request HTTP entry
            @NonNull HttpServletResponse response, //Request HTTP exit
            @NonNull FilterChain filterChain //Chain filter or controller
    ) throws ServletException, IOException {

        // Reads the "Authorization" header from the request
        // Expected format: "Bearer eyJhbGciOiJIUzI1NiJ9..."
        final String authHeader = request.getHeader("Authorization");

        // If there is no header, or it doesn't start with "Bearer " — it's not our authenticated request
        // Pass it on without authenticating — SecurityConfig will decide whether to block it or not
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); //Continue the filter chain without authentication
            return;
        }

        // Extracts only the token, removing the "Bearer " prefix
        final String token = authHeader.substring(7);

        // Extracts the email from inside the JWT token
        final String email = jwtService.extractEmail(token);

        // If it managed to extract the email AND the user is not yet authenticated in this request
        // getAuthentication() == null means we haven't set the authentication yet
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Searches for the user in the database by the email extracted from the token
            // If not found, the token is invalid — does not authenticate
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null && jwtService.isTokenValid(token, user)) {

                // Creates the Spring Security authentication object
                // SimpleGrantedAuthority: converts the Role ("USER") to Spring's format ("ROLE_USER")
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null, // No password needed — we already trust the token
                                user.getAuthorities()
                        );

                //Add request details (IP, session) to the authentication token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Registers the authentication in the current request context
                // From this point, Spring knows who is making this request
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        //Continue the filter chain, passes to the next filter or reaches the controller\
        filterChain.doFilter(request, response);
    }
}
