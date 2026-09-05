package com.controledegastos.backend.investments;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OperationCosts {
    @DecimalMin("0") @Column(name = "brokerage_fee", precision = 19, scale = 2)
    private BigDecimal brokerageFee;
    @DecimalMin("0") @Column(name = "b3_fee", precision = 19, scale = 2)
    private BigDecimal b3Fee;
    @DecimalMin("0") @Column(name = "other_costs", precision = 19, scale = 2)
    private BigDecimal otherCosts;
    @DecimalMin("0") @Column(name = "withheld_tax", precision = 19, scale = 2)
    private BigDecimal withheldTax;

    public BigDecimal total() { return zero(brokerageFee).add(zero(b3Fee)).add(zero(otherCosts)); }
    public BigDecimal retention() { return zero(withheldTax); }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
