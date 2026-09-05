package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import static com.controledegastos.backend.investments.FixedIncomeTax.money;

public final class MonthlyTaxCalculator {
    private MonthlyTaxCalculator() {}
    public record Bucket(BigDecimal result, BigDecimal exemptProfit, BigDecimal lossUsed,
                         BigDecimal remainingLoss, BigDecimal tax, BigDecimal creditUsed, BigDecimal remainingCredit) {}
    public static Bucket calculate(BigDecimal result, BigDecimal sales, BigDecimal openingLoss,
                                   BigDecimal credit, BigDecimal rate, BigDecimal exemptionLimit) {
        boolean exempt = exemptionLimit != null && sales.compareTo(exemptionLimit) <= 0;
        BigDecimal profit = result.max(BigDecimal.ZERO);
        BigDecimal used = exempt ? BigDecimal.ZERO : profit.min(openingLoss);
        BigDecimal loss = openingLoss.subtract(used).add(result.min(BigDecimal.ZERO).abs());
        BigDecimal tax = exempt ? BigDecimal.ZERO : money(profit.subtract(used).multiply(rate).movePointLeft(2));
        BigDecimal usedCredit = credit.min(tax);
        return new Bucket(money(result), money(exempt ? profit : BigDecimal.ZERO), money(used), money(loss),
                money(tax.subtract(usedCredit)), money(usedCredit), money(credit.subtract(usedCredit)));
    }
}
