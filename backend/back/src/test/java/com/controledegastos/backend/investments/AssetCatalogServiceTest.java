package com.controledegastos.backend.investments;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AssetCatalogServiceTest {
    @Test
    void usesTheBrapiFiagroSubtypeInsteadOfTheFiiSubtype() {
        assertThat(AssetCatalogService.brapiFundSubtype(InvestmentPosition.AssetType.FII)).isEqualTo("fii");
        assertThat(AssetCatalogService.brapiFundSubtype(InvestmentPosition.AssetType.FIAGRO)).isEqualTo("fi-agro");
    }

    @Test
    void keepsKnownFiagrosAvailableWhenBrapiIsUnavailable() {
        AssetCatalogService service = new AssetCatalogService();
        ReflectionTestUtils.setField(service, "brapiBaseUrl", "http://127.0.0.1:1");

        var results = service.search("RURA", InvestmentPosition.AssetType.FIAGRO);

        assertThat(results).extracting(InvestmentDtos.AssetSearchResponse::symbol).contains("RURA11");
        assertThat(results).allMatch(asset -> asset.assetType() == InvestmentPosition.AssetType.FIAGRO);
    }
}
