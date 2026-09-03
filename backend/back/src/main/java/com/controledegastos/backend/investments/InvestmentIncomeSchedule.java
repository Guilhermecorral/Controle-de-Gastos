package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_income_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentIncomeSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private InvestmentPosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false, length = 20)
    private InvestmentMovement.MovementType incomeType;

    @Column(name = "amount_per_unit", nullable = false, precision = 19, scale = 8)
    private BigDecimal amountPerUnit;

    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "ex_date")
    private LocalDate exDate;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.AGUARDANDO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Status { AGUARDANDO, RECEBIDO }
}
