package com.controledegastos.backend.investments;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AssetCatalogServiceTest {
    @Test
    void usesTheBrapiFiagroSubtypeInsteadOfTheFiiSubtype() {
        assertThat(AssetCatalogService.brapiFundSubtype(InvestmentPosition.AssetType.FII)).isEqualTo("fii");
        assertThat(AssetCatalogService.brapiFundSubtype(InvestmentPosition.AssetType.FIAGRO)).isEqualTo("fi-agro");
        assertThat(AssetCatalogService.brapiFundSubtype(InvestmentPosition.AssetType.ETF)).isEqualTo("etf");
    }

    @Test
    void keepsKnownFiagrosAvailableWhenBrapiIsUnavailable() {
        AssetCatalogService service = new AssetCatalogService();
        ReflectionTestUtils.setField(service, "brapiBaseUrl", "http://127.0.0.1:1");

        var results = service.search("RURA", InvestmentPosition.AssetType.FIAGRO);

        assertThat(results).extracting(InvestmentDtos.AssetSearchResponse::symbol).contains("RURA11");
        assertThat(results).allMatch(asset -> asset.assetType() == InvestmentPosition.AssetType.FIAGRO);
    }

    @Test
    void searchesEtfsInBrapiInsteadOfUsingOnlyTheFallbackCatalog() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/quote/list", exchange -> {
            byte[] response = "{\"stocks\":[{\"stock\":\"BOVA11\",\"name\":\"iShares Ibovespa\",\"subType\":\"etf\",\"close\":128.45}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AssetCatalogService service = new AssetCatalogService();
            ReflectionTestUtils.setField(service, "brapiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

            var results = service.search("BOVA", InvestmentPosition.AssetType.ETF);

            assertThat(results).filteredOn(asset -> asset.symbol().equals("BOVA11")).singleElement().satisfies(asset -> {
                assertThat(asset.assetType()).isEqualTo(InvestmentPosition.AssetType.ETF);
                assertThat(asset.currentPrice()).isEqualByComparingTo("128.45");
                assertThat(asset.source()).isEqualTo("BRAPI");
            });
        } finally {
            server.stop(0);
        }
    }
}
