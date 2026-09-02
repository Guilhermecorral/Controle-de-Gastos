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

    @GetMapping("/quotes")
    public QuoteResponse quote(@RequestParam InvestmentPosition.AssetType type,
                               @RequestParam(required = false) String symbol,
                               @RequestParam(required = false) String externalId,
                               @RequestParam(required = false, defaultValue = "BR") String market) {
        return investmentService.quote(type, symbol, externalId, market);
    }

    @GetMapping("/projections")
    public ProjectionResponse projection(@RequestParam BigDecimal principal,
                                         @RequestParam(required = false) BigDecimal annualRate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate maturityDate) {
        return investmentService.projection(principal, annualRate, startDate, maturityDate);
    }
}
