package com.controledegastos.backend.security;

import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolve com seguranca o usuario autenticado no contexto atual.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    /**
     * Recupera o usuario autenticado da requisicao atual, independentemente de o principal
     * ter sido armazenado como entidade completa ou apenas como email.
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("No authenticated user in the current request");
        }

        Object principal = authentication.getPrincipal();
        String email = principal instanceof User user
                ? user.getEmail()
                : principal.toString();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
