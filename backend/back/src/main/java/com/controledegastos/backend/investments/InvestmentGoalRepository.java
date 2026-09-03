package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentGoalRepository extends JpaRepository<InvestmentGoal, Long> {
    List<InvestmentGoal> findAllByUserAndActiveTrueOrderByCreatedAtDesc(User user);
    Optional<InvestmentGoal> findByIdAndUser(Long id, User user);
}
