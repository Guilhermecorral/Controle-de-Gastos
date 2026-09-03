package com.controledegastos.backend.investments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentGoalContributionRepository extends JpaRepository<InvestmentGoalContribution, Long> {
    List<InvestmentGoalContribution> findAllByGoalOrderByEventDateDescCreatedAtDesc(InvestmentGoal goal);
    Optional<InvestmentGoalContribution> findByIdAndGoal(Long id, InvestmentGoal goal);
}
