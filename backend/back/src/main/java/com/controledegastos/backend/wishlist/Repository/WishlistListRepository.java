package com.controledegastos.backend.wishlist.Repository;

import com.controledegastos.backend.user.User;
import com.controledegastos.backend.wishlist.WishlistList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Concentra o acesso a persistencia das listas nomeadas da wishlist.
 */
public interface WishlistListRepository extends JpaRepository<WishlistList, Long> {

    List<WishlistList> findAllByUserOrderByIsDefaultDescCreatedAtAsc(User user);

    Optional<WishlistList> findByIdAndUser(Long id, User user);

    Optional<WishlistList> findByUserAndIsDefaultTrue(User user);

    List<WishlistList> findAllByUserAndIsDefaultTrueOrderByCreatedAtAsc(User user);

    boolean existsByUserAndNameIgnoreCase(User user, String name);
}
