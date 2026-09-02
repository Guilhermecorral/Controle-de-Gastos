package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentMovementRepository extends JpaRepository<InvestmentMovement, Long> {
    List<InvestmentMovement> findAllByUserOrderByEventDateDescCreatedAtDesc(User user);
}
