package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentPortfolioSnapshotRepository extends JpaRepository<InvestmentPortfolioSnapshot, Long> {
    Optional<InvestmentPortfolioSnapshot> findByUserAndSnapshotDate(User user, LocalDate snapshotDate);
    List<InvestmentPortfolioSnapshot> findAllByUserAndSnapshotDateGreaterThanEqualOrderBySnapshotDate(User user, LocalDate startDate);
}
