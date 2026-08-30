package com.controledegastos.backend.ofxupload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for the response of OFX upload.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OFXUploadResponseDTO {
    private String message;
    private List<ImportPreviewTransactionDTO> transactions;
    private ImportAnalysisDTO analysis;
}
