package com.controledegastos.backend.wishlist.Repository;

import com.controledegastos.backend.user.User;
import com.controledegastos.backend.wishlist.WishlistHistoryEntry;
import com.controledegastos.backend.wishlist.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Concentra a leitura do historico de alteracoes da wishlist.
 */
public interface WishlistHistoryEntryRepository extends JpaRepository<WishlistHistoryEntry, Long> {

    List<WishlistHistoryEntry> findAllByWishlistItemAndUserOrderByCreatedAtDesc(WishlistItem wishlistItem, User user);
}
