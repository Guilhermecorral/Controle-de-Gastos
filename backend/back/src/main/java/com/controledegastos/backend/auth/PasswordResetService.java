package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.ForgotPasswordRequestDTO;
import com.controledegastos.backend.auth.dto.ForgotPasswordResponseDTO;
import com.controledegastos.backend.auth.dto.ResetPasswordRequestDTO;
import com.controledegastos.backend.security.CaptchaVerificationService;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Orquestra o fluxo seguro de esqueci minha senha sem vazar se um email existe ou nao.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetDeliveryService passwordResetDeliveryService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaVerificationService captchaVerificationService;

    @Value("${app.security.password-reset.token-expiration-minutes:30}")
    private long tokenExpirationMinutes;

    @Value("${app.security.password-reset.frontend-url:https://farolfinanceiro.online/redefinir-senha}")
    private String frontendResetUrl;

    /**
     * Dispara o fluxo de redefinicao para o email informado sem entregar pistas para atacantes.
     */
    @Transactional
    public ForgotPasswordResponseDTO requestReset(ForgotPasswordRequestDTO dto, String remoteIp, String applicationBaseUrl) {
        captchaVerificationService.assertValid(dto.captchaToken(), remoteIp, "recuperacao de senha");

        User user = userRepository.findByEmail(dto.email()).orElse(null);

        if (user != null) {
            passwordResetTokenRepository.deleteByUser(user);
            passwordResetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

            String rawToken = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(OffsetDateTime.now().plusMinutes(tokenExpirationMinutes))
                    .build();

            passwordResetTokenRepository.save(token);
            String resetLink = UriComponentsBuilder.fromUriString(applicationBaseUrl)
                    .path("/api/auth/reset-password/redirect")
                    .queryParam("token", rawToken)
                    .build()
                    .toUriString();
            passwordResetDeliveryService.deliverResetLink(user.getEmail(), resetLink);
            log.info("Fluxo de redefinicao preparado para {}", maskEmail(user.getEmail()));
        } else {
            log.info("Pedido de redefinicao recebido para email nao cadastrado");
        }

        return new ForgotPasswordResponseDTO("Se o email existir, enviaremos um link de redefinicao em instantes");
    }

    /**
     * Troca a senha quando o token ainda estiver valido e respeitar a politica minima de seguranca.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto, String remoteIp) {
        captchaVerificationService.assertValid(dto.captchaToken(), remoteIp, "troca de senha");

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new IllegalArgumentException("A confirmacao da senha precisa ser igual a senha informada");
        }

        validatePasswordStrength(dto.password());

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(dto.token()))
                .orElseThrow(() -> new IllegalArgumentException("O link de redefinicao e invalido ou expirou"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("O link de redefinicao e invalido ou expirou");
        }

        token.getUser().setPassword(passwordEncoder.encode(dto.password()));
        token.setUsedAt(OffsetDateTime.now());

        userRepository.save(token.getUser());
        passwordResetTokenRepository.save(token);
    }

    /**
     * Monta a URL final do frontend para que o backend possa redirecionar links recebidos por e-mail.
     */
    public String buildFrontendResetUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendResetUrl)
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private void validatePasswordStrength(String password) {
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!hasUppercase || !hasDigit || !hasSpecial || password.length() < 8) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres, letra maiuscula, numero e caractere especial");
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Nao foi possivel proteger o token de redefinicao", exception);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');

        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
