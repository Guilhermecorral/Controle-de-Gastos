package com.controledegastos.backend.investments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketDataSnapshotRepository extends JpaRepository<MarketDataSnapshot, Long> {
    Optional<MarketDataSnapshot> findTopBySourceAndRequestKeyOrderByFetchedAtDesc(String source, String requestKey);
}
