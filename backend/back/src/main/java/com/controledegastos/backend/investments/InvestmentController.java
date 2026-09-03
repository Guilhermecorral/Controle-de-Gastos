package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private final InvestmentService investmentService;

    @GetMapping("/portfolio")
    public PortfolioResponse portfolio() { return investmentService.portfolio(); }

    @PostMapping("/positions")
    public ResponseEntity<PositionResponse> create(@Valid @RequestBody PositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.create(request));
    }

    @PutMapping("/positions/{id}")
    public PositionResponse update(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
        return investmentService.update(id, request);
    }

    @DeleteMapping("/positions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        investmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movements")
    public java.util.List<MovementResponse> movements() { return investmentService.movements(); }

    @PostMapping("/movements/trades")
    public ResponseEntity<MovementResponse> trade(@Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.recordTrade(request));
    }

    @GetMapping("/assets/search")
    public List<AssetSearchResponse> searchAssets(@RequestParam String query,
                                                   @RequestParam InvestmentPosition.AssetType type) {
        return investmentService.searchAssets(query, type);
    }

    @PostMapping("/positions/{id}/income")
    public ResponseEntity<MovementResponse> recordIncome(@PathVariable Long id, @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.recordIncome(id, request));
    }

    @GetMapping("/income-schedules")
    public List<IncomeScheduleResponse> incomeSchedules() { return investmentService.incomeSchedules(); }

    @PostMapping("/income-schedules")
    public ResponseEntity<IncomeScheduleResponse> createIncomeSchedule(@Valid @RequestBody IncomeScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.createIncomeSchedule(request));
    }

    @PostMapping("/income-schedules/{id}/receive")
    public IncomeScheduleResponse receiveIncomeSchedule(@PathVariable Long id) {
        return investmentService.receiveIncomeSchedule(id);
    }

    @DeleteMapping("/income-schedules/{id}")
    public ResponseEntity<Void> deleteIncomeSchedule(@PathVariable Long id) {
        investmentService.deleteIncomeSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals")
    public List<GoalResponse> goals() { return investmentService.goals(); }

    @PostMapping("/goals")
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.createGoal(request));
    }

    @PutMapping("/goals/{id}")
    public GoalResponse updateGoal(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return investmentService.updateGoal(id, request);
    }

    @PostMapping("/goals/{id}/contributions")
    public GoalResponse contributeToGoal(@PathVariable Long id, @Valid @RequestBody GoalContributionRequest request) {
        return investmentService.contributeToGoal(id, request);
    }

    @GetMapping("/goals/{id}/contributions")
    public List<GoalContributionResponse> goalContributions(@PathVariable Long id) {
        return investmentService.goalContributions(id);
    }

    @DeleteMapping("/goals/{goalId}/contributions/{contributionId}")
    public ResponseEntity<Void> deleteGoalContribution(@PathVariable Long goalId, @PathVariable Long contributionId) {
        investmentService.deleteGoalContribution(goalId, contributionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tax-summary")
    public TaxSummaryResponse taxSummary(@RequestParam(required = false) Integer year) {
        return investmentService.taxSummary(year);
    }

    @GetMapping("/reconciliation")
    public ReconciliationResponse reconciliation(@RequestParam(required = false) Integer year) {
        return investmentService.reconciliation(year);
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        investmentService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quotes")
    public QuoteResponse quote(@RequestParam InvestmentPosition.AssetType type,
                               @RequestParam(required = false) String symbol,
                               @RequestParam(required = false) String externalId,
                               @RequestParam(required = false, defaultValue = "BR") String market) {
        return investmentService.quote(type, symbol, externalId, market);
    }

    @GetMapping("/projections")
    public ProjectionResponse projection(@RequestParam(required = false) BigDecimal initialAmount,
                                         @RequestParam(defaultValue = "0") BigDecimal monthlyContribution,
                                         @RequestParam(required = false) BigDecimal interestRate,
                                         @RequestParam(defaultValue = "ANNUAL") RatePeriod ratePeriod,
                                         @RequestParam(defaultValue = "MONTHLY") TimelinePeriod timelinePeriod,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(required = false) BigDecimal principal,
                                         @RequestParam(required = false) BigDecimal annualRate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate maturityDate) {
        return investmentService.projection(initialAmount != null ? initialAmount : principal, monthlyContribution,
                interestRate != null ? interestRate : annualRate, ratePeriod, timelinePeriod, startDate,
                endDate != null ? endDate : maturityDate);
    }
}
