package com.controledegastos.backend.diagnostics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe breadcrumbs curtos do navegador durante a investigacao de falhas de interface.
 * O contrato deliberadamente nao aceita ticker, valores, tokens ou mensagens de excecao.
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostics")
public class ClientDiagnosticsController {

    @PostMapping("/ui-events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordUiEvent(@Valid @RequestBody UiEventRequest request) {
        log.warn("[UI DIAGNOSTIC] area={} action={} mode={}",
                request.area(), request.action(), request.mode());
    }

    public record UiEventRequest(
            @NotBlank @Size(max = 48) @Pattern(regexp = "[a-z0-9-]+") String area,
            @NotBlank @Size(max = 48) @Pattern(regexp = "[a-z0-9-]+") String action,
            @NotBlank @Size(max = 24) @Pattern(regexp = "[A-Z_]+") String mode
    ) {
    }
}
