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

    public static void applyToSystemProperties() {
        Map<String, Object> resolved = resolve();
        resolved.forEach((key, value) -> {
            if (value != null) {
                System.setProperty(key, value.toString());
            }
        });
    }

    private static void applyDataSourceDefaults(Map<String, String> env, Map<String, Object> defaults) {
        String dataSourceUrl = resolvePreferredDataSourceUrl(env);
        ParsedJdbcSettings parsedSettings = normalizePostgresJdbcSettings(dataSourceUrl);
        String normalizedUrl = parsedSettings.url();
        boolean supabasePoolerConnection = isSupabasePoolerUrl(normalizedUrl);

        if (hasText(normalizedUrl)) {
            defaults.put("SPRING_DATASOURCE_URL", normalizedUrl);
            defaults.put("spring.datasource.url", normalizedUrl);
        }

        String username = resolveDataSourceUsername(env, parsedSettings, normalizedUrl, supabasePoolerConnection);
        if (hasText(username)) {
            defaults.put("SPRING_DATASOURCE_USERNAME", username);
            defaults.put("spring.datasource.username", username);
        }

        String password = resolveDataSourcePassword(env, parsedSettings, supabasePoolerConnection);
        if (hasText(password)) {
            defaults.put("SPRING_DATASOURCE_PASSWORD", password);
            defaults.put("spring.datasource.password", password);
        }
    }

    private static String resolvePreferredDataSourceUrl(Map<String, String> env) {
        String configuredUrl = sanitizeConnectionCandidate(firstNonBlank(
                env.get("SPRING_DATASOURCE_URL"),
                env.get("DATASOURCE_URL"),
                env.get("DATABASE_URL"),
                env.get("SUPABASE_DATABASE_URL"),
                env.get("SUPABASE_URL"),
                env.get("PROJECT_URL"),
                env.get("SUPABASE_PROJECT_URL")
        ));
        String supabasePoolerUrl = sanitizeConnectionCandidate(env.get("SUPABASE_POOLER_URL"));

        if (hasText(supabasePoolerUrl) && shouldPreferSupabasePooler(configuredUrl)) {
            return supabasePoolerUrl;
        }

        return hasText(configuredUrl) ? configuredUrl : supabasePoolerUrl;
    }

    private static String resolveDataSourceUsername(
            Map<String, String> env,
            ParsedJdbcSettings parsedSettings,
            String normalizedUrl,
            boolean supabasePoolerConnection
    ) {
        if (hasText(parsedSettings.username())) {
            return parsedSettings.username();
        }

        if (supabasePoolerConnection) {
            String explicitSupabaseUsername = firstNonBlank(
                    env.get("SPRING_DATASOURCE_USERNAME"),
                    env.get("DATASOURCE_USERNAME"),
                    env.get("SUPABASE_DB_USERNAME"),
                    env.get("SUPABASE_DATABASE_USERNAME"),
                    env.get("SUPABASE_POOLER_USERNAME"),
                    env.get("SUPABASE_POOLER_USER")
            );
            if (hasText(explicitSupabaseUsername)) {
                return explicitSupabaseUsername;
            }

            String supabaseProjectRef = resolveSupabaseProjectRef(env, normalizedUrl);
            if (hasText(supabaseProjectRef)) {
                return "postgres." + supabaseProjectRef;
            }
            return null;
        }

        return firstNonBlank(
                env.get("SPRING_DATASOURCE_USERNAME"),
                env.get("DATASOURCE_USERNAME"),
                env.get("DATABASE_USERNAME"),
                env.get("POSTGRES_USER")
        );
    }

    private static String resolveDataSourcePassword(
            Map<String, String> env,
            ParsedJdbcSettings parsedSettings,
            boolean supabasePoolerConnection
    ) {
        if (hasText(parsedSettings.password())) {
            return parsedSettings.password();
        }

        if (supabasePoolerConnection) {
            return firstNonBlank(
                    env.get("SPRING_DATASOURCE_PASSWORD"),
                    env.get("DATASOURCE_PASSWORD"),
                    env.get("SUPABASE_DB_PASSWORD"),
                    env.get("SUPABASE_DATABASE_PASSWORD"),
                    env.get("SUPABASE_POOLER_PASSWORD"),
                    env.get("SUPABASE_POOLER_PASS"),
                    env.get("DATABASE_PASSWORD"),
                    env.get("POSTGRES_PASSWORD")
            );
        }

        return firstNonBlank(
                env.get("SPRING_DATASOURCE_PASSWORD"),
                env.get("DATASOURCE_PASSWORD"),
                env.get("DATABASE_PASSWORD"),
                env.get("POSTGRES_PASSWORD")
        );
    }

    private static void applyRedisDefaults(Map<String, String> env, Map<String, Object> defaults) {
        if (hasText(env.get("SPRING_DATA_REDIS_HOST")) && hasText(env.get("SPRING_DATA_REDIS_PORT"))) {
            defaults.put("management.health.redis.enabled", "true");
            return;
        }

        String redisUrl = env.get("REDIS_URL");
        if (!hasText(redisUrl)) {
            defaults.put("management.health.redis.enabled", "false");
            defaults.putIfAbsent("app.security.auth-rate-limit.redis-enabled", "false");
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

            defaults.put("management.health.redis.enabled", "true");
        } catch (IllegalArgumentException ignored) {
            // Se a URL do Redis vier malformada, deixamos a falha aparecer no startup.
        }
    }

    private static ParsedJdbcSettings normalizePostgresJdbcSettings(String rawUrl) {
        String sanitizedRawUrl = sanitizeConnectionCandidate(rawUrl);
        if (!hasText(sanitizedRawUrl)) {
            return new ParsedJdbcSettings(sanitizedRawUrl, null, null);
        }

        String jdbcUrl = sanitizedRawUrl;
        if (sanitizedRawUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:" + sanitizedRawUrl;
        } else if (sanitizedRawUrl.startsWith("postgres://")) {
            jdbcUrl = "jdbc:postgresql://" + sanitizedRawUrl.substring("postgres://".length());
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

    private static String sanitizeConnectionCandidate(String value) {
        if (!hasText(value)) {
            return value;
        }

        String sanitized = value.trim()
                .replace("`", "")
                .replace("\"", "")
                .replace("'", "");

        while ((sanitized.startsWith("[") && sanitized.endsWith("]"))
                || (sanitized.startsWith("(") && sanitized.endsWith(")"))) {
            sanitized = sanitized.substring(1, sanitized.length() - 1).trim();
        }

        if (sanitized.startsWith("[") || sanitized.endsWith("]")) {
            sanitized = sanitized.replace("[", "").replace("]", "");
        }

        return sanitized;
    }

    private static boolean shouldPreferSupabasePooler(String configuredUrl) {
        if (!hasText(configuredUrl)) {
            return true;
        }

        String normalizedUrl = configuredUrl.toLowerCase();
        return normalizedUrl.contains("supabase.co") && !normalizedUrl.contains("pooler.supabase.com");
    }

    private static boolean isSupabasePoolerUrl(String jdbcUrl) {
        if (!hasText(jdbcUrl) || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            return false;
        }

        String host = extractJdbcHost(jdbcUrl);
        return hasText(host) && host.contains("pooler.supabase.com");
    }

    private static String resolveSupabaseProjectRef(Map<String, String> env, String jdbcUrl) {
        String explicitProjectRef = firstNonBlank(
                env.get("SUPABASE_PROJECT_REF"),
                env.get("PROJECT_REF")
        );
        if (hasText(explicitProjectRef)) {
            return sanitizeSupabaseProjectRef(explicitProjectRef);
        }

        String supabaseProjectUrl = firstNonBlank(
                env.get("SUPABASE_URL"),
                env.get("PROJECT_URL"),
                env.get("SUPABASE_PROJECT_URL")
        );
        if (hasText(supabaseProjectUrl)) {
            String extracted = extractProjectRefFromSupabaseUrl(sanitizeConnectionCandidate(supabaseProjectUrl));
            if (hasText(extracted)) {
                return extracted;
            }
        }

        String jdbcHost = extractJdbcHost(jdbcUrl);
        if (hasText(jdbcHost) && jdbcHost.startsWith("db.") && jdbcHost.endsWith(".supabase.co")) {
            return jdbcHost.substring("db.".length(), jdbcHost.length() - ".supabase.co".length());
        }

        return null;
    }

    private static String extractProjectRefFromSupabaseUrl(String url) {
        if (!hasText(url)) {
            return null;
        }

        try {
            URI uri = URI.create(url.contains("://") ? url : "https://" + url);
            String host = uri.getHost();
            if (!hasText(host)) {
                return null;
            }

            if (host.endsWith(".supabase.co")) {
                return sanitizeSupabaseProjectRef(host.substring(0, host.length() - ".supabase.co".length()));
            }
        } catch (IllegalArgumentException ignored) {
            // Se a URL estiver malformada, apenas tentamos o fallback manual.
        }

        return null;
    }

    private static String extractJdbcHost(String jdbcUrl) {
        if (!hasText(jdbcUrl) || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            return null;
        }

        try {
            URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
            return uri.getHost();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private static String sanitizeSupabaseProjectRef(String value) {
        String sanitized = sanitizeConnectionCandidate(value);
        if (!hasText(sanitized)) {
            return sanitized;
        }

        return sanitized.replace("postgres.", "")
                .replace("https://", "")
                .replace("http://", "");
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
