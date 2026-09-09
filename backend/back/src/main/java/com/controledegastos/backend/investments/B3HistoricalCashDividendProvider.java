package com.controledegastos.backend.investments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Reads the B3 cash-dividend history in bounded pages. It is intentionally not an Agenda provider:
 * historical records without a payment date are evidence for review, never automatic forecasts.
 */
@Service
@RequiredArgsConstructor
public class B3HistoricalCashDividendProvider {
    private static final String SOURCE = "B3_CASH_DIVIDENDS";
    private static final String INITIAL_COMPANIES_PATH = "/listedCompaniesProxy/CompanyCall/GetInitialCompanies/";
    private static final String PATH = "/listedCompaniesProxy/CompanyCall/GetListedCashDividends/";
    private static final int PAGE_SIZE = 120;
    private static final int MAX_PAGES = 50;
    private static final DateTimeFormatter B3_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final MarketDataSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Value("${app.investments.corporate-events.b3-base-url:https://sistemaswebb3-listados.b3.com.br}")
    private String baseUrl;
    @Value("${app.investments.corporate-events.history-cache-hours:24}")
    private long cacheHours;

    public List<HistoricalCashDividend> history(InstrumentCatalog.Instrument instrument) {
        if (instrument.assetType() != InvestmentPosition.AssetType.ACAO || instrument.isinCode() == null || instrument.isinCode().isBlank()) {
            return List.of();
        }
        String tradingName = resolveTradingName(instrument);
        List<HistoricalCashDividend> history = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<HistoricalCashDividend> current = loadPage(instrument, tradingName, page);
            history.addAll(current);
            if (current.size() < PAGE_SIZE) break;
        }
        return history.stream().distinct().toList();
    }

    private List<HistoricalCashDividend> loadPage(InstrumentCatalog.Instrument instrument, String tradingName, int page) {
        String requestKey = instrument.symbol() + ":" + page;
        Instant now = Instant.now();
        String payload = snapshotRepository.findTopBySourceAndRequestKeyOrderByFetchedAtDesc(SOURCE, requestKey)
                .filter(snapshot -> snapshot.getExpiresAt().isAfter(now))
                .map(MarketDataSnapshot::getPayload)
                .orElseGet(() -> fetchAndCache(PATH, requestKey, cashDividendRequest(tradingName, page), now));
        try {
            return parsePage(instrument.symbol(), instrument.isinCode(), objectMapper.readTree(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível interpretar o histórico de proventos da B3", exception);
        }
    }

    private String resolveTradingName(InstrumentCatalog.Instrument instrument) {
        String requestKey = "catalog:" + instrument.symbol();
        Instant now = Instant.now();
        String payload = snapshotRepository.findTopBySourceAndRequestKeyOrderByFetchedAtDesc(SOURCE, requestKey)
                .filter(snapshot -> snapshot.getExpiresAt().isAfter(now))
                .map(MarketDataSnapshot::getPayload)
                .orElseGet(() -> fetchAndCache(INITIAL_COMPANIES_PATH, requestKey, initialCompaniesRequest(instrument), now));
        try {
            JsonNode results = objectMapper.readTree(payload).path("results");
            if (!results.isArray()) throw new IllegalStateException("Catálogo B3 sem resultados");
            String issuingCompany = tickerRoot(instrument.symbol());
            for (JsonNode item : results) {
                if (!issuingCompany.equalsIgnoreCase(text(item, "issuingCompany"))) continue;
                String tradingName = text(item, "tradingName");
                if (!tradingName.isBlank()) return tradingName.replace("/", "").replace(".", "");
            }
            throw new IllegalStateException("A B3 não retornou tradingName para " + instrument.symbol());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível resolver o identificador B3 de " + instrument.symbol(), exception);
        }
    }

    private String fetchAndCache(String endpoint, String requestKey, String requestJson, Instant now) {
        try {
            String encoded = Base64.getEncoder().encodeToString(requestJson.getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + endpoint + encoded))
                    .timeout(Duration.ofSeconds(15)).header("Accept", "application/json")
                    .header("User-Agent", "FarolFinanceiro/1.4.5 corporate-event-history").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " da B3");
            }
            String payload = response.body();
            snapshotRepository.save(MarketDataSnapshot.builder().source(SOURCE).requestKey(requestKey)
                    .payload(payload).payloadHash(sha256(payload)).fetchedAt(now)
                    .expiresAt(now.plus(Duration.ofHours(Math.max(1, cacheHours)))).build());
            return payload;
        } catch (Exception exception) {
            throw new IllegalStateException("Fonte B3 indisponível", exception);
        }
    }

    private String initialCompaniesRequest(InstrumentCatalog.Instrument instrument) {
        return "{\"language\":\"pt-br\",\"pageNumber\":1,\"pageSize\":20,\"company\":\""
                + tickerRoot(instrument.symbol()) + "\"}";
    }

    private String cashDividendRequest(String tradingName, int page) {
        return "{\"language\":\"pt-br\",\"pageNumber\":" + page + ",\"pageSize\":" + PAGE_SIZE
                + ",\"tradingName\":\"" + tradingName.replace("\"", "") + "\"}";
    }

    static List<HistoricalCashDividend> parsePage(String symbol, JsonNode root) {
        return parsePage(symbol, null, root);
    }

    static List<HistoricalCashDividend> parsePage(String symbol, String expectedIsin, JsonNode root) {
        JsonNode items = root.path("results");
        if (!items.isArray()) items = root.path("cashDividends");
        if (!items.isArray()) return List.of();
        List<HistoricalCashDividend> records = new ArrayList<>();
        for (JsonNode item : items) {
            CorporateEvent.EventType type = eventType(firstText(item, "corporateAction", "label"));
            // The historical B3 endpoint calls the last eligible session "lastDatePriorEx".
            LocalDate exDate = date(firstText(item, "lastDatePriorEx", "lastDatePrior"));
            BigDecimal amount = decimal(firstText(item, "valueCash", "rate"));
            if (type == null || exDate == null || amount.signum() <= 0) continue;
            String isin = text(item, "isinCode");
            if (expectedIsin != null && !expectedIsin.equalsIgnoreCase(isin)) continue;
            LocalDate paymentDate = date(text(item, "paymentDate"));
            records.add(new HistoricalCashDividend(symbol, isin, type, amount, exDate, paymentDate,
                    "b3-history:" + symbol + ":" + isin + ":" + type + ":" + exDate + ":" + amount.stripTrailingZeros().toPlainString(), SOURCE));
        }
        return records;
    }

    private static String text(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private static String firstText(JsonNode item, String... fields) {
        for (String field : fields) {
            String value = text(item, field);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static CorporateEvent.EventType eventType(String label) {
        String value = label.toUpperCase(Locale.ROOT);
        if (value.contains("JRS CAP PROPRIO") || value.contains("JUROS") && value.contains("CAPITAL")) return CorporateEvent.EventType.JCP;
        if (value.contains("DIVIDENDO")) return CorporateEvent.EventType.DIVIDENDO;
        if (value.contains("RENDIMENTO")) return CorporateEvent.EventType.RENDIMENTO;
        return null;
    }

    private static LocalDate date(String value) {
        try {
            return value.isBlank() ? null : LocalDate.parse(value, B3_DATE);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private static String tickerRoot(String symbol) {
        String normalized = AssetResolver.normalizeSymbol(symbol);
        return normalized.matches("[A-Z]{4}\\d{1,2}") ? normalized.substring(0, 4) : normalized;
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        String normalized = value.replaceAll("[^0-9,.-]", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.lastIndexOf(',') > normalized.lastIndexOf('.') ? normalized.replace(".", "").replace(',', '.') : normalized.replace(",", "");
        } else if (normalized.contains(",")) normalized = normalized.replace(',', '.');
        try { return new BigDecimal(normalized); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private static String sha256(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    public record HistoricalCashDividend(String symbol, String isinCode, CorporateEvent.EventType eventType,
                                         BigDecimal amountPerUnit, LocalDate exDate, LocalDate paymentDate,
                                         String sourceReference, String source) {}
}
