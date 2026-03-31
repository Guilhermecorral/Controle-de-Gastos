package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // POST /api/transactions - create a new transaction
    // @Valid: Validation DTO (@NotNull, @DecimalMIn, etc.)
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @Valid @RequestBody TransactionRequestDTO dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.create(dto));
    }

    // GET /api/transactions - List optional filter
    // @ResquestParam required=false: Optional parameters in URL
    // Ex.: GET /api/transactions?type=DESPESA&category=ALIMENTACAO
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> findAll(
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionCategory category
    ) {
        return ResponseEntity.ok(transactionService.findAll(type, category));
    }

    // PUT /api/transactions/{id} - Update a transaction by ID
    // @PathVariable: Extract the ID from the URL path
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDTO dto
    ) {
        return ResponseEntity.ok(transactionService.update(id, dto));
    }

    // DELETE /api/transactions/{id} - Delete a transaction
    // 204 No Content: Operation successfully, but no body answer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
