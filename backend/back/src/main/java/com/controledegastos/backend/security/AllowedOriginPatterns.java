package com.controledegastos.backend.security;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Expande a whitelist CORS para aceitar com consistencia as variantes apex e www do mesmo dominio.
 */
public final class AllowedOriginPatterns {

    private AllowedOriginPatterns() {
    }

    public static List<String> expand(String configuredOrigins) {
        Set<String> expandedOrigins = new LinkedHashSet<>();

        Arrays.stream(configuredOrigins.split(","))
                .map(AllowedOriginPatterns::normalize)
                .filter(origin -> !origin.isBlank())
                .forEach(origin -> {
                    expandedOrigins.add(origin);
                    addOriginVariantIfNeeded(expandedOrigins, origin);
                });

        return List.copyOf(expandedOrigins);
    }

    private static void addOriginVariantIfNeeded(Set<String> expandedOrigins, String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            if (scheme == null || host == null || host.isBlank()) {
                return;
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.equals("localhost")
                    || normalizedHost.equals("127.0.0.1")
                    || normalizedHost.equals("0.0.0.0")
                    || normalizedHost.contains(":")) {
                return;
            }

            String variantHost = normalizedHost.startsWith("www.")
                    ? normalizedHost.substring(4)
                    : "www." + normalizedHost;

            String variantOrigin = scheme.toLowerCase(Locale.ROOT) + "://" + variantHost;
            if (port > 0) {
                variantOrigin += ":" + port;
            }

            expandedOrigins.add(variantOrigin);
        } catch (IllegalArgumentException ignored) {
            // Ignora valores malformados para nao quebrar o boot por conta de um item isolado.
        }
    }

    public static String normalize(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
