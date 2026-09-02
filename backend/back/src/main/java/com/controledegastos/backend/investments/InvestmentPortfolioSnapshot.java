package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "investment_portfolio_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_investment_snapshot_user_date", columnNames = {"user_id", "snapshot_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPortfolioSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "invested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "current_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "income_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal incomeAmount;
}
