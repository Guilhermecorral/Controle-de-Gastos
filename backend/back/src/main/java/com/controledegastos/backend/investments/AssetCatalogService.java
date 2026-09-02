package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.AssetSearchResponse;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssetCatalogService {
    private static final Logger log = LoggerFactory.getLogger(AssetCatalogService.class);
    private static final List<AssetSearchResponse> FALLBACK = List.of(
            asset(InvestmentPosition.AssetType.ACAO, "PETR4", "PETR4.SA", "Petrobras PN", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.ACAO, "ITUB4", "ITUB4.SA", "Itaú Unibanco PN", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.ACAO, "BBAS3", "BBAS3.SA", "Banco do Brasil ON", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.ACAO, "BBSE3", "BBSE3.SA", "BB Seguridade ON", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.FII, "HGLG11", "HGLG11.SA", "CSHG Logística FII", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.FII, "KNCR11", "KNCR11.SA", "Kinea Rendimentos Imobiliários FII", "BR", "B3", "BRL"),
            asset(InvestmentPosition.AssetType.ACAO, "AAPL", "AAPL", "Apple Inc.", "US", "NASDAQ", "USD"),
            asset(InvestmentPosition.AssetType.ACAO, "MSFT", "MSFT", "Microsoft Corporation", "US", "NASDAQ", "USD"),
            asset(InvestmentPosition.AssetType.ACAO, "NVDA", "NVDA", "NVIDIA Corporation", "US", "NASDAQ", "USD"),
            asset(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "Bitcoin", "GLOBAL", "CRYPTO", "BRL"),
            asset(InvestmentPosition.AssetType.CRIPTO, "ETH", "ethereum", "Ethereum", "GLOBAL", "CRYPTO", "BRL")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    @Value("${app.investments.yahoo-base-url:https://query1.finance.yahoo.com}")
    private String yahooBaseUrl;
    @Value("${app.investments.brapi-base-url:https://brapi.dev}")
    private String brapiBaseUrl;
    @Value("${app.investments.brapi-token:}")
    private String brapiToken;
    @Value("${app.investments.coingecko-base-url:https://api.coingecko.com/api/v3}")
    private String coinGeckoBaseUrl;
    @Value("${app.investments.coingecko-api-key:}")
    private String coinGeckoApiKey;

    public List<AssetSearchResponse> search(String rawQuery, InvestmentPosition.AssetType type) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2 || type == InvestmentPosition.AssetType.RENDA_FIXA) return List.of();

        Map<String, AssetSearchResponse> results = new LinkedHashMap<>();
        if (type == InvestmentPosition.AssetType.CRIPTO) {
            safelySearch(() -> searchCrypto(query), results, "CoinGecko");
        } else {
            safelySearch(() -> searchBrazilianAssets(query, type), results, "Brapi");
            if (type == InvestmentPosition.AssetType.ACAO) {
                safelySearch(() -> searchExchangeAssets(query, type), results, "Yahoo Finance");
            }
        }

        String normalized = normalizeSearch(query);
        FALLBACK.stream()
                .filter(item -> item.assetType() == type)
                .filter(item -> normalizeSearch(item.symbol()).contains(normalized) || normalizeSearch(item.name()).contains(normalized))
                .forEach(item -> results.putIfAbsent(identity(item), item));
        return results.values().stream().limit(12).toList();
    }

    private List<AssetSearchResponse> searchBrazilianAssets(String query, InvestmentPosition.AssetType type) throws Exception {
        String uri = brapiBaseUrl + "/api/quote/list?search=" + encode(query) + "&limit=12"
                + (type == InvestmentPosition.AssetType.FII ? "&type=fund&subType=fii" : "&type=stock");
        if (brapiToken != null && !brapiToken.isBlank()) uri += "&token=" + encode(brapiToken);
        JsonNode stocks = send(uri, null).path("stocks");
        List<AssetSearchResponse> results = new ArrayList<>();
        for (JsonNode stock : stocks) {
            String symbol = stock.path("stock").asText("").toUpperCase(Locale.ROOT);
            if (symbol.isBlank() || symbol.matches(".*\\dF$")) continue;
            BigDecimal price = stock.path("close").isNumber() ? stock.path("close").decimalValue() : null;
            results.add(new AssetSearchResponse(type, symbol, symbol + ".SA", stock.path("name").asText(symbol),
                    "BR", "B3", "BRL", price, "BRAPI"));
        }
        return results;
    }

    private List<AssetSearchResponse> searchExchangeAssets(String query, InvestmentPosition.AssetType type) throws Exception {
        JsonNode quotes = send(yahooBaseUrl + "/v1/finance/search?q=" + encode(query) + "&quotesCount=16&newsCount=0", null).path("quotes");
        List<AssetSearchResponse> results = new ArrayList<>();
        for (JsonNode quote : quotes) {
            String providerSymbol = quote.path("symbol").asText("").toUpperCase(Locale.ROOT);
            String quoteType = quote.path("quoteType").asText("");
            if (providerSymbol.isBlank() || !(quoteType.equals("EQUITY") || quoteType.equals("ETF"))) continue;
            boolean brazilian = providerSymbol.endsWith(".SA");
            if (!brazilian && !isUnitedStatesExchange(quote.path("exchange").asText(""))) continue;
            if (type == InvestmentPosition.AssetType.FII && !brazilian) continue;

            String symbol = brazilian ? providerSymbol.substring(0, providerSymbol.length() - 3) : providerSymbol;
            String name = quote.path("longname").asText(quote.path("shortname").asText(symbol));
            String exchange = brazilian ? "B3" : quote.path("exchDisp").asText(quote.path("exchange").asText("EUA"));
            String currency = quote.path("currency").asText(brazilian ? "BRL" : "USD");
            BigDecimal price = quote.path("regularMarketPrice").isNumber() ? quote.path("regularMarketPrice").decimalValue() : null;
            results.add(new AssetSearchResponse(type, symbol, providerSymbol, name, brazilian ? "BR" : "US",
                    exchange, currency, price, "YAHOO_FINANCE"));
        }
        return results;
    }

    private List<AssetSearchResponse> searchCrypto(String query) throws Exception {
        JsonNode coins = send(coinGeckoBaseUrl + "/search?query=" + encode(query), coinGeckoApiKey).path("coins");
        List<AssetSearchResponse> results = new ArrayList<>();
        for (JsonNode coin : coins) {
            String id = coin.path("id").asText("");
            String symbol = coin.path("symbol").asText("").toUpperCase(Locale.ROOT);
            if (id.isBlank() || symbol.isBlank()) continue;
            results.add(new AssetSearchResponse(InvestmentPosition.AssetType.CRIPTO, symbol, id,
                    coin.path("name").asText(symbol), "GLOBAL", "CRYPTO", "BRL", null, "COINGECKO"));
            if (results.size() == 12) break;
        }
        return results;
    }

    private JsonNode send(String uri, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(7)).GET();
        if (apiKey != null && !apiKey.isBlank()) builder.header("x-cg-demo-api-key", apiKey);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("HTTP " + response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private boolean isUnitedStatesExchange(String exchange) {
        return List.of("NMS", "NGM", "NCM", "NYQ", "ASE", "PCX", "BTS").contains(exchange);
    }

    private String identity(AssetSearchResponse item) {
        return item.assetType() + ":" + item.market() + ":" + item.externalId();
    }

    private void safelySearch(SearchSupplier supplier, Map<String, AssetSearchResponse> results, String provider) {
        try {
            supplier.get().forEach(item -> results.put(identity(item), item));
        } catch (Exception exception) {
            log.warn("Busca de ativos no {} indisponível: {}", provider, exception.getMessage());
        }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private String normalizeSearch(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }

    private static AssetSearchResponse asset(InvestmentPosition.AssetType type, String symbol, String externalId,
                                             String name, String market, String exchange, String currency) {
        return new AssetSearchResponse(type, symbol, externalId, name, market, exchange, currency, null, "CATALOGO_LOCAL");
    }

    @FunctionalInterface
    private interface SearchSupplier { List<AssetSearchResponse> get() throws Exception; }
}
