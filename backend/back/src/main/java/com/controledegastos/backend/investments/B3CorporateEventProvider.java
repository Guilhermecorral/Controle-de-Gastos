package com.controledegastos.backend.investments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Experimental adapter for public B3 listed-company responses. It intentionally runs serially
 * and only for assets held in a wallet. The source is not a contracted B3 API and may change.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.investments.corporate-events.provider", havingValue = "b3")
public class B3CorporateEventProvider implements MarketDataProvider {
    private static final DateTimeFormatter B3_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final String SUPPLEMENT_PATH = "/listedCompaniesProxy/CompanyCall/GetListedSupplementCompany/";

    // Jackson 2 remains an explicit library dependency while Spring Boot 4 exposes Jackson 3 beans.
    // Keep this adapter self-contained instead of requiring a legacy ObjectMapper bean from Spring.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final long minimumDelayMs;

    public B3CorporateEventProvider(
            @Value("${app.investments.corporate-events.b3-base-url:https://sistemaswebb3-listados.b3.com.br}") String baseUrl,
            @Value("${app.investments.corporate-events.b3-minimum-delay-ms:300}") long minimumDelayMs
    ) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.minimumDelayMs = Math.max(0, minimumDelayMs);
    }

    @Override
    public List<CorporateEventData> corporateEvents(LocalDate referenceDate, Set<String> symbols) {
        var symbolsByRoot = new HashMap<String, Set<String>>();
        for (String symbol : symbols) {
            String root = tickerRoot(symbol);
            if (!root.isBlank()) {
                symbolsByRoot.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).add(symbol.trim().toUpperCase(Locale.ROOT));
            }
        }
        List<String> roots = symbolsByRoot.keySet().stream().sorted().toList();
        List<CorporateEventData> events = new ArrayList<>();

        for (int index = 0; index < roots.size(); index++) {
            String root = roots.get(index);
            try {
                events.addAll(fetch(root, symbolsByRoot.get(root)));
            } catch (Exception exception) {
                log.warn("[B3_EVENTS] Unable to load corporate events for symbolRoot={} reason={}", root, exception.getMessage());
            }
            pauseBetweenRequests(index, roots.size());
        }

        return events.stream()
                .filter(event -> !event.exDate().isAfter(referenceDate))
                .sorted(Comparator.comparing(CorporateEventData::exDate).thenComparing(CorporateEventData::symbol))
                .toList();
    }

    private List<CorporateEventData> fetch(String symbolRoot, Set<String> candidateSymbols) throws Exception {
        String payload = Base64.getEncoder().encodeToString(("{\"issuingCompany\":\"" + symbolRoot + "\",\"language\":\"pt-br\"}")
                .getBytes(StandardCharsets.UTF_8));
        HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + SUPPLEMENT_PATH + payload).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "FarolFinanceiro/1.4.5-beta.1 corporate-event-pilot");

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status);
        }
        try (InputStream stream = connection.getInputStream()) {
            JsonNode root = objectMapper.readTree(stream);
            if (root.isTextual()) {
                root = objectMapper.readTree(root.asText());
            }
            return parseResponse(symbolRoot, candidateSymbols, root);
        } finally {
            connection.disconnect();
        }
    }

    static List<CorporateEventData> parseResponse(String requestedSymbol, JsonNode root) {
        return parseResponse(requestedSymbol, Set.of(requestedSymbol), root);
    }

    static List<CorporateEventData> parseResponse(String requestedRoot, Set<String> candidateSymbols, JsonNode root) {
        JsonNode events = root.path("cashDividends");
        if (!events.isArray()) {
            return List.of();
        }
        List<CorporateEventData> parsed = new ArrayList<>();
        for (JsonNode item : events) {
            CorporateEvent.EventType eventType = mapEventType(text(item, "label"));
            LocalDate recordDate = parseDate(text(item, "lastDatePrior"));
            LocalDate paymentDate = parseDate(text(item, "paymentDate"));
            BigDecimal amount = parseDecimal(text(item, "rate"));
            if (eventType == null || recordDate == null || paymentDate == null || amount.signum() <= 0) {
                continue;
            }
            String isin = text(item, "isinCode");
            String symbol = resolveSymbol(candidateSymbols, text(item, "assetIssued"), isin);
            BigDecimal taxRate = eventType == CorporateEvent.EventType.JCP ? new BigDecimal("15.0000") : BigDecimal.ZERO;
            String sourceReference = "b3:" + requestedRoot + ":" + eventType + ":" + isin + ":" + recordDate + ":" + paymentDate + ":" + amount.toPlainString();
            parsed.add(new CorporateEventData(sourceReference, symbol, isin, eventType, null, amount, taxRate,
                    recordDate, paymentDate, "B3_EXPERIMENTAL", symbol == null ? "TICKER_AMBIGUO" : "VALIDO"));
        }
        return parsed;
    }

    private static String resolveSymbol(Set<String> candidateSymbols, String assetIssued, String isin) {
        String candidate = assetIssued == null ? "" : assetIssued.trim().toUpperCase(Locale.ROOT);
        if (candidate.matches("[A-Z]{4}\\d{1,2}")) {
            // An explicit class is safe only when that exact class is held in the wallet.
            return candidateSymbols.contains(candidate) ? candidate : null;
        }
        if (candidateSymbols.size() == 1) {
            String symbol = candidateSymbols.iterator().next();
            return isUnambiguousFor(symbol, isin) ? symbol : null;
        }
        String normalizedIsin = isin == null ? "" : isin.toUpperCase(Locale.ROOT);
        if (normalizedIsin.contains("OR")) {
            return candidateSymbols.stream().filter(symbol -> symbol.endsWith("3")).findFirst().orElse(null);
        }
        if (normalizedIsin.contains("PR")) {
            return candidateSymbols.stream().filter(symbol -> symbol.endsWith("4")).findFirst()
                    .orElseGet(() -> candidateSymbols.stream().filter(symbol -> symbol.endsWith("5") || symbol.endsWith("6")).findFirst().orElse(null));
        }
        return null;
    }

    private static boolean isUnambiguousFor(String symbol, String isin) {
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String normalizedIsin = isin == null ? "" : isin.toUpperCase(Locale.ROOT);
        if (normalizedSymbol.endsWith("11")) return true;
        if (normalizedSymbol.endsWith("3")) return normalizedIsin.contains("OR");
        if (normalizedSymbol.endsWith("4")) return normalizedIsin.contains("PR");
        return false;
    }

    private static CorporateEvent.EventType mapEventType(String label) {
        String normalized = label.toUpperCase(Locale.ROOT);
        if (normalized.contains("JRS CAP PROPRIO") || normalized.contains("JUROS") && normalized.contains("CAPITAL")) {
            return CorporateEvent.EventType.JCP;
        }
        if (normalized.contains("RENDIMENTO")) {
            return CorporateEvent.EventType.RENDIMENTO;
        }
        if (normalized.contains("DIVIDENDO")) {
            return CorporateEvent.EventType.DIVIDENDO;
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim(), B3_DATE);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        String normalized = value.replaceAll("[^0-9,.-]", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.lastIndexOf(',') > normalized.lastIndexOf('.')
                    ? normalized.replace(".", "").replace(',', '.')
                    : normalized.replace(",", "");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(',', '.');
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String tickerRoot(String symbol) {
        if (symbol == null) return "";
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{4}\\d{1,2}") ? normalized.substring(0, 4) : "";
    }

    private void pauseBetweenRequests(int index, int total) {
        if (minimumDelayMs == 0 || index + 1 >= total) return;
        try {
            Thread.sleep(minimumDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
