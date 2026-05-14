package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Concentra a leitura do historico de alteracoes da wishlist.
 */
public interface WishlistHistoryEntryRepository extends JpaRepository<WishlistHistoryEntry, Long> {

    List<WishlistHistoryEntry> findAllByWishlistItemAndUserOrderByCreatedAtDesc(WishlistItem wishlistItem, User user);
}
