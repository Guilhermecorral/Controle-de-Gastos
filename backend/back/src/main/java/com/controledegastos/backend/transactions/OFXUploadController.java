package com.controledegastos.backend.transactions;

import com.controledegastos.backend.ofxupload.OFXUploadResponseDTO;
import com.controledegastos.backend.ofxupload.OFXUploadService;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for OFX and CSV file upload and parsing.
 */
@RestController
@RequestMapping("/api/ofx")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OFX Upload", description = "Endpoints for uploading and parsing OFX and CSV files")
public class OFXUploadController {

    private final OFXUploadService ofxUploadService;

    @Value("${app.statement-import.max-bytes:5242880}")
    private long maxFileBytes;

    @Value("${app.statement-import.max-rows:1000}")
    private int maxRows;

    /**
     * Upload and parse an OFX or CSV file.
     * @param file the file to upload (OFX or CSV)
     * @return a response containing the parsed transactions and a message
     */
    @Operation(summary = "Upload and parse OFX or CSV file", description = "Accepts an OFX or CSV file, parses it, and returns a list of transactions for preview.")
    @ApiResponse(responseCode = "200", description = "Successfully parsed the file",
            content = @Content(schema = @Schema(implementation = OFXUploadResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid file type or parsing error")
    @PostMapping("/upload")
    public ResponseEntity<OFXUploadResponseDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            validateUpload(file);
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            List<TransactionRequestDTO> transactions;

            if (filename.endsWith(".ofx")) {
                transactions = ofxUploadService.parseOFX(file);
            } else if (filename.endsWith(".csv")) {
                transactions = ofxUploadService.parseCSV(file);
            } else {
                return ResponseEntity.badRequest()
                        .body(new OFXUploadResponseDTO("Envie apenas arquivos OFX ou CSV.", List.of()));
            }

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new OFXUploadResponseDTO("Nenhuma transacao valida foi encontrada no arquivo.", List.of()));
            }

            if (transactions.size() > maxRows) {
                return ResponseEntity.badRequest()
                        .body(new OFXUploadResponseDTO("O extrato ultrapassa o limite de " + maxRows + " transacoes por importacao.", List.of()));
            }

            return ResponseEntity.ok(new OFXUploadResponseDTO(
                    "Arquivo lido com sucesso. Revise " + transactions.size() + " transacao(oes) antes de confirmar.", transactions));
        } catch (Exception e) {
            log.warn("Falha ao ler extrato para pre-visualizacao. type={}", e.getClass().getSimpleName());
            return ResponseEntity.badRequest()
                    .body(new OFXUploadResponseDTO("Nao foi possivel ler esse arquivo. Confirme o formato OFX ou CSV e tente novamente.", List.of()));
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo OFX ou CSV para continuar.");
        }

        if (file.getSize() > maxFileBytes) {
            throw new IllegalArgumentException("O arquivo excede o limite de " + (maxFileBytes / (1024 * 1024)) + " MB.");
        }
    }
}
