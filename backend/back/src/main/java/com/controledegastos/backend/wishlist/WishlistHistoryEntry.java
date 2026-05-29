package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registra eventos importantes do ciclo de vida de um item da wishlist.
 */
@Entity
@Table(name = "wishlist_history_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_item_id", nullable = false)
    private WishlistItem wishlistItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Convert(converter = WishlistHistoryActionTypeConverter.class)
    private ActionType actionType;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPriceSnapshot;

    @Column(length = 120)
    private String listNameSnapshot;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ActionType {
        CREATED,
        UPDATED,
        MOVED,
        PURCHASED,
        PURCHASE_UNDONE,
        DELETED
    }
}
