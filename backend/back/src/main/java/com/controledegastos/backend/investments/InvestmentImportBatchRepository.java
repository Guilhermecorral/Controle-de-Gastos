package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestmentImportBatchRepository extends JpaRepository<InvestmentImportBatch, Long> {
    Optional<InvestmentImportBatch> findByIdAndUser(Long id, User user);
}
