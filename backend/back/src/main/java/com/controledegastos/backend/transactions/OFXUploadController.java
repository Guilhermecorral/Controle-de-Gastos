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
@Tag(name = "OFX Upload", description = "Endpoints for uploading and parsing OFX and CSV files")
public class OFXUploadController {

    private final OFXUploadService ofxUploadService;

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
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            List<TransactionRequestDTO> transactions;

            if (filename.endsWith(".ofx")) {
                transactions = ofxUploadService.parseOFX(file);
            } else if (filename.endsWith(".csv")) {
                transactions = ofxUploadService.parseCSV(file);
            } else {
                // Try to detect by content type
                String contentType = file.getContentType();
                if (contentType != null && contentType.contains("ofx")) {
                    transactions = ofxUploadService.parseOFX(file);
                } else if (contentType != null && contentType.contains("csv")) {
                    transactions = ofxUploadService.parseCSV(file);
                } else {
                    return ResponseEntity.badRequest()
                            .body(new OFXUploadResponseDTO("Unsupported file type. Please upload an OFX or CSV file.", null));
                }
            }

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new OFXUploadResponseDTO("No valid transactions found in the file.", null));
            }

            return ResponseEntity.ok(new OFXUploadResponseDTO(
                    "File parsed successfully. Found " + transactions.size() + " transactions.", transactions));
        } catch (Exception e) {
            // Log the exception (in a real application, use a logger)
            return ResponseEntity.badRequest()
                    .body(new OFXUploadResponseDTO("Error parsing file: " + e.getMessage(), null));
        }
    }
}