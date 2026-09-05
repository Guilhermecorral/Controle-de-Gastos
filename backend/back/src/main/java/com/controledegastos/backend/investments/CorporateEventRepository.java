package com.controledegastos.backend.investments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorporateEventRepository extends JpaRepository<CorporateEvent, Long> {
    Optional<CorporateEvent> findBySourceReference(String sourceReference);
}
