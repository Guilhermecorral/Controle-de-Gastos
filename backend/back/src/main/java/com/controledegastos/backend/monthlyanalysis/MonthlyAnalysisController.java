package com.controledegastos.backend.monthlyanalysis; // Declares the package for the monthly analysis web layer.

import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO; // Imports the response DTO returned by the endpoint.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for injected dependencies.
import org.springframework.http.ResponseEntity; // Imports the HTTP response wrapper used by Spring MVC.
import org.springframework.web.bind.annotation.GetMapping; // Imports the GET mapping annotation.
import org.springframework.web.bind.annotation.RequestMapping; // Imports the base route mapping annotation.
import org.springframework.web.bind.annotation.RequestParam; // Imports the query-parameter binding annotation.
import org.springframework.web.bind.annotation.RestController; // Marks the class as a REST controller.

@RestController // Registers the class as a Spring MVC controller that returns JSON.
@RequestMapping("/api/monthly-analysis") // Defines the base route of the monthly analysis feature.
@RequiredArgsConstructor // Generates the constructor for the injected service dependency.
public class MonthlyAnalysisController { // Declares the controller that exposes the monthly analysis endpoint.

    private final MonthlyAnalysisService monthlyAnalysisService; // Stores the service that assembles the monthly analysis payload.

    // GET /api/monthly-analysis returns the analysis of the informed month for the authenticated user.
    @GetMapping // Maps this method to HTTP GET on the base controller route.
    public ResponseEntity<MonthlyAnalysisResponseDTO> getMonthlyAnalysis( // Declares the endpoint method that returns the monthly analysis.
            @RequestParam int year, // Binds the required year query parameter.
            @RequestParam int month // Binds the required month query parameter.
    ) { // Closes the method signature.
        return ResponseEntity.ok(monthlyAnalysisService.getMonthlyAnalysis(year, month)); // Returns status 200 with the assembled monthly analysis payload.
    } // Closes the endpoint method.
} // Closes the controller class.
