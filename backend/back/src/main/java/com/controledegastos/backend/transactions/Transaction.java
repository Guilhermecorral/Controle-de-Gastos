package com.controledegastos.backend.transactions;


import com.controledegastos.backend.wishlist.WishlistItem;
import jakarta.persistence.*;
import lombok.*;
import com.controledegastos.backend.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_item_id")
    private WishlistItem wishlistItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


    public enum TransactionType {
        RECEITA,
        DESPESA
    }

    public enum TransactionCategory {
        ALIMENTACAO,
        TRANSPORTE,
        MORADIA,
        SAUDE,
        LAZER,
        EDUCACAO,
        COMPRAS,
        OUTROS
    }

    public enum PaymentMethod {
        PIX,
        CARTAO_DEBITO,
        CARTAO_CREDITO_AVISTA,
        CARTAO_CREDITO_PARCELADO,
        DINHEIRO
    }
}
