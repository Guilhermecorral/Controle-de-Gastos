package com.controledegastos.backend.user;

import com.controledegastos.backend.auth.AuthenticationSession;
import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.SimpleMessageResponseDTO;
import com.controledegastos.backend.security.AuthCookieService;
import com.controledegastos.backend.security.JwtService;
import com.controledegastos.backend.user.dto.DeleteAccountRequestDTO;
import com.controledegastos.backend.user.dto.DisableTwoFactorRequestDTO;
import com.controledegastos.backend.user.dto.TwoFactorSetupResponseDTO;
import com.controledegastos.backend.user.dto.TwoFactorStatusResponseDTO;
import com.controledegastos.backend.user.dto.TwoFactorVerifyRequestDTO;
import com.controledegastos.backend.user.dto.UpdateProfileRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe o perfil autenticado e as ações sensíveis da conta no app.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final TwoFactorService twoFactorService;
    private final AuthCookieService authCookieService;
    private final JwtService jwtService;

    /**
     * Atualiza o perfil e renova a sessão para manter o cookie alinhado ao usuário atual.
     */
    @PutMapping
    public ResponseEntity<AuthResponseDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO dto,
            HttpServletResponse response
    ) {
        AuthenticationSession session = userProfileService.updateProfile(dto);
        authCookieService.writeAuthenticationCookies(
                response,
                session.accessToken(),
                jwtService.getAccessExpiration(),
                session.refreshToken(),
                jwtService.getRefreshExpiration()
        );
        return ResponseEntity.ok(session.user());
    }

    /**
     * Exclui a conta definitivamente e remove a sessão do navegador.
     */
    @DeleteMapping
    public ResponseEntity<SimpleMessageResponseDTO> deleteAccount(
            @Valid @RequestBody DeleteAccountRequestDTO dto,
            HttpServletResponse response
    ) {
        userProfileService.deleteAccount(dto);
        authCookieService.clearAuthenticationCookies(response);
        return ResponseEntity.ok(new SimpleMessageResponseDTO("Conta excluída com sucesso"));
    }

    /**
     * Informa se o segundo fator já está ativo ou se existe uma configuração pendente.
     */
    @GetMapping("/two-factor")
    public ResponseEntity<TwoFactorStatusResponseDTO> getTwoFactorStatus() {
        return ResponseEntity.ok(twoFactorService.getStatus());
    }

    /**
     * Gera um segredo temporário para cadastrar a conta em um app autenticador.
     */
    @PostMapping("/two-factor/setup")
    public ResponseEntity<TwoFactorSetupResponseDTO> beginTwoFactorSetup() {
        return ResponseEntity.ok(twoFactorService.beginSetup());
    }

    /**
     * Confirma o segundo fator depois que o usuário digita um código TOTP válido.
     */
    @PostMapping("/two-factor/confirm")
    public ResponseEntity<TwoFactorStatusResponseDTO> confirmTwoFactor(
            @Valid @RequestBody TwoFactorVerifyRequestDTO dto
    ) {
        return ResponseEntity.ok(twoFactorService.confirmSetup(dto));
    }

    /**
     * Desliga o segundo fator exigindo senha atual e código do autenticador.
     */
    @PostMapping("/two-factor/disable")
    public ResponseEntity<TwoFactorStatusResponseDTO> disableTwoFactor(
            @Valid @RequestBody DisableTwoFactorRequestDTO dto
    ) {
        return ResponseEntity.ok(twoFactorService.disable(dto));
    }
}
