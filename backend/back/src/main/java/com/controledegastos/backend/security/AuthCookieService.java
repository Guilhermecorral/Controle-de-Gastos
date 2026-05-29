package com.controledegastos.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Centraliza a emissao e a leitura dos cookies HttpOnly usados na sessao autenticada.
 */
@Service
public class AuthCookieService {

    @Value("${app.security.cookies.access-token-name:cg_access_token}")
    private String accessTokenCookieName;

    @Value("${app.security.cookies.refresh-token-name:cg_refresh_token}")
    private String refreshTokenCookieName;

    @Value("${app.security.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${app.security.cookies.same-site:Lax}")
    private String sameSite;

    @Value("${app.security.cookies.domain:}")
    private String cookieDomain;

    /**
     * Escreve os cookies de acesso e refresh com escopo e expiracao diferentes.
     */
    public void writeAuthenticationCookies(
            HttpServletResponse response,
            String accessToken,
            long accessTokenExpirationMs,
            String refreshToken,
            long refreshTokenExpirationMs
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                accessTokenCookieName,
                accessToken,
                Duration.ofMillis(accessTokenExpirationMs),
                "/"
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                refreshTokenCookieName,
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationMs),
                "/api/auth"
        ).toString());
    }

    /**
     * Remove os cookies de sessao quando o usuario encerra a autenticacao.
     */
    public void clearAuthenticationCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessTokenCookieName, "", Duration.ZERO, "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshTokenCookieName, "", Duration.ZERO, "/api/auth").toString());
    }

    /**
     * Le o cookie de acesso para autenticar chamadas protegidas sem expor o token ao JavaScript.
     */
    public String resolveAccessToken(HttpServletRequest request) {
        return resolveCookieValue(request, accessTokenCookieName);
    }

    /**
     * Le o cookie de refresh para renovar a sessao de forma controlada no backend.
     */
    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, refreshTokenCookieName);
    }

    private String resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .path(path)
                .sameSite(sameSite)
                .maxAge(maxAge);

        Optional.ofNullable(cookieDomain)
                .map(String::trim)
                .filter(domain -> !domain.isBlank())
                .ifPresent(builder::domain);

        return builder.build();
    }
}
