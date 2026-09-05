package com.controledegastos.backend.investments;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class MonthlyTaxCalculatorTest {
    private BigDecimal n(String value) { return new BigDecimal(value); }
    @Test void exemptionIncludesTwentyThousandAndPreservesPriorLosses() {
        var result = MonthlyTaxCalculator.calculate(n("800"), n("20000"), n("500"), n("2"), n("15"), n("20000"));
        assertThat(result.tax()).isZero(); assertThat(result.remainingLoss()).isEqualByComparingTo("500");
        assertThat(result.remainingCredit()).isEqualByComparingTo("2");
    }
    @Test void offsetsLossThenWithheldCredit() {
        var result = MonthlyTaxCalculator.calculate(n("800"), n("20001"), n("500"), n("2"), n("15"), n("20000"));
        assertThat(result.tax()).isEqualByComparingTo("43");
        assertThat(result.lossUsed()).isEqualByComparingTo("500");
        assertThat(result.remainingLoss()).isZero();
    }
    @Test void fundsHaveNoSalesExemptionAndLossesAccumulate() {
        assertThat(MonthlyTaxCalculator.calculate(n("10"), n("20"), n("0"), n("0"), n("20"), null).tax()).isEqualByComparingTo("2");
        assertThat(MonthlyTaxCalculator.calculate(n("-200"), n("2000"), n("500"), n("0"), n("15"), n("20000")).remainingLoss()).isEqualByComparingTo("700");
    }
}
