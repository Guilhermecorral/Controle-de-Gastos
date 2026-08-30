package com.controledegastos.backend.ofxupload;

import java.util.List;

public record UniversalImportResult(
        List<ImportPreviewTransactionDTO> transactions,
        ImportAnalysisDTO analysis
) {
}
