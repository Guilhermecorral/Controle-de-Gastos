package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionReceiptResponseDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /**
     * Salva ou substitui a nota fiscal vinculada a uma transacao especifica.
     */
    @PostMapping(path = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionResponseDTO> uploadReceipt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(transactionService.attachReceipt(id, file));
    }

    /**
     * Lista as notas fiscais anexadas no periodo selecionado pelo usuario.
     */
    @GetMapping("/receipts")
    public ResponseEntity<List<TransactionReceiptResponseDTO>> listReceiptsByPeriod(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(transactionService.listReceiptsByPeriod(year, month));
    }

    /**
     * Devolve o arquivo salvo para download autenticado.
     */
    @GetMapping("/{id}/receipt/download")
    public ResponseEntity<Resource> downloadReceipt(@PathVariable Long id) {
        TransactionService.ReceiptDownloadPayload payload = transactionService.loadReceiptForDownload(id);
        String encodedFilename = URLEncoder.encode(payload.originalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }
}
