package com.controledegastos.backend.investments;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha editável da área de staging; ainda não representa uma movimentação da carteira. */
@Entity
@Table(name = "investment_import_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentImportItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id", nullable = false)
    private InvestmentImportBatch batch;
    @Column(name = "source_row", nullable = false)
    private int sourceRow;
    @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 20)
    private InvestmentMovement.MovementType movementType;
    @Enumerated(EnumType.STRING) @Column(name = "asset_type", nullable = false, length = 20)
    private InvestmentPosition.AssetType assetType;
    @Column(length = 30) private String symbol;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 10) private String market;
    @Column(length = 30) private String exchange;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 24, scale = 8) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 6) private BigDecimal unitPrice;
    @Embedded private OperationCosts costs;
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;
    @Column(name = "event_date", nullable = false) private LocalDate eventDate;
    @Column(name = "possible_duplicate", nullable = false) @Builder.Default private boolean possibleDuplicate = false;
    @Column(length = 500) private String warning;
}
