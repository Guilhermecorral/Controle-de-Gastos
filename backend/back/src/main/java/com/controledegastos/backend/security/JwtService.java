package com.controledegastos.backend.security;

import com.controledegastos.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gera, le e valida tokens JWT usados pela aplicacao.
 */
@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Garante que o segredo e os tempos de expiracao estejam aceitaveis antes de subir a aplicacao.
     */
    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres");
        }

        if (expiration <= 0 || refreshExpiration <= 0) {
            throw new IllegalStateException("As expiracoes do JWT precisam ser maiores que zero");
        }
    }

    /**
     * Converte o segredo configurado em chave criptografica para assinatura dos tokens.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera o token de acesso com dados suficientes para a navegacao do usuario autenticado.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        return buildToken(claims, user.getEmail(), expiration);
    }

    /**
     * Gera o token de refresh usado para renovar a sessao sem novo login imediato.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        return buildToken(claims, user.getEmail(), refreshExpiration);
    }

    /**
     * Centraliza a montagem do JWT com subject, expiracao e assinatura.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationTime) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrai o email do subject do token, que funciona como identificador do usuario.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Considera valido apenas o token que ainda nao expirou e pertence ao usuario esperado.
     */
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token) && ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    /**
     * Valida explicitamente tokens de refresh para impedir o uso do cookie errado no fluxo de renovacao.
     */
    public boolean isRefreshTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token) && REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    /**
     * Verifica se a data de expiracao do token ja passou.
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Permite que filtros lidem com tokens invalidos sem transformar toda chamada em erro 500.
     */
    public String extractEmailSafely(String token) {
        try {
            return extractEmail(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Expoe a expiracao do access token para alinhar max-age do cookie no servidor.
     */
    public long getAccessExpiration() {
        return expiration;
    }

    /**
     * Expoe a expiracao do refresh token para alinhar max-age do cookie no servidor.
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    private String extractTokenType(String token) {
        return extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    /**
     * Faz o parse completo do token e devolve suas claims apos verificar a assinatura.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
