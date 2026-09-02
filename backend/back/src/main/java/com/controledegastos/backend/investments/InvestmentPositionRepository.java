package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, Long> {
    List<InvestmentPosition> findAllByUserOrderByCreatedAtDesc(User user);
    Optional<InvestmentPosition> findByIdAndUser(Long id, User user);
    Optional<InvestmentPosition> findFirstByUserAndAssetTypeAndMarketAndSymbolIgnoreCase(
            User user, InvestmentPosition.AssetType assetType, String market, String symbol);
    Optional<InvestmentPosition> findFirstByUserAndAssetTypeAndExternalIdIgnoreCase(
            User user, InvestmentPosition.AssetType assetType, String externalId);
}
