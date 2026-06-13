package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.ForgotPasswordResponseDTO;
import com.controledegastos.backend.auth.dto.ForgotPasswordRequestDTO;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.auth.dto.RegisterRequestDTO;
import com.controledegastos.backend.auth.dto.ResetPasswordRequestDTO;
import com.controledegastos.backend.security.CaptchaVerificationService;
import com.controledegastos.backend.security.JwtService;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o fluxo de cadastro, login e emissao de tokens.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CaptchaVerificationService captchaVerificationService;
    private final PasswordResetService passwordResetService;
    private final AuthenticatedUserService authenticatedUserService;
    private final TwoFactorService twoFactorService;

    /**
     * Cria um novo usuario, aplica hash na senha e devolve os tokens iniciais.
     */
    public AuthenticationSession register(RegisterRequestDTO dto, String remoteIp) {
        captchaVerificationService.assertValid(dto.captchaToken(), remoteIp, "cadastro");

        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email ja cadastrado");
        }

        validatePasswordStrength(dto.password());

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(User.Role.USER)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Email ja cadastrado");
        }

        return buildAuthenticationSession(user);
    }

    /**
     * Valida as credenciais informadas e devolve novos tokens para o usuario.
     */
    public LoginAttemptResult login(LoginRequestDTO dto, String remoteIp) {
        captchaVerificationService.assertValid(dto.captchaToken(), remoteIp, "login");

        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        if (!user.isActive()) {
            throw new IllegalStateException("Esta conta esta suspensa no momento");
        }

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }

        if (user.isTwoFactorEnabled() && (dto.twoFactorCode() == null || dto.twoFactorCode().isBlank())) {
            return LoginAttemptResult.challenge("Digite o código do seu aplicativo autenticador para concluir a entrada.");
        }

        if (user.isTwoFactorEnabled() && !twoFactorService.validateLoginCode(user, dto.twoFactorCode())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }

        return LoginAttemptResult.success(buildAuthenticationSession(user));
    }

    /**
     * Renova a sessao a partir de um refresh token valido e devolve o mesmo perfil publico.
     */
    public AuthenticationSession refreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Sessao expirada");
        }

        String email = jwtService.extractEmailSafely(refreshToken);

        if (email == null) {
            throw new BadCredentialsException("Sessao expirada");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessao expirada"));

        if (!user.isActive()) {
            throw new IllegalStateException("Esta conta esta suspensa no momento");
        }

        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new BadCredentialsException("Sessao expirada");
        }

        return buildAuthenticationSession(user);
    }

    /**
     * Devolve o perfil autenticado sem expor tokens ao frontend.
     */
    public AuthResponseDTO getCurrentUser() {
        User user = authenticatedUserService.getAuthenticatedUser();
        return buildAuthResponse(user);
    }

    /**
     * Devolve o perfil autenticado quando houver sessao valida e responde vazio quando o visitante ainda nao entrou.
     */
    public AuthResponseDTO getCurrentUserIfAuthenticated() {
        User user = authenticatedUserService.getAuthenticatedUserOrNull();
        return user == null ? null : buildAuthResponse(user);
    }

    /**
     * Inicia o fluxo de redefinicao de senha e mantem a resposta neutra por seguranca.
     */
    public ForgotPasswordResponseDTO requestPasswordReset(ForgotPasswordRequestDTO dto, String remoteIp, String applicationBaseUrl) {
        return passwordResetService.requestReset(dto, remoteIp, applicationBaseUrl);
    }

    /**
     * Conclui a troca de senha a partir do token temporario emitido anteriormente.
     */
    public void resetPassword(ResetPasswordRequestDTO dto, String remoteIp) {
        passwordResetService.resetPassword(dto, remoteIp);
    }

    public String buildFrontendResetUrl(String token) {
        return passwordResetService.buildFrontendResetUrl(token);
    }

    private AuthenticationSession buildAuthenticationSession(User user) {
        return new AuthenticationSession(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                buildAuthResponse(user)
        );
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        return new AuthResponseDTO(user.getName(), user.getEmail(), user.getRole().name(), user.isTwoFactorEnabled());
    }

    private void validatePasswordStrength(String password) {
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!hasUppercase || !hasDigit || !hasSpecial || password.length() < 8) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres, letra maiuscula, numero e caractere especial");
        }
    }

    public record LoginAttemptResult(
            AuthenticationSession session,
            boolean requiresTwoFactor,
            String message
    ) {
        static LoginAttemptResult success(AuthenticationSession session) {
            return new LoginAttemptResult(session, false, null);
        }

        static LoginAttemptResult challenge(String message) {
            return new LoginAttemptResult(null, true, message);
        }
    }
}
