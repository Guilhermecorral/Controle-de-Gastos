package com.controledegastos.backend.investments;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static com.controledegastos.backend.investments.InvestmentDtos.*;

class FixedIncomeTaxTest {
    private final LocalDate start = LocalDate.of(2026, 1, 1);
    @Test void appliesAllRegressiveBoundariesToEarningsOnly() {
        int[] days = {180,181,360,361,720,721};
        String[] tax = {"22.50","20.00","20.00","17.50","17.50","15.00"};
        for (int i=0;i<days.length;i++) {
            var result = FixedIncomeTax.calculate(new BigDecimal("1000"), new BigDecimal("1100"), start, start.plusDays(days[i]), FixedIncomeTax.Regime.REGRESSIVO, null, true);
            assertThat(result.incomeTax()).isEqualByComparingTo(tax[i]);
            assertThat(result.net()).isEqualByComparingTo(new BigDecimal("1100").subtract(new BigDecimal(tax[i])));
        }
    }
    @Test void deductsIofBeforeIncomeTaxAndStopsOnDayThirty() {
        var first = FixedIncomeTax.calculate(new BigDecimal("1000"), new BigDecimal("1100"), start, start.plusDays(1), FixedIncomeTax.Regime.REGRESSIVO, null, true);
        assertThat(first.iof()).isEqualByComparingTo("96");
        assertThat(first.incomeTax()).isEqualByComparingTo("0.90");
        assertThat(first.net()).isEqualByComparingTo("1003.10");
        assertThat(FixedIncomeTax.calculate(new BigDecimal("1000"), new BigDecimal("1100"), start, start.plusDays(30), FixedIncomeTax.Regime.REGRESSIVO, null, true).iof()).isZero();
    }
    @Test void doesNotTaxLossesAndKeepsIofIndependentOfIrExemption() {
        assertThat(FixedIncomeTax.calculate(new BigDecimal("1000"), new BigDecimal("900"), start, start.plusDays(1), FixedIncomeTax.Regime.REGRESSIVO, null, true).net()).isEqualByComparingTo("900");
        var exempt = FixedIncomeTax.calculate(new BigDecimal("1000"), new BigDecimal("1100"), start, start.plusDays(1), FixedIncomeTax.Regime.ISENTO, null, true);
        assertThat(exempt.incomeTax()).isZero(); assertThat(exempt.iof()).isEqualByComparingTo("96");
    }
    @Test void projectsExactEndDateAndIndividualContributionAges() {
        var result = FixedIncomeProjection.calculate(new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("1"), RatePeriod.MONTHLY, TimelinePeriod.MONTHLY,
                start, start.plusYears(1).plusDays(1), FixedIncomeTax.Regime.REGRESSIVO, null, true);
        assertThat(result.timeline().getLast().date()).isEqualTo(start.plusYears(1).plusDays(1));
        assertThat(result.totalInvested()).isEqualByComparingTo("13000");
        assertThat(result.iof()).isPositive();
        assertThat(result.incomeTax()).isGreaterThan(result.projectedEarnings().subtract(result.iof()).multiply(new BigDecimal("0.175")));
        var noContribution = FixedIncomeProjection.calculate(new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1"), RatePeriod.MONTHLY, TimelinePeriod.MONTHLY,
                start, start.plusYears(1).plusDays(1), FixedIncomeTax.Regime.REGRESSIVO, null, true);
        assertThat(noContribution.projectedBalance()).isGreaterThan(new BigDecimal("1126.83"));
    }
    @Test void shortSimulationDoesNotAccrueAnEntireMonthOrAddAFutureContribution() {
        var result = FixedIncomeProjection.calculate(new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("1"), RatePeriod.MONTHLY, TimelinePeriod.MONTHLY,
                start, start.plusDays(5), FixedIncomeTax.Regime.REGRESSIVO, null, true);
        assertThat(result.totalInvested()).isEqualByComparingTo("1000");
        assertThat(result.projectedBalance()).isBetween(new BigDecimal("1001"), new BigDecimal("1002"));
        assertThat(result.timeline().getLast().date()).isEqualTo(start.plusDays(5));
    }
}
