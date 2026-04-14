package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    //Description of the item the user wants to buy
    @Column(nullable = false, length = 255)
    private String description;

    //price original of product old discount
    //BigDecimal: precision exactly for money - never float/double
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    //Percent of discount - ex.: 10 significally 10% off
    //Default 0
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    //final price calculated: originalPrice * (1 - discountPercent / 100)
    //Calculated of service old of save - never through the front-end
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    //item priority on the wishlist
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIA;

    //Category - reutilization is same category's of the transaction
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WishlistCategory category = WishlistCategory.COMPRAS;

    //Free observation - ex.: "I want to buy this on Black Friday"
    @Column(length = 500)
    private String notes;

    //Item status: PENDING or PURCHASED
    //Use enum + status field instead of boolean "isBought"
    //Reason: in the future, "RESERVED","CANCELED", etc. may arise - enum scales, boolean does not
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WishlistStatus status = WishlistStatus.PENDENTE;

    //Date when the item was marked as purchased - null if still pending
    //Useful for history and future reports
    @Column
    private LocalDate purchaseDate;

    //Stores the payment method chosen when the item is purchased
    //Null while the item is still pending
    @Enumerated(EnumType.STRING)
    @Column
    private PurchasePaymentMethod paymentMethod;

    //Stores how many installments were chosen by the user
    //Defaults to 1 because one-time purchases are the default case
    @Column(nullable = false)
    @Builder.Default
    private Integer installments = 1;

    //Stores whether the first installment starts only in the next month
    //Useful when the credit-card bill closes before the purchase date
    @Column(nullable = false)
    @Builder.Default
    private Boolean firstInstallmentNextMonth = false;

    // Relationship: this item belongs to ONE user
    // @ManyToOne: many items → one user
    // LAZY: does not load the full User object when fetching an item
    // @JoinColumn: creates the "user_id" column in the wishlist_items table
    // nullable = false: every item MUST have an owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //Automatically set createdAt when the item is first saved to the database
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Automatically updated every time the item changes
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        //Ensures that the finalPrice is calculated even if the Service forgets
        if (this.finalPrice == null) {
            this.calculateFinalPrice();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.calculateFinalPrice();
    }

    //Business method within the entity - calculates the discounted price
    //Rule: finalPrice = originalPrice - (originalPrice * discountPercent / 100)
    public void calculateFinalPrice() {
        if (this.discountPercent == null || this.discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            this.finalPrice = this.originalPrice.setScale(2, java.math.RoundingMode.HALF_UP); //No discount
        } else {
            BigDecimal discount = this.originalPrice
                    .multiply(this.discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            this.finalPrice = this.originalPrice.subtract(discount).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    //Enums of module's Wishlist
    public enum WishlistStatus {
        PENDENTE, //Item not yet purchased - appears in the main list
        COMPRADO //Item purchased - appears in the "Already Purchased" section with Undo button
    }

    public enum PurchasePaymentMethod {
        PIX,
        CARTAO_DEBITO,
        CARTAO_CREDITO_AVISTA,
        CARTAO_CREDITO_PARCELADO,
        DINHEIRO
    }

    public enum Priority {
        ALTO, //Displayed in red/highligh in the prototype
        MEDIA, //Displayed in yellow - default
        BAIXO //Displayed in green/neutral
    }

    public enum WishlistCategory {
        COMPRAS, ALIMENTACAO, MORADIA, SAUDE,
        LAZER, EDUCACAO, TRANSPORTE, OUTROS
    }
}
