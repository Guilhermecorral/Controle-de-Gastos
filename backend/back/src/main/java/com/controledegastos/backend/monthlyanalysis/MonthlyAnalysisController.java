package com.controledegastos.backend.monthlyanalysis;

import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expoe o endpoint de analise mensal por ano e mes de referencia.
 */
@RestController
@RequestMapping("/api/monthly-analysis")
@RequiredArgsConstructor
public class MonthlyAnalysisController {

    private final MonthlyAnalysisService monthlyAnalysisService;

    /**
     * Devolve a analise detalhada de um mes especifico escolhido pelo usuario.
     */
    @GetMapping
    public ResponseEntity<MonthlyAnalysisResponseDTO> getMonthlyAnalysis(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(monthlyAnalysisService.getMonthlyAnalysis(year, month));
    }
}
