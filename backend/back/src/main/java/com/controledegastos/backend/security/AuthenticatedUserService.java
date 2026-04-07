package com.controledegastos.backend.security; // Declares the package for authentication helper services.

import com.controledegastos.backend.user.User; // Imports the authenticated user entity.
import com.controledegastos.backend.user.UserRepository; // Imports the repository used to resolve the user by email.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for injected dependencies.
import org.springframework.security.authentication.AnonymousAuthenticationToken; // Imports the anonymous authentication type for defensive checks.
import org.springframework.security.core.Authentication; // Imports the Spring Security authentication contract.
import org.springframework.security.core.context.SecurityContextHolder; // Imports access to the current security context.
import org.springframework.stereotype.Service; // Registers the helper as a Spring service bean.

@Service // Registers this class as a Spring-managed service.
@RequiredArgsConstructor // Generates the constructor for the required repository dependency.
public class AuthenticatedUserService { // Declares the helper responsible for resolving the current authenticated user.

    private final UserRepository userRepository; // Stores the repository used to load the user when needed.

    // Resolves the authenticated user from the current security context.
    public User getAuthenticatedUser() { // Starts the method used by services that need the current user.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // Reads the current authentication from the security context.
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) { // Validates that a real authenticated request exists.
            throw new RuntimeException("No authenticated user in the current request"); // Fails fast when the endpoint is accessed without a valid authenticated principal.
        }
        Object principal = authentication.getPrincipal(); // Reads the authenticated principal from the request.
        if (principal instanceof User user) { // Uses the entity directly when the JWT filter already stored it.
            return user; // Returns the authenticated user immediately.
        }
        String email = principal.toString(); // Falls back to the principal string when the subject is represented as email text.
        return userRepository.findByEmail(email) // Loads the user that owns the authenticated email.
                .orElseThrow(() -> new RuntimeException("Authenticated user not found")); // Fails fast when the token subject no longer exists in the database.
    }
}
