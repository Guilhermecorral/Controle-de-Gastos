package com.controledegastos.backend.investments;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class InvestmentDtos {
    private InvestmentDtos() {}

    public record PositionRequest(
            @NotNull InvestmentPosition.AssetType assetType,
            @Size(max = 30) String symbol,
            @Size(max = 80) String externalId,
            @NotBlank @Size(max = 120) String name,
            @DecimalMin(value = "0.00000001") BigDecimal quantity,
            @DecimalMin(value = "0.00") BigDecimal averagePrice,
            @DecimalMin(value = "0.01") BigDecimal principal,
            @DecimalMin(value = "0.00") BigDecimal annualRate,
            @NotNull LocalDate purchaseDate,
            LocalDate maturityDate
    ) {}

    public record QuoteResponse(
            String symbol,
            BigDecimal price,
            BigDecimal changePercent,
            BigDecimal dividendYield,
            String currency,
            String source,
            Instant updatedAt,
            boolean available
    ) {}

    public record PositionResponse(
            Long id,
            InvestmentPosition.AssetType assetType,
            String symbol,
            String externalId,
            String name,
            BigDecimal quantity,
            BigDecimal averagePrice,
            BigDecimal principal,
            BigDecimal annualRate,
            LocalDate purchaseDate,
            LocalDate maturityDate,
            BigDecimal investedAmount,
            BigDecimal currentValue,
            BigDecimal returnAmount,
            QuoteResponse quote
    ) {}

    public record PortfolioResponse(
            BigDecimal totalInvested,
            BigDecimal currentValue,
            BigDecimal totalReturn,
            List<PositionResponse> positions
    ) {}

    public record ProjectionPoint(int month, LocalDate date, BigDecimal balance, BigDecimal earnings) {}

    public record ProjectionResponse(
            BigDecimal principal,
            BigDecimal annualRate,
            BigDecimal projectedBalance,
            BigDecimal projectedEarnings,
            int months,
            List<ProjectionPoint> timeline,
            String disclaimer
    ) {}

    public record IncomeRequest(
            @NotNull InvestmentMovement.MovementType movementType,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull LocalDate eventDate
    ) {}

    public record MovementResponse(
            Long id,
            Long positionId,
            String assetName,
            InvestmentMovement.MovementType movementType,
            BigDecimal amount,
            LocalDate eventDate,
            boolean automatic
    ) {}
}
