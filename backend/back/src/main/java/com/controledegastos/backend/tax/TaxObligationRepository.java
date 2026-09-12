package com.controledegastos.backend.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxObligationRepository extends JpaRepository<TaxObligation, UUID> {
    @EntityGraph(attributePaths = {"linkedTransaction", "linkedDarf"})
    List<TaxObligation> findByUser_IdOrderByDueDateAsc(Long userId);
    List<TaxObligation> findByUser_IdAndStatus(Long userId, TaxObligation.Status status);
    Optional<TaxObligation> findByIdAndUser_Id(UUID id, Long userId);
    boolean existsByUser_IdAndLinkedDarf_Id(Long userId, Long linkedDarfId);
}
