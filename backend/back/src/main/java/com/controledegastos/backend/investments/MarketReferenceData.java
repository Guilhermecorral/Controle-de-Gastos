package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/** Contracts keep external market domains independent from portfolio and tax rules. */
public final class MarketReferenceData {
    private MarketReferenceData() {}

    public record Observation(BigDecimal value, String unit, String source, Instant observedAt) {}

    public interface ExchangeRates { Optional<Observation> rateToBrl(String currency); }
    public interface InterestRates { Optional<Observation> annualRate(String index); }
    public interface Inflation { Optional<Observation> annualRate(String index); }
    public interface CryptoPrices { Optional<Observation> price(String externalId, String currency); }
}
