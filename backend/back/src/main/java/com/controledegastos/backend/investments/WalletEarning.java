package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_earnings", uniqueConstraints = @UniqueConstraint(name = "uk_wallet_earning_user_event", columnNames = {"user_id", "corporate_event_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEarning {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "position_id", nullable = false)
    private InvestmentPosition position;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "corporate_event_id", nullable = false)
    private CorporateEvent corporateEvent;

    @Column(name = "quantity_eligible", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantityEligible;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "withheld_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal withheldAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "investment_movement_id", unique = true)
    private Long investmentMovementId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum Status { PROVISIONADO, PENDENTE_CONCILIACAO, EFETIVADO, CANCELADO }
}
