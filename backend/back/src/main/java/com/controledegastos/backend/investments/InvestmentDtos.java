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
            LocalDate maturityDate,
            @Size(max = 10) String market,
            @Size(max = 30) String exchange,
            @Size(max = 3) String currency
    ) {}

    public record AssetSearchResponse(
            InvestmentPosition.AssetType assetType,
            String symbol,
            String externalId,
            String name,
            String market,
            String exchange,
            String currency,
            BigDecimal currentPrice,
            String source
    ) {}

    public record TradeRequest(
            Long positionId,
            @NotNull InvestmentMovement.MovementType movementType,
            @NotNull InvestmentPosition.AssetType assetType,
            @Size(max = 30) String symbol,
            @Size(max = 80) String externalId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 10) String market,
            @Size(max = 30) String exchange,
            @NotBlank @Size(max = 3) String currency,
            @NotNull @DecimalMin(value = "0.00000001") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.000001") BigDecimal unitPrice,
            @DecimalMin(value = "0.00") BigDecimal fees,
            @NotNull LocalDate eventDate
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
            String market,
            String exchange,
            String currency,
            BigDecimal quantity,
            BigDecimal averagePrice,
            BigDecimal principal,
            BigDecimal annualRate,
            LocalDate purchaseDate,
            LocalDate maturityDate,
            BigDecimal investedAmount,
            BigDecimal currentValue,
            BigDecimal capitalGainAmount,
            BigDecimal capitalGainPercent,
            BigDecimal incomeAmount,
            BigDecimal totalReturnAmount,
            BigDecimal totalReturnPercent,
            QuoteResponse quote
    ) {}

    public record PortfolioResponse(
            BigDecimal totalInvested,
            BigDecimal currentValue,
            BigDecimal totalCapitalGain,
            BigDecimal totalIncome,
            BigDecimal totalReturn,
            BigDecimal totalReturnPercent,
            List<PositionResponse> positions,
            List<PortfolioEvolutionPoint> evolution
    ) {}

    public record PortfolioEvolutionPoint(
            LocalDate date,
            BigDecimal investedAmount,
            BigDecimal currentValue,
            BigDecimal incomeAmount
    ) {}

    public enum RatePeriod { MONTHLY, ANNUAL }
    public enum TimelinePeriod { MONTHLY, YEARLY }

    public record ProjectionPoint(
            int month,
            LocalDate date,
            BigDecimal contribution,
            BigDecimal interest,
            BigDecimal totalInvested,
            BigDecimal totalInterest,
            BigDecimal balance
    ) {}

    public record ProjectionResponse(
            BigDecimal initialAmount,
            BigDecimal monthlyContribution,
            BigDecimal interestRate,
            RatePeriod ratePeriod,
            TimelinePeriod timelinePeriod,
            BigDecimal effectiveMonthlyRate,
            BigDecimal totalInvested,
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

    public record IncomeScheduleRequest(
            @NotNull Long positionId,
            @NotNull InvestmentMovement.MovementType incomeType,
            @NotNull @DecimalMin(value = "0.00000001") BigDecimal amountPerUnit,
            @DecimalMin(value = "0.00") BigDecimal taxRate,
            LocalDate exDate,
            @NotNull LocalDate paymentDate
    ) {}

    public record IncomeScheduleResponse(
            Long id,
            Long positionId,
            String symbol,
            String assetName,
            InvestmentMovement.MovementType incomeType,
            BigDecimal amountPerUnit,
            BigDecimal quantityEligible,
            BigDecimal grossAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal netAmount,
            LocalDate exDate,
            LocalDate paymentDate,
            InvestmentIncomeSchedule.Status status
    ) {}

    public record GoalRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
            @DecimalMin(value = "0.00") BigDecimal monthlyContribution,
            @DecimalMin(value = "0.00") BigDecimal annualGrowthRate
    ) {}

    public record GoalResponse(
            Long id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            BigDecimal remainingAmount,
            BigDecimal progressPercent,
            BigDecimal monthlyContribution,
            BigDecimal annualGrowthRate,
            Integer estimatedMonths,
            boolean achieved
    ) {}

    public record MovementResponse(
            Long id,
            Long positionId,
            String assetName,
            InvestmentMovement.MovementType movementType,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fees,
            String currency,
            LocalDate eventDate,
            boolean automatic
    ) {}
}
