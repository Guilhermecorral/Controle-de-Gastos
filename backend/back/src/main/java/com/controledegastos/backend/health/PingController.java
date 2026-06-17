package com.controledegastos.backend.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expoe um ping publico e enxuto para health checks externos e cron jobs de wake-up.
 */
@RestController
public class PingController {

    /**
     * Responde rapidamente sem depender de autenticacao para manter o servico aquecido.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("message", "pong"));
    }
}
