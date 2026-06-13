package com.controledegastos.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Responde o preflight CORS da API explicitamente para evitar diferencas de comportamento entre ambientes.
 */
@Component
public class ApiCorsFilter extends OncePerRequestFilter {

    private final CorsHeaderService corsHeaderService;

    public ApiCorsFilter(CorsHeaderService corsHeaderService) {
        this.corsHeaderService = corsHeaderService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String allowedOrigin = corsHeaderService.resolveAllowedOrigin(origin);

        if (allowedOrigin != null) {
            corsHeaderService.applyIfAllowed(request, response);
        }

        boolean isPreflight = HttpMethod.OPTIONS.matches(request.getMethod())
                && request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;

        if (isPreflight) {
            if (allowedOrigin == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origem nao autorizada");
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
