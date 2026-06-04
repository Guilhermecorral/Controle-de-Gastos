package com.controledegastos.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aplica um limite simples por IP nas rotas publicas de autenticacao para reduzir abuso automatizado.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimitService authRateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;

    public AuthRateLimitFilter(AuthRateLimitService authRateLimitService, Environment environment) {
        this.authRateLimitService = authRateLimitService;
        this.environment = environment;
    }

    /**
     * Limita rotas publicas sensiveis onde abuso automatizado causa mais risco.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(
                "/api/auth/login".equals(path)
                        || "/api/auth/register".equals(path)
                        || "/api/auth/forgot-password".equals(path)
                        || "/api/auth/reset-password".equals(path)
                        || "/api/auth/refresh".equals(path)
        );
    }

    /**
     * Conta tentativas por janela curta e devolve 429 quando o limite for excedido.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long windowSeconds = resolveLongProperty("app.security.auth-rate-limit.window-seconds", 60L);
        int maxAttempts = resolveIntProperty("app.security.auth-rate-limit.max-attempts", 12);
        String key = resolveClientKey(request);

        if (!authRateLimitService.isAllowed(key, windowSeconds, maxAttempts)) {
            writeRateLimitResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Usa X-Forwarded-For quando existir e cai para o remote address quando nao houver proxy na frente.
     */
    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim() + ":" + request.getRequestURI();
        }

        return request.getRemoteAddr() + ":" + request.getRequestURI();
    }

    /**
     * Devolve um JSON consistente para o frontend quando o rate limit bloquear a chamada.
     */
    private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "message", "Muitas tentativas em pouco tempo. Aguarde um pouco antes de tentar novamente.",
                "path", request.getRequestURI(),
                "details", List.of()
        ));
    }

    private long resolveLongProperty(String propertyName, long defaultValue) {
        String rawValue = environment.getProperty(propertyName);

        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int resolveIntProperty(String propertyName, int defaultValue) {
        String rawValue = environment.getProperty(propertyName);

        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

}
