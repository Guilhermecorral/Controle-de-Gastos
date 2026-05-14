package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expoe os endpoints de criacao, consulta, edicao e exclusao de transacoes.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Cria uma nova transacao para o usuario autenticado.
     */
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(@Valid @RequestBody TransactionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(dto));
    }

    /**
     * Lista as transacoes do usuario com filtros opcionais de tipo e categoria.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> findAll(
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionCategory category
    ) {
        return ResponseEntity.ok(transactionService.findAll(type, category));
    }

    /**
     * Atualiza uma transacao existente que pertence ao usuario autenticado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDTO dto
    ) {
        return ResponseEntity.ok(transactionService.update(id, dto));
    }

    /**
     * Remove uma transacao existente do usuario autenticado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
