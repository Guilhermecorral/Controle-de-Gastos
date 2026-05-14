package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.user.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Concentra as consultas de persistencia da wishlist.
 */
@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByIdAndUser(Long id, User user);

    List<WishlistItem> findAllByUser(User user, Sort sort);

    List<WishlistItem> findAllByUserAndStatus(User user, WishlistItem.WishlistStatus status, Sort sort);

    long countByUserAndStatus(User user, WishlistItem.WishlistStatus status);

    /**
     * Soma o valor final dos itens de um status especifico para montar o resumo da wishlist.
     */
    @Query("SELECT COALESCE(SUM(w.finalPrice), 0) FROM WishlistItem w WHERE w.user = :user AND w.status = :status")
    BigDecimal sumFinalPriceByUserAndStatus(
            @Param("user") User user,
            @Param("status") WishlistItem.WishlistStatus status
    );
}
