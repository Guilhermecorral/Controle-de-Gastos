package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletEarningRepository extends JpaRepository<WalletEarning, Long> {
    List<WalletEarning> findAllByUserOrderByCorporateEventPaymentDateAscCreatedAtDesc(User user);
    Optional<WalletEarning> findByIdAndUser(Long id, User user);
    boolean existsByUserAndCorporateEvent(User user, CorporateEvent corporateEvent);
}
