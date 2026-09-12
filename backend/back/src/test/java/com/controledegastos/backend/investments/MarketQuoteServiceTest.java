package com.controledegastos.backend.investments;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MarketQuoteServiceTest {
    @Test
    void cachesCryptoForFiveMinutesAndLimitsTheBrlPriceToEightDecimals() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/simple/price", exchange -> {
            requests.incrementAndGet();
            byte[] response = "{\"bitcoin\":{\"brl\":0.123456789,\"brl_24h_change\":1.25}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MarketQuoteService service = new MarketQuoteService();
            ReflectionTestUtils.setField(service, "coinGeckoBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "cacheSeconds", 0L);
            Instant beforeRequest = Instant.now();

            var first = service.quote(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "GLOBAL", LocalDate.now());
            var cached = service.quote(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "GLOBAL");

            assertThat(first.price()).isEqualByComparingTo("0.12345679");
            assertThat(first.price().scale()).isLessThanOrEqualTo(8);
            assertThat(cached).isEqualTo(first);
            assertThat(requests).hasValue(1);

            Map<?, ?> quoteCache = (Map<?, ?>) ReflectionTestUtils.getField(service, "cache");
            Object cachedQuote = quoteCache.values().iterator().next();
            Instant expiresAt = (Instant) ReflectionTestUtils.getField(cachedQuote, "expiresAt");
            assertThat(expiresAt).isBetween(beforeRequest.plusSeconds(299), Instant.now().plusSeconds(301));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchesHistoricalCryptoByDateAndKeepsItInLongLivedCache() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        LocalDate quoteDate = LocalDate.now().minusDays(2);
        String expectedDate = quoteDate.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/coins/bitcoin/history", exchange -> {
            requests.incrementAndGet();
            assertThat(exchange.getRequestURI().getRawQuery())
                    .isEqualTo("date=" + expectedDate + "&localization=false");
            byte[] response = "{\"market_data\":{\"current_price\":{\"brl\":345678.123456789}}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MarketQuoteService service = new MarketQuoteService();
            ReflectionTestUtils.setField(service, "coinGeckoBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            Instant beforeRequest = Instant.now();

            var first = service.quote(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "GLOBAL", quoteDate);
            var cached = service.quote(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "GLOBAL", quoteDate);

            assertThat(first.price()).isEqualByComparingTo("345678.12345679");
            assertThat(first.price().scale()).isLessThanOrEqualTo(8);
            assertThat(first.source()).isEqualTo("COINGECKO");
            assertThat(cached).isEqualTo(first);
            assertThat(requests).hasValue(1);

            Map<?, ?> quoteCache = (Map<?, ?>) ReflectionTestUtils.getField(service, "cache");
            Object cachedQuote = quoteCache.values().iterator().next();
            Instant expiresAt = (Instant) ReflectionTestUtils.getField(cachedQuote, "expiresAt");
            assertThat(expiresAt).isAfter(beforeRequest.plusSeconds(3000L * 24 * 60 * 60));
        } finally {
            server.stop(0);
        }
    }
}
