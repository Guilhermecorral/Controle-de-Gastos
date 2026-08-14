package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
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
    private List<TransactionResponseDTO> transactions;
}