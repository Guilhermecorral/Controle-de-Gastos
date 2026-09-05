package com.controledegastos.backend.investments;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporate_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporateEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "payer_cnpj", length = 18)
    private String payerCnpj;

    @Column(name = "amount_per_unit", nullable = false, precision = 19, scale = 8)
    private BigDecimal amountPerUnit;

    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "ex_date", nullable = false)
    private LocalDate exDate;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "source_reference", nullable = false, unique = true, length = 120)
    private String sourceReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum EventType { DIVIDENDO, JCP }
}
