package com.controledegastos.backend.user;

import com.controledegastos.backend.wishlist.WishlistItem;
import com.controledegastos.backend.transactions.Transaction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") // Create table users
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

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
    private Role role = Role.USER;

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return "";
    }

    //Enum of the system's global roles
    public enum Role {
        USER,
        ADMIN
    }
}