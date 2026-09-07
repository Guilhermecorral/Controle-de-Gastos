package com.controledegastos.backend.investments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
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
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
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
                List<CorporateEventData> rootEvents = fetch(root, symbolsByRoot.get(root));
                events.addAll(rootEvents);
                log.info("[B3_EVENTS] root={} candidates={}", root, rootEvents.size());
            } catch (Exception exception) {
                log.warn("[B3_EVENTS] Unable to load corporate events for symbolRoot={} reason={}", root, exception.getMessage());
            }
            pauseBetweenRequests(index, roots.size());
        }

        List<CorporateEventData> eligibleEvents = sortEligibleEvents(referenceDate, events);
        log.info("[B3_EVENTS] symbols={} parsed={} eligibleAsOf={}", symbols.size(), events.size(), eligibleEvents.size());
        return eligibleEvents;
    }

    static List<CorporateEventData> sortEligibleEvents(LocalDate referenceDate, List<CorporateEventData> events) {
        return events.stream()
                .filter(event -> !event.exDate().isAfter(referenceDate))
                // Unresolved ON/PN classes are intentionally returned as "Revisar" in the pilot.
                // They must not prevent confirmed tickers from appearing in the same response.
                .sorted(Comparator.comparing(CorporateEventData::exDate)
                        .thenComparing(CorporateEventData::symbol, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<CorporateEventData> fetch(String symbolRoot, Set<String> candidateSymbols) throws Exception {
        String payload = Base64.getEncoder().encodeToString(("{\"issuingCompany\":\"" + symbolRoot + "\",\"language\":\"pt-br\"}")
                .getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + SUPPLEMENT_PATH + payload))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "FarolFinanceiro/1.4.5-beta.1 corporate-event-pilot")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status + " da B3");
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.isTextual()) {
            root = objectMapper.readTree(root.asText());
        }
        return parseResponse(symbolRoot, candidateSymbols, root);
    }

    static List<CorporateEventData> parseResponse(String requestedSymbol, JsonNode root) {
        return parseResponse(requestedSymbol, Set.of(requestedSymbol), root);
    }

    static List<CorporateEventData> parseResponse(String requestedRoot, Set<String> candidateSymbols, JsonNode root) {
        // The public B3 endpoint wraps company data in an array, while fixtures and older
        // responses may provide the company object directly. Support both representations.
        if (root.isArray()) {
            List<CorporateEventData> parsedCompanies = new ArrayList<>();
            for (JsonNode company : root) {
                parsedCompanies.addAll(parseResponse(requestedRoot, candidateSymbols, company));
            }
            return parsedCompanies;
        }
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
            String sourceReference = sourceReference(requestedRoot, symbol, eventType, isin, recordDate, paymentDate, amount);
            parsed.add(new CorporateEventData(sourceReference, symbol, isin, eventType, null, amount, taxRate,
                    recordDate, paymentDate, "B3_EXPERIMENTAL", symbol == null ? "TICKER_AMBIGUO" : "VALIDO"));
        }
        return deduplicateEquivalentEvents(parsed);
    }

    private static String sourceReference(String requestedRoot, String symbol, CorporateEvent.EventType eventType,
                                          String isin, LocalDate recordDate, LocalDate paymentDate, BigDecimal amount) {
        // The B3 payload may repeat one economic event under technical ISIN variants.
        // A resolved ticker lets us keep a stable reference without creating repeated agenda entries.
        String identity = symbol == null || symbol.isBlank() ? requestedRoot + ":" + isin : symbol;
        return "b3:" + identity + ":" + eventType + ":" + recordDate + ":" + paymentDate + ":" + amount.toPlainString();
    }

    private static List<CorporateEventData> deduplicateEquivalentEvents(List<CorporateEventData> events) {
        var distinct = new LinkedHashMap<String, CorporateEventData>();
        for (CorporateEventData event : events) {
            String key = event.symbol() == null
                    ? event.sourceReference()
                    : event.symbol() + ":" + event.eventType() + ":" + event.exDate() + ":" + event.paymentDate()
                    + ":" + event.amountPerUnit().stripTrailingZeros().toPlainString() + ":" + event.taxRate().stripTrailingZeros().toPlainString();
            distinct.putIfAbsent(key, event);
        }
        return List.copyOf(distinct.values());
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
