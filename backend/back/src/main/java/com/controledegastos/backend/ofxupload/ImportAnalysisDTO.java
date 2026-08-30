package com.controledegastos.backend.ofxupload;

import java.util.List;

public record ImportAnalysisDTO(
        String format,
        String layout,
        int processedRows,
        int detectedTransactions,
        List<String> sheets,
        List<String> warnings
) {
}
