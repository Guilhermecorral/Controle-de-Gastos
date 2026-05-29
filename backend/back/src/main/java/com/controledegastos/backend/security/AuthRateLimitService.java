package com.controledegastos.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mantém o limite de tentativas com fallback local e só usa Redis quando a infraestrutura estiver realmente disponível.
 */
@Service
public class AuthRateLimitService {

    private final Environment environment;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final Map<String, RateWindow> localAttempts = new ConcurrentHashMap<>();

    public AuthRateLimitService(
            Environment environment,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider
    ) {
        this.environment = environment;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
    }

    /**
     * Conta a tentativa no backend e informa se a chamada ainda pode prosseguir.
     */
    public boolean isAllowed(String key, long windowSeconds, int maxAttempts) {
        if (isRedisRateLimitEnabled()) {
            return isAllowedWithRedis(key, windowSeconds, maxAttempts);
        }

        return isAllowedInMemory(key, windowSeconds, maxAttempts);
    }

    private boolean isRedisRateLimitEnabled() {
        return environment.getProperty("app.security.auth-rate-limit.redis-enabled", Boolean.class, false);
    }

    private boolean isAllowedWithRedis(String key, long windowSeconds, int maxAttempts) {
        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();

        if (connectionFactory == null) {
            return isAllowedInMemory(key, windowSeconds, maxAttempts);
        }

        try {
            StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
            Long attempts = redisTemplate.opsForValue().increment(key);

            if (attempts != null && attempts == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            return attempts == null || attempts <= maxAttempts;
        } catch (Exception exception) {
            return isAllowedInMemory(key, windowSeconds, maxAttempts);
        }
    }

    private boolean isAllowedInMemory(String key, long windowSeconds, int maxAttempts) {
        long now = System.currentTimeMillis();
        RateWindow window = localAttempts.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt() <= now) {
                return new RateWindow(new AtomicInteger(1), now + (windowSeconds * 1000));
            }

            current.attempts().incrementAndGet();
            return current;
        });

        return window.attempts().get() <= maxAttempts;
    }

    /**
     * Agrupa a janela local apenas para o fallback de desenvolvimento e instância única.
     */
    private record RateWindow(AtomicInteger attempts, long expiresAt) {
    }
}
