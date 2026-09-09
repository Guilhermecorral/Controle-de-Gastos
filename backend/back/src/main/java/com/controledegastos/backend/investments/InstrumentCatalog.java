package com.controledegastos.backend.investments;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical identity and product capabilities for instruments supported by the portfolio.
 * A ticker is a display/trading identifier and must never be replaced by its ISIN.
 */
public final class InstrumentCatalog {
    public enum InstrumentClass { ON, PN, UNT, FUND_SHARE, BDR, ETF, CRYPTO, FIXED_INCOME, UNKNOWN }

    public record Capabilities(boolean supportsQuote, boolean supportsHistoricalQuote, boolean supportsCorporateEvents,
                               boolean supportsAutomaticSchedule, boolean supportsFractionalQuantity,
                               boolean requiresDerivativeLedger) {}

    public record Instrument(String symbol, String isinCode, String issuer, InstrumentClass instrumentClass,
                             InvestmentPosition.AssetType assetType, Capabilities capabilities) {}

    private static final Map<String, Instrument> KNOWN = Map.ofEntries(
            entry("PETR4", "BRPETRACNPR6", "Petrobras", InstrumentClass.PN, InvestmentPosition.AssetType.ACAO),
            entry("BBAS3", "BRBBASA04OR8", "Banco do Brasil", InstrumentClass.ON, InvestmentPosition.AssetType.ACAO),
            entry("BBDC3", null, "Banco Bradesco", InstrumentClass.ON, InvestmentPosition.AssetType.ACAO),
            entry("BBDC4", null, "Banco Bradesco", InstrumentClass.PN, InvestmentPosition.AssetType.ACAO),
            entry("MXRF11", null, "Maxi Renda", InstrumentClass.FUND_SHARE, InvestmentPosition.AssetType.FII),
            entry("RURA11", null, "Itaú Asset Rural", InstrumentClass.FUND_SHARE, InvestmentPosition.AssetType.FIAGRO),
            entry("BOVA11", null, "iShares Ibovespa", InstrumentClass.ETF, InvestmentPosition.AssetType.ETF),
            entry("IVVB11", null, "iShares S&P 500", InstrumentClass.ETF, InvestmentPosition.AssetType.ETF),
            entry("AAPL34", null, "Apple Inc.", InstrumentClass.BDR, InvestmentPosition.AssetType.BDR),
            entry("MSFT34", null, "Microsoft Corporation", InstrumentClass.BDR, InvestmentPosition.AssetType.BDR)
    );

    private InstrumentCatalog() {}

    public static Optional<Instrument> find(String symbol, InvestmentPosition.AssetType requestedType) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        Instrument instrument = KNOWN.get(symbol.trim().toUpperCase(Locale.ROOT));
        return instrument != null && instrument.assetType() == requestedType ? Optional.of(instrument) : Optional.empty();
    }

    public static Capabilities capabilitiesFor(InvestmentPosition.AssetType type) {
        return switch (type) {
            case ACAO -> new Capabilities(true, true, true, true, false, false);
            case FII, FIAGRO -> new Capabilities(true, false, true, true, true, false);
            case BDR, ETF -> new Capabilities(true, true, false, false, true, false);
            case CRIPTO -> new Capabilities(true, true, false, false, true, false);
            case RENDA_FIXA -> new Capabilities(false, false, false, false, true, false);
        };
    }

    private static Map.Entry<String, Instrument> entry(String symbol, String isinCode, String issuer,
                                                        InstrumentClass instrumentClass, InvestmentPosition.AssetType type) {
        return Map.entry(symbol, new Instrument(symbol, isinCode, issuer, instrumentClass, type, capabilitiesFor(type)));
    }
}
