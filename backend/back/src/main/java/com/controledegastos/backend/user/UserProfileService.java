package com.controledegastos.backend.user;

import com.controledegastos.backend.auth.AuthenticationSession;
import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.security.JwtService;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.dto.DeleteAccountRequestDTO;
import com.controledegastos.backend.user.dto.UpdateProfileRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centraliza as alterações de perfil e o encerramento definitivo da conta.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Atualiza nome e e-mail do usuário autenticado e renova a sessão quando o e-mail muda.
     */
    @Transactional
    public AuthenticationSession updateProfile(UpdateProfileRequestDTO dto) {
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!user.getEmail().equalsIgnoreCase(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Já existe uma conta usando este e-mail");
        }

        user.setName(dto.name().trim());
        user.setEmail(dto.email().trim().toLowerCase());

        User savedUser = userRepository.save(user);

        return new AuthenticationSession(
                jwtService.generateAccessToken(savedUser),
                jwtService.generateRefreshToken(savedUser),
                buildAuthResponse(savedUser)
        );
    }

    /**
     * Exclui a conta autenticada apenas depois de confirmar a senha atual.
     */
    @Transactional
    public void deleteAccount(DeleteAccountRequestDTO dto) {
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("A senha atual não confere");
        }

        userRepository.delete(user);
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        return new AuthResponseDTO(user.getName(), user.getEmail(), user.getRole().name());
    }
}
