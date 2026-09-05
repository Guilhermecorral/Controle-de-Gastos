package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static com.controledegastos.backend.investments.FixedIncomeTax.money;
import static com.controledegastos.backend.investments.InvestmentDtos.*;

public final class FixedIncomeProjection {
    private FixedIncomeProjection() {}
    private static final class Lot {
        final LocalDate date;
        final BigDecimal principal;
        BigDecimal balance;
        Lot(LocalDate date, BigDecimal principal) { this.date = date; this.principal = principal; this.balance = principal; }
    }
    public static ProjectionResponse calculate(BigDecimal initial, BigDecimal contribution, BigDecimal rate,
            RatePeriod ratePeriod, TimelinePeriod timelinePeriod, LocalDate start, LocalDate end,
            FixedIncomeTax.Regime regime, BigDecimal manualRate, boolean iofApplicable) {
        if (end.isAfter(start.plusYears(100))) throw new IllegalArgumentException("O periodo maximo e de 100 anos");
        double monthly = ratePeriod == RatePeriod.MONTHLY ? rate.doubleValue() / 100 : Math.pow(1 + rate.doubleValue() / 100, 1.0 / 12) - 1;
        List<Lot> lots = new ArrayList<>();
        lots.add(new Lot(start, initial));
        List<ProjectionPoint> points = new ArrayList<>();
        BigDecimal invested = initial, interest = BigDecimal.ZERO, periodInterest = BigDecimal.ZERO, periodContribution = BigDecimal.ZERO;
        BigDecimal ir = BigDecimal.ZERO, iof = BigDecimal.ZERO, balance = initial;
        LocalDate previous = start;
        int month = 0;
        while (previous.isBefore(end)) {
            month++;
            LocalDate anniversary = start.plusMonths(month);
            LocalDate date = anniversary.isAfter(end) ? end : anniversary;
            double fraction = (double) ChronoUnit.DAYS.between(previous, date) / ChronoUnit.DAYS.between(previous, anniversary);
            BigDecimal factor = BigDecimal.valueOf(Math.pow(1 + monthly, fraction));
            BigDecimal earned = BigDecimal.ZERO;
            for (Lot lot : lots) {
                BigDecimal next = lot.balance.multiply(factor);
                earned = earned.add(next.subtract(lot.balance));
                lot.balance = next;
            }
            if (date.equals(anniversary) && contribution.signum() > 0) {
                lots.add(new Lot(date, contribution));
                invested = invested.add(contribution);
                periodContribution = periodContribution.add(contribution);
            }
            interest = interest.add(earned);
            periodInterest = periodInterest.add(earned);
            balance = invested.add(interest);
            ir = BigDecimal.ZERO;
            iof = BigDecimal.ZERO;
            for (Lot lot : lots) {
                var tax = FixedIncomeTax.calculate(lot.principal, lot.balance, lot.date, date, regime, manualRate, iofApplicable);
                ir = ir.add(tax.incomeTax()); iof = iof.add(tax.iof());
            }
            if (timelinePeriod == TimelinePeriod.MONTHLY || month % 12 == 0 || date.equals(end)) {
                points.add(new ProjectionPoint(month, date, money(periodContribution), money(periodInterest), money(invested),
                        money(interest), money(balance), money(ir), money(iof), money(balance.subtract(ir).subtract(iof))));
                periodContribution = BigDecimal.ZERO; periodInterest = BigDecimal.ZERO;
            }
            previous = date;
        }
        return new ProjectionResponse(money(initial), money(contribution), rate, ratePeriod, timelinePeriod,
                BigDecimal.valueOf(monthly * 100), money(invested), money(balance), money(interest), month, points,
                "Taxa constante. Aportes no aniversario mensal, ao fim do periodo. Dias parciais proporcionais ao intervalo mensal. Impostos estimados por aporte como se resgatado na data exibida; nao sao descontos mensais. Sem taxas ou inflacao.",
                money(ir), money(iof), money(balance.subtract(ir).subtract(iof)));
    }
}
