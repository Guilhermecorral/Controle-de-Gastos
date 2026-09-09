package com.controledegastos.backend.investments;

import java.util.Locale;
import java.util.Optional;

/** Resolves an exact ticker/ISIN pair without guessing between share classes. */
public final class AssetResolver {
    private AssetResolver() {}

    public static Optional<InstrumentCatalog.Instrument> resolve(String symbol, String isinCode,
                                                                  InvestmentPosition.AssetType type) {
        Optional<InstrumentCatalog.Instrument> known = InstrumentCatalog.find(symbol, type);
        if (known.isEmpty()) return Optional.empty();
        String expectedIsin = known.get().isinCode();
        if (expectedIsin == null || isinCode == null || isinCode.isBlank()) return known;
        return expectedIsin.equalsIgnoreCase(isinCode.trim()) ? known : Optional.empty();
    }

    public static boolean supportsCorporateEvents(InvestmentPosition.AssetType type) {
        return InstrumentCatalog.capabilitiesFor(type).supportsCorporateEvents();
    }

    public static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }
}
