package com.controledegastos.backend.wishlist; // Declares the package for wishlist data access.

import com.controledegastos.backend.user.User; // Imports the user entity because every query is scoped to the authenticated user.
import org.springframework.data.domain.Sort; // Imports the sort abstraction used for dynamic ordering.
import org.springframework.data.jpa.repository.JpaRepository; // Imports the base JPA repository contract.
import org.springframework.data.jpa.repository.Query; // Imports the custom query annotation used by summary projections.
import org.springframework.data.repository.query.Param; // Imports the named parameter annotation used by custom queries.
import org.springframework.stereotype.Repository; // Marks the interface as a Spring repository.

import java.math.BigDecimal; // Imports the money type used by summary queries.
import java.util.List; // Imports the list type used by repository methods.
import java.util.Optional; // Imports the optional type used by owner-safe lookups.

@Repository // Registers the interface as a Spring-managed repository bean.
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> { // Declares the repository responsible for wishlist persistence.

    Optional<WishlistItem> findByIdAndUser(Long id, User user); // Loads one wishlist item only if it belongs to the informed user.

    List<WishlistItem> findAllByUser(User user, Sort sort); // Loads all wishlist items of the informed user using the requested ordering.

    List<WishlistItem> findAllByUserAndStatus(User user, WishlistItem.WishlistStatus status, Sort sort); // Loads wishlist items of one status using the requested ordering.

    long countByUserAndStatus(User user, WishlistItem.WishlistStatus status); // Counts items of one status for the summary section.

    @Query("SELECT COALESCE(SUM(w.finalPrice), 0) FROM WishlistItem w WHERE w.user = :user AND w.status = :status") // Sums final prices by status for the summary section.
    BigDecimal sumFinalPriceByUserAndStatus( // Declares the summary query that sums wishlist values by status.
            @Param("user") User user, // Filters the query by the authenticated user.
            @Param("status") WishlistItem.WishlistStatus status // Filters the query by the requested wishlist status.
    ); // Closes the method declaration.
} // Closes the repository interface.
