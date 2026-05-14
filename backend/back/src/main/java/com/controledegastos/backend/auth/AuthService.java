package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.auth.dto.RegisterRequestDTO;
import com.controledegastos.backend.security.JwtService;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Orquestra o fluxo de cadastro, login e emissao de tokens.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Cria um novo usuario, aplica hash na senha e devolve os tokens iniciais.
     */
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email ja cadastrado" + dto.email());
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    /**
     * Valida as credenciais informadas e devolve novos tokens para o usuario.
     */
    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }

        return buildAuthResponse(user);
    }

    /**
     * Centraliza a montagem da resposta de autenticacao para cadastro e login.
     */
    private AuthResponseDTO buildAuthResponse(User user) {
        return new AuthResponseDTO(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
