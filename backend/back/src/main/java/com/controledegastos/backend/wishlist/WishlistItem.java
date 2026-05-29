package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "wishlist_items")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    @Column(nullable = false)
    @Convert(converter = WishlistPriorityConverter.class)
    @Builder.Default
    private Priority priority = Priority.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WishlistCategory category = WishlistCategory.COMPRAS;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WishlistStatus status = WishlistStatus.PENDENTE;

    @Column
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column
    private PurchasePaymentMethod paymentMethod;

    @Column(nullable = false)
    @Builder.Default
    private Integer installments = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean firstInstallmentNextMonth = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean archivedAfterPurchase = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_list_id", nullable = false)
    private WishlistList wishlistList;

    @OneToMany(mappedBy = "wishlistItem", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WishlistHistoryEntry> historyEntries;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.finalPrice == null) {
            this.calculateFinalPrice();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.calculateFinalPrice();
    }

    public void calculateFinalPrice() {
        if (this.discountPercent == null || this.discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            this.finalPrice = this.originalPrice.setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            BigDecimal discount = this.originalPrice
                    .multiply(this.discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            this.finalPrice = this.originalPrice.subtract(discount).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public enum WishlistStatus {
        PENDENTE,
        COMPRADO
    }

    public enum PurchasePaymentMethod {
        PIX,
        CARTAO_DEBITO,
        CARTAO_CREDITO_AVISTA,
        CARTAO_CREDITO_PARCELADO,
        DINHEIRO
    }

    public enum Priority {
        ALTO,
        MEDIA,
        BAIXO
    }

    public enum WishlistCategory {
        COMPRAS, ALIMENTACAO, MORADIA, SAUDE,
        LAZER, EDUCACAO, TRANSPORTE, OUTROS
    }
}
