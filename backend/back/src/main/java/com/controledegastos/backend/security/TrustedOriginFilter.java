package com.controledegastos.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Rejeita origens inesperadas em chamadas mutáveis quando a autenticação estiver baseada em cookies.
 */
@Component
public class TrustedOriginFilter extends OncePerRequestFilter {

    private final CorsConfiguration corsConfiguration;
    private final CorsHeaderService corsHeaderService;

    public TrustedOriginFilter(
            CorsHeaderService corsHeaderService,
            @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173,https://farolfinanceiro.online,https://www.farolfinanceiro.online}")
            String allowedOrigins
    ) {
        this.corsHeaderService = corsHeaderService;
        List<String> allowedOriginPatterns = AllowedOriginPatterns.expand(allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        this.corsConfiguration = configuration;
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

        String matchedOrigin = origin.isBlank() ? null : corsConfiguration.checkOrigin(origin);

        if (origin != null && !origin.isBlank() && matchedOrigin == null && !requestOrigin.equals(origin)) {
            corsHeaderService.applyIfAllowed(request, response);
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
        return AllowedOriginPatterns.normalize(origin);
    }
}
