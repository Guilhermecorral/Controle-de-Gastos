package com.controledegastos.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
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
        String dataSourceUrl = resolvePreferredDataSourceUrl(env);
        ParsedJdbcSettings parsedSettings = normalizePostgresJdbcSettings(dataSourceUrl);
        String normalizedUrl = parsedSettings.url();

        if (hasText(normalizedUrl)) {
            defaults.put("SPRING_DATASOURCE_URL", normalizedUrl);
            defaults.put("spring.datasource.url", normalizedUrl);
        }

        String username = firstNonBlank(
                env.get("SPRING_DATASOURCE_USERNAME"),
                parsedSettings.username(),
                env.get("DATABASE_USERNAME"),
                env.get("POSTGRES_USER")
        );
        if (hasText(username)) {
            defaults.put("SPRING_DATASOURCE_USERNAME", username);
            defaults.put("spring.datasource.username", username);
        }

        String password = firstNonBlank(
                env.get("SPRING_DATASOURCE_PASSWORD"),
                parsedSettings.password(),
                env.get("DATABASE_PASSWORD"),
                env.get("POSTGRES_PASSWORD")
        );
        if (hasText(password)) {
            defaults.put("SPRING_DATASOURCE_PASSWORD", password);
            defaults.put("spring.datasource.password", password);
        }
    }

    private static String resolvePreferredDataSourceUrl(Map<String, String> env) {
        String configuredUrl = firstNonBlank(
                env.get("SPRING_DATASOURCE_URL"),
                env.get("DATABASE_URL"),
                env.get("SUPABASE_DATABASE_URL")
        );
        String supabasePoolerUrl = env.get("SUPABASE_POOLER_URL");

        if (hasText(supabasePoolerUrl) && shouldPreferSupabasePooler(configuredUrl)) {
            return supabasePoolerUrl;
        }

        return hasText(configuredUrl) ? configuredUrl : supabasePoolerUrl;
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

    private static ParsedJdbcSettings normalizePostgresJdbcSettings(String rawUrl) {
        if (!hasText(rawUrl)) {
            return new ParsedJdbcSettings(rawUrl, null, null);
        }

        String jdbcUrl = rawUrl;
        if (rawUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:" + rawUrl;
        } else if (rawUrl.startsWith("postgres://")) {
            jdbcUrl = "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }

        ParsedJdbcSettings parsed = extractJdbcUserInfo(jdbcUrl);
        String sslAwareUrl = ensureSupabaseSslMode(parsed.url());
        return new ParsedJdbcSettings(sslAwareUrl, parsed.username(), parsed.password());
    }

    private static ParsedJdbcSettings extractJdbcUserInfo(String jdbcUrl) {
        if (!hasText(jdbcUrl) || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            return new ParsedJdbcSettings(jdbcUrl, null, null);
        }

        String candidate = jdbcUrl.substring("jdbc:".length());
        try {
            URI uri = new URI(candidate);
            String userInfo = uri.getUserInfo();
            if (!hasText(userInfo)) {
                return new ParsedJdbcSettings(jdbcUrl, null, null);
            }

            String[] credentials = userInfo.split(":", 2);
            String username = credentials.length > 0 ? credentials[0] : null;
            String password = credentials.length > 1 ? credentials[1] : null;

            URI sanitizedUri = new URI(
                    uri.getScheme(),
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );

            return new ParsedJdbcSettings("jdbc:" + sanitizedUri, username, password);
        } catch (URISyntaxException ignored) {
            return new ParsedJdbcSettings(jdbcUrl, null, null);
        }
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

    private static boolean shouldPreferSupabasePooler(String configuredUrl) {
        if (!hasText(configuredUrl)) {
            return true;
        }

        String normalizedUrl = configuredUrl.toLowerCase();
        return normalizedUrl.contains("supabase.co") && !normalizedUrl.contains("pooler.supabase.com");
    }

    private static String ensureSupabaseSslMode(String jdbcUrl) {
        if (!hasText(jdbcUrl) || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            return jdbcUrl;
        }

        if (!jdbcUrl.contains("supabase.co")) {
            return jdbcUrl;
        }

        String lowerCaseUrl = jdbcUrl.toLowerCase();
        if (lowerCaseUrl.contains("sslmode=") || lowerCaseUrl.contains("ssl=true")) {
            return jdbcUrl;
        }

        return jdbcUrl.contains("?")
                ? jdbcUrl + "&sslmode=require"
                : jdbcUrl + "?sslmode=require";
    }

    private record ParsedJdbcSettings(String url, String username, String password) {
    }
}
