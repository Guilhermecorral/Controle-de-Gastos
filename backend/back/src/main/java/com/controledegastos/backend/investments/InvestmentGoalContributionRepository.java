package com.controledegastos.backend.investments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentGoalContributionRepository extends JpaRepository<InvestmentGoalContribution, Long> {
    List<InvestmentGoalContribution> findAllByGoalOrderByEventDateDescCreatedAtDesc(InvestmentGoal goal);
}
