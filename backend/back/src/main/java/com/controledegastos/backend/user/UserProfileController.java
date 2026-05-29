package com.controledegastos.backend.user;

import com.controledegastos.backend.auth.AuthenticationSession;
import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.SimpleMessageResponseDTO;
import com.controledegastos.backend.security.AuthCookieService;
import com.controledegastos.backend.security.JwtService;
import com.controledegastos.backend.user.dto.DeleteAccountRequestDTO;
import com.controledegastos.backend.user.dto.UpdateProfileRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe o perfil da conta autenticada para ajustes diretos no app.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthCookieService authCookieService;
    private final JwtService jwtService;

    /**
     * Atualiza o perfil e devolve a nova identidade pública da sessão.
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
     * Exclui a conta e remove a sessão do navegador.
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
}
