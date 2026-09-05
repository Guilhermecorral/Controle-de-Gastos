package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class FixedIncomeTax {
    private FixedIncomeTax() {}
    public enum Regime { REGRESSIVO, ISENTO, MANUAL }
    private static final int[] IOF = {100,96,93,90,86,83,80,76,73,70,66,63,60,56,53,50,46,43,40,36,33,30,26,23,20,16,13,10,6,3,0};
    public record Result(BigDecimal gross, BigDecimal earnings, BigDecimal incomeTax,
                         BigDecimal iof, BigDecimal net, BigDecimal rate, long days) {}

    public static Result calculate(BigDecimal principal, BigDecimal gross, LocalDate start, LocalDate end,
                                   Regime regime, BigDecimal manualRate, boolean iofApplicable) {
        if (end.isBefore(start)) throw new IllegalArgumentException("Resgate anterior a aplicacao");
        if (regime == null) throw new IllegalArgumentException("Informe o regime tributario da aplicacao");
        long days = ChronoUnit.DAYS.between(start, end);
        BigDecimal earnings = gross.subtract(principal).max(BigDecimal.ZERO);
        BigDecimal rate = regime == Regime.ISENTO ? BigDecimal.ZERO : regime == Regime.MANUAL
                ? manualRate : new BigDecimal(days <= 180 ? "22.5" : days <= 360 ? "20" : days <= 720 ? "17.5" : "15");
        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Informe uma aliquota entre 0 e 100");
        BigDecimal iof = iofApplicable && days < 30 ? money(earnings.multiply(BigDecimal.valueOf(IOF[(int) days])).movePointLeft(2)) : money(BigDecimal.ZERO);
        BigDecimal ir = money(earnings.subtract(iof).multiply(rate).movePointLeft(2));
        return new Result(money(gross), money(earnings), ir, iof, money(gross.subtract(ir).subtract(iof)), rate, days);
    }
    public static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
