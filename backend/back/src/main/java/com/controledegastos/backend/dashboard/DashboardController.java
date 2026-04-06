package com.controledegastos.backend.dashboard; // Declares the package for dashboard web endpoints.

import com.controledegastos.backend.dashboard.dto.DashboardResponseDTO; // Imports the DTO returned to the API client.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for dependency injection.
import org.springframework.http.ResponseEntity; // Imports the HTTP response wrapper used by Spring MVC.
import org.springframework.web.bind.annotation.GetMapping; // Imports the GET mapping annotation.
import org.springframework.web.bind.annotation.RequestMapping; // Imports the base route mapping annotation.
import org.springframework.web.bind.annotation.RestController; // Marks the class as a REST controller.

@RestController // Registers the class as a JSON REST controller.
@RequestMapping("/api/dashboard") // Defines the base route for dashboard requests.
@RequiredArgsConstructor // Generates the constructor for the injected service.
public class DashboardController { // Declares the controller that exposes the dashboard endpoint.

    private final DashboardService dashboardService; // Stores the service that assembles the dashboard data.

    // GET /api/dashboard returns the minimal financial overview for the authenticated user.
    @GetMapping // Maps this method to HTTP GET on the controller route.
    public ResponseEntity<DashboardResponseDTO> getDashboard() { // Starts the endpoint method.
        return ResponseEntity.ok(dashboardService.getDashboard()); // Returns status 200 with the assembled dashboard payload.
    } // Closes the endpoint method.
} // Closes the controller class.
