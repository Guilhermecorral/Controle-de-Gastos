package com.controledegastos.backend.transactions;


import com.controledegastos.backend.wishlist.WishlistItem;
import jakarta.persistence.*;
import lombok.*;
import com.controledegastos.backend.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "installments")
    private Integer installments;

    @Column(name = "transaction_group_id")
    private UUID transactionGroupId;

    @Column(name = "receipt_original_filename", length = 255)
    private String receiptOriginalFilename;

    @Column(name = "receipt_storage_name", length = 255)
    private String receiptStorageName;

    @Column(name = "receipt_content_type", length = 120)
    private String receiptContentType;

    @Column(name = "receipt_size_bytes")
    private Long receiptSizeBytes;

    @Column(name = "receipt_uploaded_at")
    private LocalDateTime receiptUploadedAt;

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
