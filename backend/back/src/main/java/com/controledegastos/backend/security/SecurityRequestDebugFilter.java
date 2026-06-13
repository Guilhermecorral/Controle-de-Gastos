package com.controledegastos.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Gera logs detalhados das rotas de autenticacao para diagnosticar problemas de CORS, cookies e seguranca em producao.
 */
@Slf4j
@Component
public class SecurityRequestDebugFilter extends OncePerRequestFilter {

    @Value("${app.security.request-debug.enabled:false}")
    private boolean requestDebugEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!requestDebugEnabled) {
            return true;
        }

        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/auth") || uri.startsWith("/api/admin") || uri.equals("/actuator/health"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String origin = safeValue(request.getHeader("Origin"));
        String referer = safeValue(request.getHeader("Referer"));
        String userAgent = abbreviate(safeValue(request.getHeader("User-Agent")), 140);
        boolean hasAuthorizationHeader = request.getHeader("Authorization") != null;
        boolean hasAccessCookie = hasCookie(request, "cg_access_token");
        boolean hasRefreshCookie = hasCookie(request, "cg_refresh_token");

        log.info(
                "[SECURITY DEBUG] IN method={} uri={} origin={} referer={} authHeader={} accessCookie={} refreshCookie={} userAgent={}",
                method,
                uri,
                origin,
                referer,
                hasAuthorizationHeader,
                hasAccessCookie,
                hasRefreshCookie,
                userAgent
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String principal = authentication == null ? "anonymous" : abbreviate(String.valueOf(authentication.getPrincipal()), 80);
            String authorities = authentication == null ? "-" : abbreviate(String.valueOf(authentication.getAuthorities()), 120);

            log.info(
                    "[SECURITY DEBUG] OUT method={} uri={} status={} durationMs={} principal={} authorities={}",
                    method,
                    uri,
                    response.getStatus(),
                    durationMs,
                    principal,
                    authorities
            );
        }
    }

    private boolean hasCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return false;
        }

        return Arrays.stream(cookies).anyMatch(cookie -> cookieName.equals(cookie.getName()));
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String abbreviate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }
}
