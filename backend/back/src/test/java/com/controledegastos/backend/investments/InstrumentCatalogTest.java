package com.controledegastos.backend.investments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstrumentCatalogTest {
    @Test
    void keepsTickerIsinIssuerAndShareClassAsSeparateFields() {
        var ordinary = AssetResolver.resolve("BBAS3", "BRBBASA04OR8", InvestmentPosition.AssetType.ACAO).orElseThrow();
        var preferred = InstrumentCatalog.find("PETR4", InvestmentPosition.AssetType.ACAO).orElseThrow();

        assertThat(ordinary.symbol()).isEqualTo("BBAS3");
        assertThat(ordinary.isinCode()).isEqualTo("BRBBASA04OR8");
        assertThat(ordinary.instrumentClass()).isEqualTo(InstrumentCatalog.InstrumentClass.ON);
        assertThat(preferred.symbol()).isEqualTo("PETR4");
        assertThat(preferred.instrumentClass()).isEqualTo(InstrumentCatalog.InstrumentClass.PN);
        assertThat(AssetResolver.resolve("BBAS3", "BRPETRACNPR6", InvestmentPosition.AssetType.ACAO)).isEmpty();
    }

    @Test
    void registersBdrsAndEtfsWithoutCorporateEventCapabilities() {
        var bdr = InstrumentCatalog.find("AAPL34", InvestmentPosition.AssetType.BDR).orElseThrow();
        var etf = InstrumentCatalog.find("BOVA11", InvestmentPosition.AssetType.ETF).orElseThrow();

        assertThat(bdr.capabilities().supportsQuote()).isTrue();
        assertThat(bdr.capabilities().supportsCorporateEvents()).isFalse();
        assertThat(etf.capabilities().supportsAutomaticSchedule()).isFalse();
        assertThat(AssetResolver.supportsCorporateEvents(InvestmentPosition.AssetType.BDR)).isFalse();
        assertThat(AssetResolver.supportsCorporateEvents(InvestmentPosition.AssetType.ETF)).isFalse();
    }
}
