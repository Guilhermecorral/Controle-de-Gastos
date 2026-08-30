package com.controledegastos.backend.transactions;

import com.controledegastos.backend.ofxupload.OFXUploadResponseDTO;
import com.controledegastos.backend.ofxupload.OFXUploadService;
import com.controledegastos.backend.ofxupload.ImportAnalysisDTO;
import com.controledegastos.backend.ofxupload.ImportPreviewTransactionDTO;
import com.controledegastos.backend.ofxupload.UniversalImportResult;
import com.controledegastos.backend.ofxupload.UniversalStatementImportService;
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
 * Controller for safe statement and spreadsheet preview.
 */
@RestController
@RequestMapping("/api/ofx")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statement import", description = "Endpoints for previewing OFX, CSV, TSV and Excel files")
public class OFXUploadController {

    private final OFXUploadService ofxUploadService;
    private final UniversalStatementImportService universalStatementImportService;

    @Value("${app.statement-import.max-bytes:5242880}")
    private long maxFileBytes;

    @Value("${app.statement-import.max-rows:5000}")
    private int maxRows;

    /**
     * Upload and parse a supported financial file.
     * @param file the file to upload
     * @return a response containing the parsed transactions and a message
     */
    @Operation(summary = "Preview a financial file", description = "Accepts OFX, CSV, TSV, XLS or XLSX and returns editable transaction suggestions with diagnostics.")
    @ApiResponse(responseCode = "200", description = "Successfully parsed the file",
            content = @Content(schema = @Schema(implementation = OFXUploadResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid file type or parsing error")
    @PostMapping("/upload")
    public ResponseEntity<OFXUploadResponseDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            validateUpload(file);
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            UniversalImportResult result;

            if (filename.endsWith(".ofx")) {
                List<TransactionRequestDTO> parsedTransactions = ofxUploadService.parseOFX(file);
                List<ImportPreviewTransactionDTO> previewTransactions = java.util.stream.IntStream
                        .range(0, parsedTransactions.size())
                        .mapToObj(index -> ImportPreviewTransactionDTO.fromOFX(parsedTransactions.get(index), index))
                        .toList();
                result = new UniversalImportResult(
                        previewTransactions,
                        new ImportAnalysisDTO(
                                "OFX",
                                "EXTRATO_BANCARIO",
                                parsedTransactions.size(),
                                parsedTransactions.size(),
                                List.of("Extrato OFX"),
                                List.of("Os dados estruturados do banco foram convertidos para uma prévia editável.")
                        )
                );
            } else if (isSpreadsheet(filename)) {
                result = universalStatementImportService.analyze(file);
            } else {
                return ResponseEntity.badRequest()
                        .body(errorResponse("Envie um arquivo OFX, CSV, TSV, XLS ou XLSX."));
            }

            if (result.transactions().size() > maxRows) {
                return ResponseEntity.badRequest()
                        .body(errorResponse("O arquivo ultrapassa o limite de " + maxRows + " transações por importação."));
            }

            String message = result.transactions().isEmpty()
                    ? "Não encontramos transações seguras automaticamente. Consulte o diagnóstico e ajuste a estrutura do arquivo."
                    : "Arquivo lido com sucesso. Revise " + result.transactions().size() + " transação(ões) antes de confirmar.";
            return ResponseEntity.ok(new OFXUploadResponseDTO(
                    message,
                    result.transactions(),
                    result.analysis()
            ));
        } catch (Exception e) {
            log.warn("Falha ao ler extrato para pre-visualizacao. type={}", e.getClass().getSimpleName());
            return ResponseEntity.badRequest()
                    .body(errorResponse("Não foi possível ler esse arquivo. Confirme o formato e tente novamente."));
        }
    }

    private boolean isSpreadsheet(String filename) {
        return filename.endsWith(".csv")
                || filename.endsWith(".tsv")
                || filename.endsWith(".xls")
                || filename.endsWith(".xlsx");
    }

    private OFXUploadResponseDTO errorResponse(String message) {
        return new OFXUploadResponseDTO(message, List.of(), null);
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo OFX, CSV, TSV ou Excel para continuar.");
        }

        if (file.getSize() > maxFileBytes) {
            throw new IllegalArgumentException("O arquivo excede o limite de " + (maxFileBytes / (1024 * 1024)) + " MB.");
        }
    }
}
