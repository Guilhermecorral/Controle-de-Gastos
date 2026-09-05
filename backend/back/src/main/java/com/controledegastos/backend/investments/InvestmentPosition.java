package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_positions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(length = 30)
    private String symbol;

    @Column(name = "external_id", length = 80)
    private String externalId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String market = "BR";

    @Column(length = 30)
    private String exchange;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BRL";

    @Column(nullable = false, length = 120)
    private String name;

    @Column(precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", precision = 19, scale = 6)
    private BigDecimal averagePrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", precision = 8, scale = 4)
    private BigDecimal annualRate;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING) @Column(name = "tax_regime", length = 20)
    private FixedIncomeTax.Regime taxRegime;
    @Column(name = "manual_tax_rate", precision = 8, scale = 4)
    private BigDecimal manualTaxRate;
    @Column(name = "iof_applicable", nullable = false)
    private boolean iofApplicable;
    @Column(name = "opening_date")
    private LocalDate openingDate;
    @Column(nullable = false)
    private boolean redeemed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AssetType { ACAO, FII, CRIPTO, RENDA_FIXA }
}
