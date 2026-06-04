package com.controledegastos.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
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

    private static final String DEFAULT_ACCESS_COOKIE_NAME = "cg_access_token";
    private static final String DEFAULT_REFRESH_COOKIE_NAME = "cg_refresh_token";
    private static final boolean DEFAULT_SECURE_COOKIES = false;
    private static final String DEFAULT_SAME_SITE = "Lax";

    private final Environment environment;

    public AuthCookieService(Environment environment) {
        this.environment = environment;
    }

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
                resolveAccessTokenCookieName(),
                accessToken,
                Duration.ofMillis(accessTokenExpirationMs),
                "/"
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                resolveRefreshTokenCookieName(),
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationMs),
                "/api/auth"
        ).toString());
    }

    /**
     * Remove os cookies de sessao quando o usuario encerra a autenticacao.
     */
    public void clearAuthenticationCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(resolveAccessTokenCookieName(), "", Duration.ZERO, "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(resolveRefreshTokenCookieName(), "", Duration.ZERO, "/api/auth").toString());
    }

    /**
     * Le o cookie de acesso para autenticar chamadas protegidas sem expor o token ao JavaScript.
     */
    public String resolveAccessToken(HttpServletRequest request) {
        return resolveCookieValue(request, resolveAccessTokenCookieName());
    }

    /**
     * Le o cookie de refresh para renovar a sessao de forma controlada no backend.
     */
    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, resolveRefreshTokenCookieName());
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
                .secure(resolveSecureCookies())
                .path(path)
                .sameSite(resolveSameSite())
                .maxAge(maxAge);

        Optional.ofNullable(resolveCookieDomain())
                .map(String::trim)
                .filter(domain -> !domain.isBlank())
                .ifPresent(builder::domain);

        return builder.build();
    }

    private String resolveAccessTokenCookieName() {
        return resolveStringProperty("app.security.cookies.access-token-name", DEFAULT_ACCESS_COOKIE_NAME);
    }

    private String resolveRefreshTokenCookieName() {
        return resolveStringProperty("app.security.cookies.refresh-token-name", DEFAULT_REFRESH_COOKIE_NAME);
    }

    private boolean resolveSecureCookies() {
        return resolveBooleanProperty("app.security.cookies.secure", DEFAULT_SECURE_COOKIES);
    }

    private String resolveSameSite() {
        return resolveStringProperty("app.security.cookies.same-site", DEFAULT_SAME_SITE);
    }

    private String resolveCookieDomain() {
        return resolveStringProperty("app.security.cookies.domain", "");
    }

    private String resolveStringProperty(String propertyName, String defaultValue) {
        String rawValue = environment.getProperty(propertyName);
        return rawValue == null ? defaultValue : rawValue;
    }

    private boolean resolveBooleanProperty(String propertyName, boolean defaultValue) {
        String rawValue = environment.getProperty(propertyName);

        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(rawValue.trim());
    }
}
