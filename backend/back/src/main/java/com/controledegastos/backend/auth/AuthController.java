package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.AuthResponseDTO;
import com.controledegastos.backend.auth.dto.ForgotPasswordResponseDTO;
import com.controledegastos.backend.auth.dto.ForgotPasswordRequestDTO;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.auth.dto.RegisterRequestDTO;
import com.controledegastos.backend.auth.dto.ResetPasswordRequestDTO;
import com.controledegastos.backend.auth.dto.SimpleMessageResponseDTO;
import com.controledegastos.backend.auth.dto.TwoFactorChallengeResponseDTO;
import com.controledegastos.backend.security.AuthCookieService;
import com.controledegastos.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;

/**
 * Expoe o fluxo publico de autenticacao e os utilitarios de sessao usados pelo frontend.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final JwtService jwtService;

    /**
     * Registra um novo usuario, abre a sessao em cookies seguros e devolve apenas o perfil publico.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info("Tentativa de cadastro para email={} a partir de {}", dto.email(), request.getRemoteAddr());
        AuthenticationSession session = authService.register(dto, request.getRemoteAddr());
        writeCookies(response, session);
        log.info("Cadastro concluido com sucesso para email={}", dto.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(session.user());
    }

    /**
     * Autentica um usuario existente e passa a sessao para cookies HttpOnly.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginAttemptResult result = authService.login(dto, request.getRemoteAddr());
        if (result.requiresTwoFactor()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new TwoFactorChallengeResponseDTO(true, result.message()));
        }

        AuthenticationSession session = result.session();
        writeCookies(response, session);
        return ResponseEntity.ok(session.user());
    }

    /**
     * Permite que o frontend recupere o usuario atual a partir da sessao ja aberta.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    /**
     * Renova a sessao usando apenas o refresh token protegido no cookie do backend.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticationSession session = authService.refreshSession(authCookieService.resolveRefreshToken(request));
        writeCookies(response, session);
        return ResponseEntity.ok(session.user());
    }

    /**
     * Encerra a sessao removendo os cookies sensiveis do navegador.
     */
    @PostMapping("/logout")
    public ResponseEntity<SimpleMessageResponseDTO> logout(HttpServletResponse response) {
        authCookieService.clearAuthenticationCookies(response);
        return ResponseEntity.ok(new SimpleMessageResponseDTO("Sessao encerrada com sucesso"));
    }

    /**
     * Inicia o fluxo de recuperacao de senha e devolve sempre a mesma resposta externa.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO dto,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(authService.requestPasswordReset(dto, request.getRemoteAddr(), resolveApplicationBaseUrl(request)));
    }

    /**
     * Conclui a troca de senha quando o token temporario ainda estiver valido.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<SimpleMessageResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto,
            HttpServletRequest request
    ) {
        authService.resetPassword(dto, request.getRemoteAddr());
        return ResponseEntity.ok(new SimpleMessageResponseDTO("Senha alterada com sucesso"));
    }

    /**
     * Recebe o link do e-mail e redireciona para a tela correta do frontend com o token preservado.
     */
    @GetMapping("/reset-password/redirect")
    public ResponseEntity<Void> redirectToFrontendReset(@RequestParam("token") String token) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authService.buildFrontendResetUrl(token))
                .build();
    }

    private void writeCookies(HttpServletResponse response, AuthenticationSession session) {
        authCookieService.writeAuthenticationCookies(
                response,
                session.accessToken(),
                jwtService.getAccessExpiration(),
                session.refreshToken(),
                jwtService.getRefreshExpiration()
        );
    }

    private String resolveApplicationBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        return defaultPort
                ? scheme + "://" + serverName
                : scheme + "://" + serverName + ":" + serverPort;
    }
}
