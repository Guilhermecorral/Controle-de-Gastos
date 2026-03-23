package com.controledegastos.backend.security;

import com.controledegastos.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    //@Value: inject value of application.yaml automatically
    //Spring reads jwt.secret and puts it in this variable at initialization
    @Value("${JWT_SECRET}")
    private String secret;

    @Value("${JWT_ACCESS_EXPIRATION}")
    private long expiration;

    @Value("${JWT_REFRESH_EXPIRATION}")
    private long refreshExpiration;

    //Convert the .env string into a secure cryptographic key
    //HMAC-SHA256 it's the signing algorithm - industry standard for JWT
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generated access Token
    // User completed for extraction the information
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        return buildToken(claims, user.getEmail(), expiration);
    }

    // Generated refresh Token
    // Contains less information
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getEmail(), refreshExpiration);
    }

    // Method private construction a token JWT with parameters provided
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationTime) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    // Extraction email (subject) inside the token
    // Used by the JwtFilter for identify the user
    public String extractEmail(String token) {
        return  extractAllClaims(token).getSubject();
    }

    // Verify token is valid for user specific
    // Two conditions: email extracted from token must be the same as user email and token must not be expired
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    // Verify if token passed is date expired
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    //Performs the completed parsing of the token - validates the signature and returns all claims
    //If the signature is invalid or the token has been tampered with, it automatically throws an exception
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
