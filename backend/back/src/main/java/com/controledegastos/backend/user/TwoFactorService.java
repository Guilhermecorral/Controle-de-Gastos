package com.controledegastos.backend.user;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.security.SensitiveFieldEncryptionService;
import com.controledegastos.backend.security.TwoFactorTotpService;
import com.controledegastos.backend.user.dto.DisableTwoFactorRequestDTO;
import com.controledegastos.backend.user.dto.TwoFactorSetupResponseDTO;
import com.controledegastos.backend.user.dto.TwoFactorStatusResponseDTO;
import com.controledegastos.backend.user.dto.TwoFactorVerifyRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Centraliza a ativacao, confirmacao e desligamento do segundo fator.
 */
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final AuthenticatedUserService authenticatedUserService;
    private final SensitiveFieldEncryptionService sensitiveFieldEncryptionService;
    private final TwoFactorTotpService twoFactorTotpService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Informa se o segundo fator esta ativo ou se existe uma configuracao pendente.
     */
    @Transactional(readOnly = true)
    public TwoFactorStatusResponseDTO getStatus() {
        User user = authenticatedUserService.getAuthenticatedUser();
        return new TwoFactorStatusResponseDTO(
                user.isTwoFactorEnabled(),
                user.getTwoFactorPendingSecretEncrypted() != null,
                twoFactorTotpService.getIssuer()
        );
    }

    /**
     * Gera um novo segredo temporario e devolve os dados para cadastro no app autenticador.
     */
    @Transactional
    public TwoFactorSetupResponseDTO beginSetup() {
        User user = authenticatedUserService.getAuthenticatedUser();
        String secret = twoFactorTotpService.generateSecret();

        user.setTwoFactorPendingSecretEncrypted(sensitiveFieldEncryptionService.encrypt(secret));

        return new TwoFactorSetupResponseDTO(
                secret,
                twoFactorTotpService.getIssuer(),
                user.getEmail(),
                twoFactorTotpService.buildOtpAuthUri(user.getEmail(), secret)
        );
    }

    /**
     * Confirma a configuracao pendente somente quando o codigo do app autenticador bate com o segredo gerado.
     */
    @Transactional
    public TwoFactorStatusResponseDTO confirmSetup(TwoFactorVerifyRequestDTO dto) {
        User user = authenticatedUserService.getAuthenticatedUser();
        String pendingSecret = sensitiveFieldEncryptionService.decrypt(user.getTwoFactorPendingSecretEncrypted());

        if (pendingSecret == null) {
            throw new IllegalArgumentException("Nenhuma configuracao de dois fatores esta pendente");
        }

        if (!twoFactorTotpService.verifyCode(pendingSecret, dto.code())) {
            throw new BadCredentialsException("Codigo de dois fatores invalido");
        }

        user.setTwoFactorSecretEncrypted(user.getTwoFactorPendingSecretEncrypted());
        user.setTwoFactorPendingSecretEncrypted(null);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorEnabledAt(LocalDateTime.now());

        return getStatus();
    }

    /**
     * Desliga o segundo fator apenas com senha atual e codigo valido para evitar sequestro de conta.
     */
    @Transactional
    public TwoFactorStatusResponseDTO disable(DisableTwoFactorRequestDTO dto) {
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Senha atual invalida");
        }

        String activeSecret = sensitiveFieldEncryptionService.decrypt(user.getTwoFactorSecretEncrypted());
        if (activeSecret == null || !user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("A autenticacao em dois fatores nao esta ativa");
        }

        if (!twoFactorTotpService.verifyCode(activeSecret, dto.code())) {
            throw new BadCredentialsException("Codigo de dois fatores invalido");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecretEncrypted(null);
        user.setTwoFactorPendingSecretEncrypted(null);
        user.setTwoFactorEnabledAt(null);

        return getStatus();
    }

    /**
     * Valida o codigo informado no login quando o usuario ja possui o segundo fator ativado.
     */
    @Transactional(readOnly = true)
    public boolean validateLoginCode(User user, String code) {
        if (!user.isTwoFactorEnabled()) {
            return true;
        }

        String activeSecret = sensitiveFieldEncryptionService.decrypt(user.getTwoFactorSecretEncrypted());
        return activeSecret != null && twoFactorTotpService.verifyCode(activeSecret, code);
    }
}
