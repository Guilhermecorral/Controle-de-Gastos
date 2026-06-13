package com.controledegastos.backend.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normaliza variaveis de ambiente de provedores como Render e Supabase para as
 * chaves esperadas pelo Spring Boot em producao.
 */
public final class RuntimeEnvironmentDefaults {

    private RuntimeEnvironmentDefaults() {
    }

    public static Map<String, Object> resolve() {
        Map<String, String> env = System.getenv();
        Map<String, Object> defaults = new HashMap<>();

        applyDataSourceDefaults(env, defaults);
        applyRedisDefaults(env, defaults);

        return defaults;
    }

    private static void applyDataSourceDefaults(Map<String, String> env, Map<String, Object> defaults) {
        String dataSourceUrl = firstNonBlank(
                env.get("SPRING_DATASOURCE_URL"),
                env.get("DATABASE_URL")
        );
        String normalizedUrl = normalizePostgresJdbcUrl(dataSourceUrl);

        if (hasText(normalizedUrl)) {
            defaults.put("SPRING_DATASOURCE_URL", normalizedUrl);
            defaults.put("spring.datasource.url", normalizedUrl);
        }

        String username = firstNonBlank(
                env.get("SPRING_DATASOURCE_USERNAME"),
                env.get("DATABASE_USERNAME"),
                env.get("POSTGRES_USER")
        );
        if (hasText(username)) {
            defaults.put("SPRING_DATASOURCE_USERNAME", username);
            defaults.put("spring.datasource.username", username);
        }

        String password = firstNonBlank(
                env.get("SPRING_DATASOURCE_PASSWORD"),
                env.get("DATABASE_PASSWORD"),
                env.get("POSTGRES_PASSWORD")
        );
        if (hasText(password)) {
            defaults.put("SPRING_DATASOURCE_PASSWORD", password);
            defaults.put("spring.datasource.password", password);
        }
    }

    private static void applyRedisDefaults(Map<String, String> env, Map<String, Object> defaults) {
        if (hasText(env.get("SPRING_DATA_REDIS_HOST")) && hasText(env.get("SPRING_DATA_REDIS_PORT"))) {
            return;
        }

        String redisUrl = env.get("REDIS_URL");
        if (!hasText(redisUrl)) {
            return;
        }

        try {
            URI uri = URI.create(redisUrl);
            if (hasText(uri.getHost())) {
                defaults.put("SPRING_DATA_REDIS_HOST", uri.getHost());
                defaults.put("spring.data.redis.host", uri.getHost());
            }

            int port = uri.getPort();
            if (port > 0) {
                defaults.put("SPRING_DATA_REDIS_PORT", Integer.toString(port));
                defaults.put("spring.data.redis.port", Integer.toString(port));
            }
        } catch (IllegalArgumentException ignored) {
            // Se a URL do Redis vier malformada, deixamos a falha aparecer no startup.
        }
    }

    private static String normalizePostgresJdbcUrl(String rawUrl) {
        if (!hasText(rawUrl)) {
            return rawUrl;
        }

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("postgresql://")) {
            return "jdbc:" + rawUrl;
        }

        if (rawUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }

        return rawUrl;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
