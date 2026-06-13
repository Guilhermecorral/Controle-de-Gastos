package com.controledegastos.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rejeita origens inesperadas em chamadas mutáveis quando a autenticação estiver baseada em cookies.
 */
@Component
public class TrustedOriginFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    public TrustedOriginFilter(
            @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
            String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(TrustedOriginFilter::normalizeOrigin)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toSet());
    }

    /**
     * Aplica a checagem apenas em chamadas mutáveis da API, onde o risco de abuso é maior.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        boolean safeMethod = HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);
        return safeMethod || !request.getRequestURI().startsWith("/api/");
    }

    /**
     * Permite chamadas sem header Origin, mas valida origens de navegador contra a lista aprovada.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = normalizeOrigin(request.getHeader("Origin"));
        String requestOrigin = normalizeOrigin(request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort()));

        if (origin != null && !origin.isBlank() && !allowedOrigins.contains(origin) && !requestOrigin.equals(origin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"message":"Origem nao autorizada para esta operacao"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String normalizeOrigin(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
