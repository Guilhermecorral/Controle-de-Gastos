package com.controledegastos.backend.config;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Padroniza o formato de erro devolvido pela API para facilitar o consumo no frontend.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
