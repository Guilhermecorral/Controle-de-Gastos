package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.QuoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketQuoteService {
    private static final Logger log = LoggerFactory.getLogger(MarketQuoteService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();
    private final Map<String, CachedRate> exchangeRateCache = new ConcurrentHashMap<>();

    @Value("${app.investments.brapi-base-url:https://brapi.dev}")
    private String brapiBaseUrl;
    @Value("${app.investments.brapi-token:}")
    private String brapiToken;
    @Value("${app.investments.yahoo-base-url:https://query1.finance.yahoo.com}")
    private String yahooBaseUrl;
    @Value("${app.investments.coingecko-base-url:https://api.coingecko.com/api/v3}")
    private String coinGeckoBaseUrl;
    @Value("${app.investments.coingecko-api-key:}")
    private String coinGeckoApiKey;
    @Value("${app.investments.quote-cache-seconds:300}")
    private long cacheSeconds;

    public QuoteResponse quote(InvestmentPosition.AssetType type, String symbol, String externalId, String market) {
        if (type == InvestmentPosition.AssetType.RENDA_FIXA) return unavailable(symbol, "PROJECAO_INTERNA");
        String key = type + ":" + market + ":" + (externalId == null ? symbol : externalId);
        CachedQuote cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.quote();
        try {
            QuoteResponse quote = type == InvestmentPosition.AssetType.CRIPTO
                    ? fetchCrypto(externalId)
                    : fetchExchangeAssetWithFallback(symbol, externalId, market);
            cache.put(key, new CachedQuote(quote, Instant.now().plusSeconds(cacheSeconds)));
            return quote;
        } catch (Exception exception) {
            log.warn("Cotacao indisponivel para {}: {}", key, exception.getMessage());
            return cached != null ? cached.quote() : unavailable(symbol, type == InvestmentPosition.AssetType.CRIPTO ? "COINGECKO" : "BRAPI");
        }
    }

    public BigDecimal exchangeRateToBrl(String rawCurrency) {
        String currency = rawCurrency == null ? "BRL" : rawCurrency.trim().toUpperCase(Locale.ROOT);
        if (currency.equals("BRL")) return BigDecimal.ONE;
        CachedRate cached = exchangeRateCache.get(currency);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.rate();
        try {
            String pair = currency.equals("USD") ? "BRL=X" : currency + "BRL=X";
            JsonNode meta = send(yahooBaseUrl + "/v8/finance/chart/" + encode(pair) + "?interval=1d&range=5d", null)
                    .path("chart").path("result").path(0).path("meta");
            BigDecimal rate = decimal(meta, "regularMarketPrice");
            if (rate == null || rate.signum() <= 0) throw new IllegalStateException("Câmbio indisponível para " + currency);
            exchangeRateCache.put(currency, new CachedRate(rate, Instant.now().plusSeconds(cacheSeconds)));
            return rate;
        } catch (Exception exception) {
            log.warn("Câmbio {}-BRL indisponível: {}", currency, exception.getMessage());
            return cached == null ? BigDecimal.ONE : cached.rate();
        }
    }

    private QuoteResponse fetchExchangeAssetWithFallback(String symbol, String externalId, String market) throws Exception {
        boolean brazilian = market == null || market.isBlank() || market.equalsIgnoreCase("BR");
        if (brazilian && brapiToken != null && !brapiToken.isBlank()) {
            try {
                return fetchBrazilianAsset(symbol);
            } catch (Exception exception) {
                log.warn("Brapi indisponivel para {}; tentando fallback de mercado", symbol);
            }
        }
        if (brazilian) {
            try {
                return fetchBrazilianAssetFromPublicList(symbol);
            } catch (Exception exception) {
                log.warn("Lista publica Brapi indisponivel para {}; tentando Yahoo Finance", symbol);
            }
        }
        String providerSymbol = externalId == null || externalId.isBlank()
                ? (brazilian ? symbol + ".SA" : symbol)
                : externalId;
        return fetchYahooAsset(symbol, providerSymbol);
    }

    private QuoteResponse fetchBrazilianAssetFromPublicList(String rawSymbol) throws Exception {
        String symbol = required(rawSymbol, "Informe o ticker do ativo").toUpperCase(Locale.ROOT);
        JsonNode stocks = send(brapiBaseUrl + "/api/quote/list?search=" + encode(symbol) + "&limit=10", null).path("stocks");
        for (JsonNode item : stocks) {
            if (!symbol.equalsIgnoreCase(item.path("stock").asText())) continue;
            BigDecimal price = decimal(item, "close");
            if (price == null) break;
            return new QuoteResponse(symbol, price, decimal(item, "change"), null,
                    "BRL", "BRAPI", Instant.now(), true);
        }
        throw new IllegalStateException("Ativo não encontrado na listagem pública");
    }

    private QuoteResponse fetchBrazilianAsset(String rawSymbol) throws Exception {
        String symbol = required(rawSymbol, "Informe o ticker do ativo").toUpperCase(Locale.ROOT);
        String uri = brapiBaseUrl + "/api/v2/stocks/quote?symbols=" + encode(symbol);
        if (brapiToken != null && !brapiToken.isBlank()) uri += "&token=" + encode(brapiToken);
        JsonNode root = send(uri, null);
        JsonNode item = root.path("stocks").isArray() ? root.path("stocks").path(0) : root.path("results").path(0);
        BigDecimal price = decimal(item, "regularMarketPrice", "price");
        if (price == null) throw new IllegalStateException("Provedor não devolveu preço para " + symbol);
        return new QuoteResponse(symbol, price, decimal(item, "regularMarketChangePercent", "change"),
                decimal(item, "dividendYield", "dy"), "BRL", "BRAPI", Instant.now(), true);
    }

    private QuoteResponse fetchCrypto(String rawExternalId) throws Exception {
        String id = required(rawExternalId, "Informe o identificador CoinGecko, como bitcoin").toLowerCase(Locale.ROOT);
        String uri = coinGeckoBaseUrl + "/simple/price?ids=" + encode(id) + "&vs_currencies=brl&include_24hr_change=true";
        JsonNode item = send(uri, coinGeckoApiKey).path(id);
        BigDecimal price = item.path("brl").isNumber() ? item.path("brl").decimalValue() : null;
        if (price == null) throw new IllegalStateException("Criptoativo não encontrado");
        return new QuoteResponse(id.toUpperCase(Locale.ROOT), price,
                item.path("brl_24h_change").isNumber() ? item.path("brl_24h_change").decimalValue() : null,
                null, "BRL", "COINGECKO", Instant.now(), true);
    }

    private QuoteResponse fetchYahooAsset(String rawSymbol, String rawProviderSymbol) throws Exception {
        String symbol = required(rawSymbol, "Informe o ticker do ativo").toUpperCase(Locale.ROOT);
        String providerSymbol = required(rawProviderSymbol, "Informe o identificador de mercado do ativo");
        JsonNode result = send(yahooBaseUrl + "/v8/finance/chart/" + encode(providerSymbol) + "?interval=1d&range=1y&events=div", null)
                .path("chart").path("result").path(0);
        JsonNode meta = result.path("meta");
        BigDecimal price = decimal(meta, "regularMarketPrice");
        if (price == null) throw new IllegalStateException("Provedor não devolveu preço para " + symbol);
        BigDecimal dividends = BigDecimal.ZERO;
        for (JsonNode dividend : result.path("events").path("dividends")) {
            if (dividend.path("amount").isNumber()) dividends = dividends.add(dividend.path("amount").decimalValue());
        }
        BigDecimal dividendYield = dividends.signum() == 0 ? null
                : dividends.multiply(new BigDecimal("100")).divide(price, 4, java.math.RoundingMode.HALF_UP);
        return new QuoteResponse(symbol, price, decimal(meta, "regularMarketChangePercent"), dividendYield,
                meta.path("currency").asText("BRL"), "YAHOO_FINANCE", Instant.now(), true);
    }

    private JsonNode send(String uri, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(7)).GET();
        if (apiKey != null && !apiKey.isBlank()) builder.header("x-cg-demo-api-key", apiKey);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("HTTP " + response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private BigDecimal decimal(JsonNode item, String... fields) {
        for (String field : fields) if (item.path(field).isNumber()) return item.path(field).decimalValue();
        return null;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private QuoteResponse unavailable(String symbol, String source) {
        return new QuoteResponse(symbol, null, null, null, "BRL", source, Instant.now(), false);
    }

    private record CachedQuote(QuoteResponse quote, Instant expiresAt) {}
    private record CachedRate(BigDecimal rate, Instant expiresAt) {}
}
