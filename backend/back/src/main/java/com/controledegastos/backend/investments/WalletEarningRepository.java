package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletEarningRepository extends JpaRepository<WalletEarning, Long> {
    List<WalletEarning> findAllByUserOrderByCorporateEventPaymentDateAscCreatedAtDesc(User user);
    Optional<WalletEarning> findByIdAndUser(Long id, User user);
    boolean existsByUserAndCorporateEvent(User user, CorporateEvent corporateEvent);

    @Query("""
            select earning
            from WalletEarning earning
            join fetch earning.position
            join fetch earning.corporateEvent
            where earning.user = :user
            order by earning.createdAt desc
            """)
    List<WalletEarning> findAllForPilotPreviewByUser(@Param("user") User user);
}
