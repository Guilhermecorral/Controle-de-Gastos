package com.controledegastos.backend.investments;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ponto de extensão reservado para notas B3. Não é exposto enquanto a flag de produção estiver desligada.
 */
@Service
@ConditionalOnProperty(name = "app.investments.imports.pdf-enabled", havingValue = "true")
public class PdfInvestmentImportParser {
    public void preview(MultipartFile file) {
        throw new UnsupportedOperationException("A leitura de PDF B3 ainda não está disponível. Use CSV, Excel ou OFX de investimentos.");
    }
}
