package com.controledegastos.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Reaplica os headers CORS em respostas de erro para que o navegador consiga enxergar a causa real da falha.
 */
@Service
public class CorsHeaderService {

    private final CorsConfiguration corsConfiguration;

    public CorsHeaderService(
            @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173,https://farolfinanceiro.duckdns.org,https://project-niqqo-farolfinanceiro.vercel.app}")
            String allowedOrigins
    ) {
        List<String> allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(CorsHeaderService::normalizeOrigin)
                .filter(origin -> !origin.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Origin", "Accept"));
        configuration.setAllowCredentials(true);
        this.corsConfiguration = configuration;
    }

    /**
     * Replica os principais headers CORS quando a origem enviada pelo navegador estiver dentro da whitelist.
     */
    public void applyIfAllowed(HttpServletRequest request, HttpServletResponse response) {
        String origin = normalizeOrigin(request.getHeader(HttpHeaders.ORIGIN));

        if (origin.isBlank()) {
            return;
        }

        String matchedOrigin = resolveAllowedOrigin(origin);

        if (matchedOrigin == null) {
            return;
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, matchedOrigin);
        response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization,Content-Type,X-Requested-With,Origin,Accept");
    }

    /**
     * Informa se a origem da requisicao faz parte da whitelist configurada.
     */
    public String resolveAllowedOrigin(String origin) {
        return corsConfiguration.checkOrigin(normalizeOrigin(origin));
    }

    private static String normalizeOrigin(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
