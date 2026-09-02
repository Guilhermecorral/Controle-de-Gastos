package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_movements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "position_id", nullable = false)
    private InvestmentPosition position;
    @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(precision = 24, scale = 8)
    private BigDecimal quantity;
    @Column(name = "unit_price", precision = 19, scale = 6)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    @Column(nullable = false)
    private boolean automatic;
    @Column(name = "external_reference", length = 120)
    private String externalReference;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum MovementType { COMPRA, VENDA, APORTE, RESGATE, DIVIDENDO, RENDIMENTO }
}
