package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UniversalStatementImportServiceTest {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final UniversalStatementImportService service = new UniversalStatementImportService();

    @Test
    void parsesPatrimonialHistoryWithoutDuplicatingCalculatedColumns() throws Exception {
        String csv = """
                Mês;Idade;Fase de Vida;Renda Total (R$);Custo de Vida (R$);Parcela Casa (R$);Parcela Carro (R$);Fluxo do Mês (R$);Rendimento do Investimento (R$);Patrimônio Líquido Acumulado (R$)
                2006-06-06 00:00:00;25;Solteiro;R$ 4000,000;R$ 1200,000;R$ 1500,000;R$ 1666,66667;-R$ 366,66667;R$ 200,000;R$ 19833,33333
                2006-07-06 00:00:00;25;Solteiro;R$ 4100,000;R$ 1250,000;R$ 1500,000;R$ 1666,66667;-R$ 316,66667;R$ 198,33333;R$ 19714,99999
                """;

        UniversalImportResult result = service.analyze(csvFile("joao.csv", csv));

        assertThat(result.analysis().format()).isEqualTo("CSV");
        assertThat(result.analysis().layout()).contains("TABELA_FINANCEIRA_MULTICOLUNA");
        assertThat(result.transactions()).hasSize(10);
        assertThat(result.transactions()).allMatch(ImportPreviewTransactionDTO::selectedByDefault);
        assertThat(result.transactions()).extracting(ImportPreviewTransactionDTO::description)
                .doesNotContain("Fluxo do Mês (R$)", "Patrimônio Líquido Acumulado (R$)");
        assertThat(result.transactions()).filteredOn(transaction -> transaction.transactionDate().equals(LocalDate.of(2006, 6, 6)))
                .extracting(ImportPreviewTransactionDTO::type)
                .containsExactlyInAnyOrder(
                        TransactionType.RECEITA,
                        TransactionType.DESPESA,
                        TransactionType.DESPESA,
                        TransactionType.DESPESA,
                        TransactionType.RECEITA
                );
    }

    @Test
    void parsesVerticalAndHorizontalRegionsFromTheSameRafaelFile() throws Exception {
        String csv = """
                Mês;Idade;Fase;Entradas_Servicos (R$);Santander_Parcelas (R$);Casa_Aluguel_Energia (R$);Sem_Categoria (R$);Saidas_Totais (R$);Fluxo_do_Mes (R$);Saldo_Reserva_Operacional (R$);;;
                2026-06-16;24;Início;R$ 3761,000;R$ 400,000;R$ 800,000;R$ 1340,000;R$ 2540,000;R$ 1221,000;R$ ,000;;;
                2026-07-16;24;Início;R$ 2782,000;R$ 400,000;R$ 800,000;R$ 1111,000;R$ 2311,000;R$ 471,000;R$ ,000;;;
                ;;;;;;;;;;;Mes;2031-02-16;2031-03-16
                ;;;;;;;;;;;IDAde;28;28
                ;;;;;;;;;;;Fase;Família/Descontrole;Família/Descontrole
                ;;;;;;;;;;;Entradas_Servicos (R$);R$ 7915,000;R$ 6069,000
                ;;;;;;;;;;;Santander_Parcelas (R$);R$ 1424,7000;R$ 1092,42000
                ;;;;;;;;;;;Casa_Aluguel_Energia (R$);R$ 1736,4000;R$ 1736,4000
                ;;;;;;;;;;;Sem_Categoria (R$);R$ 2330,000;R$ 2222,000
                ;;;;;;;;;;;Saidas_Totais (R$);R$ 5491,1000;R$ 5050,82000
                ;;;;;;;;;;;Fluxo_do_Mes (R$);R$ 2423,9000;R$ 1018,18000
                ;;;;;;;;;;;Saldo_Reserva_Operacional (R$);R$ ,000;R$ ,000
                """;

        UniversalImportResult result = service.analyze(csvFile("rafael.csv", csv));

        assertThat(result.analysis().layout())
                .isEqualTo("TABELA_FINANCEIRA_MULTICOLUNA + MATRIZ_MENSAL");
        assertThat(result.transactions()).hasSize(16);
        assertThat(result.transactions()).allMatch(ImportPreviewTransactionDTO::selectedByDefault);
        assertThat(result.transactions()).extracting(ImportPreviewTransactionDTO::description)
                .doesNotContain(
                        "Saidas_Totais (R$)",
                        "Fluxo_do_Mes (R$)",
                        "Saldo_Reserva_Operacional (R$)"
                );
        assertThat(result.transactions())
                .filteredOn(transaction -> transaction.description().contains("Casa_Aluguel_Energia"))
                .extracting(ImportPreviewTransactionDTO::category)
                .containsOnly(TransactionCategory.MORADIA);
        assertThat(result.transactions())
                .filteredOn(transaction -> transaction.description().contains("Santander_Parcelas"))
                .extracting(ImportPreviewTransactionDTO::category)
                .containsOnly(TransactionCategory.COMPRAS);
    }

    @Test
    void readsXlsxUsingCachedValuesWithoutEvaluatingFormulas() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Extrato");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Data");
            header.createCell(1).setCellValue("Descrição");
            header.createCell(2).setCellValue("Valor");
            header.createCell(3).setCellValue("Tipo");

            var transaction = sheet.createRow(1);
            transaction.createCell(0).setCellValue("30/08/2026");
            transaction.createCell(1).setCellValue("Uber Trip");
            transaction.createCell(2).setCellValue(42.50);
            transaction.createCell(3).setCellValue("Despesa");

            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        UniversalImportResult result = service.analyze(file);

        assertThat(result.analysis().format()).isEqualTo("XLSX");
        assertThat(result.transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.description()).isEqualTo("Uber Trip");
            assertThat(transaction.category()).isEqualTo(TransactionCategory.TRANSPORTE);
            assertThat(transaction.transactionDate()).isEqualTo(LocalDate.of(2026, 8, 30));
        });
    }

    @Test
    @EnabledIfSystemProperty(named = "statement.acceptance.enabled", matches = "true")
    void parsesTheTwoFullAcceptanceHistories() throws Exception {
        UniversalImportResult joao = service.analyze(fileFromSystemProperty("statement.acceptance.joao"));
        UniversalImportResult rafael = service.analyze(fileFromSystemProperty("statement.acceptance.rafael"));

        assertThat(joao.transactions()).hasSize(1_020);
        assertThat(joao.analysis().layout()).contains("TABELA_FINANCEIRA_MULTICOLUNA");
        assertThat(rafael.transactions()).hasSize(480);
        assertThat(rafael.analysis().layout())
                .isEqualTo("TABELA_FINANCEIRA_MULTICOLUNA + MATRIZ_MENSAL");
    }

    private MockMultipartFile csvFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/csv", content.getBytes(WINDOWS_1252));
    }

    private MockMultipartFile fileFromSystemProperty(String property) throws Exception {
        Path path = Path.of(System.getProperty(property));
        return new MockMultipartFile("file", path.getFileName().toString(), "text/csv", Files.readAllBytes(path));
    }
}
