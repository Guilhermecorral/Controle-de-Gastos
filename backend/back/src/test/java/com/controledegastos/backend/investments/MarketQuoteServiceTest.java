package com.controledegastos.backend.investments;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

            var first = service.quote(InvestmentPosition.AssetType.CRIPTO, "BTC", "bitcoin", "GLOBAL");
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
}
