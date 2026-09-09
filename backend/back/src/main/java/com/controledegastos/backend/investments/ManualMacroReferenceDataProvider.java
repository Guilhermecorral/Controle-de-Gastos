package com.controledegastos.backend.investments;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Prevents the application from silently treating a projection input as an official macro rate.
 * A source-specific provider may replace this only after its terms, cadence and provenance are validated.
 */
@Service
public class ManualMacroReferenceDataProvider implements MarketReferenceData.InterestRates, MarketReferenceData.Inflation {
    @Override
    public Optional<MarketReferenceData.Observation> annualRate(String index) {
        return Optional.empty();
    }
}
