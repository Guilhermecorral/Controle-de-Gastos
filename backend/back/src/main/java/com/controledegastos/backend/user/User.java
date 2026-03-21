package com.controledegastos.backend.user;


import com.controledegastos.backend.Transactions.Transaction;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users") // Create table users
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // Key primary
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //unique = false: two users they can have the same display name
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    //Role to user in global system
    // USER = user default - ADMIN = access administrative
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Transaction.TransactionType transactionType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationship: a User has many Transactions
    // mappedBy = "user":tells JPA that the "user" field inside Transaction owns the relationship
    // Cascade REMOVE: if you delete the user, it deletes all transactions as well
    // fetch LAZY: does Not load transactions automatically - only when you request them
    // LAZY is always the right choice for large collections (avoids N+1 queries)
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    // Same relationship for the wishlist - a User has many WishlistItems
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<WishlistItem> wishlistItems;

    // Automatically set createdAt when the user is first saved to the database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    //Enum of the system's global roles
    public enum Role {
        USER,
        ADMIN
    }
}