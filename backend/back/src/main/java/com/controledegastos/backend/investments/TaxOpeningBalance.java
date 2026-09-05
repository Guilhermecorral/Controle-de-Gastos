package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "tax_opening_balances", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor
public class TaxOpeningBalance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal commonLoss;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal dayTradeLoss;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal fundLoss;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal commonCredit;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal dayTradeCredit;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal pendingTax;
    @Column(nullable = false, length = 255) private String source;
}
