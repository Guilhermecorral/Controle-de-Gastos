package com.controledegastos.backend.investments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentImportItemRepository extends JpaRepository<InvestmentImportItem, Long> {
    List<InvestmentImportItem> findAllByBatchOrderBySourceRowAsc(InvestmentImportBatch batch);
}
