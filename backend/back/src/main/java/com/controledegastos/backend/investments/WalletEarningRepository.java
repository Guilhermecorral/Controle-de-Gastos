package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WalletEarningRepository extends JpaRepository<WalletEarning, Long> {
    List<WalletEarning> findAllByUserOrderByCorporateEventPaymentDateAscCreatedAtDesc(User user);
    Optional<WalletEarning> findByIdAndUser(Long id, User user);
    boolean existsByUserAndCorporateEvent(User user, CorporateEvent corporateEvent);

    @Query("""
            select earning.corporateEvent.sourceReference
            from WalletEarning earning
            where earning.user = :user
              and earning.corporateEvent.sourceReference in :sourceReferences
            """)
    Set<String> findSourceReferencesAlreadyInAgenda(@Param("user") User user,
                                                    @Param("sourceReferences") Set<String> sourceReferences);
}
