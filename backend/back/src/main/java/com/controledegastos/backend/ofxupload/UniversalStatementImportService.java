package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta planilhas sem executar formulas e preserva a origem de cada sugestao para revisao humana.
 */
@Service
public class UniversalStatementImportService {

    private static final int MAX_SHEETS = 20;
    private static final int MAX_ROWS_PER_SHEET = 10_000;
    private static final int MAX_COLUMNS = 120;
    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(?i)\\b(\\d{1,3})\\s*/\\s*(\\d{1,3})\\b");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BRAZILIAN_DASH_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    public UniversalImportResult analyze(MultipartFile file) throws IOException, CsvException {
        String filename = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        ReadDocument document;

        if (normalizedFilename.endsWith(".csv") || normalizedFilename.endsWith(".tsv")) {
            document = readDelimited(file, normalizedFilename.endsWith(".tsv") ? '\t' : null);
        } else if (normalizedFilename.endsWith(".xlsx") || normalizedFilename.endsWith(".xls")) {
            document = readWorkbook(file, normalizedFilename.endsWith(".xlsx") ? "XLSX" : "XLS");
        } else {
            throw new IllegalArgumentException("Formato de planilha nao suportado.");
        }

        List<ImportPreviewTransactionDTO> transactions = new ArrayList<>();
        Set<String> layouts = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>(document.warnings());
        int processedRows = 0;

        for (TabularSheet sheet : document.sheets()) {
            SheetAnalysis analysis = analyzeSheet(sheet);
            transactions.addAll(analysis.transactions());
            processedRows += analysis.processedRows();
            layouts.add(analysis.layout());
            warnings.addAll(analysis.warnings());
        }

        transactions = inferHistoricalInstallmentSequences(transactions, warnings);

        if (transactions.isEmpty()) {
            warnings.add("Encontramos dados no arquivo, mas nao uma combinacao segura de data, descricao e valor. Revise a estrutura ou use o mapeamento assistido.");
        }

        long uncertain = transactions.stream().filter(transaction -> transaction.confidence() == ImportConfidence.BAIXA).count();
        if (uncertain > 0) {
            warnings.add(uncertain + " sugestao(oes) ficaram desmarcadas por precisarem de data ou confirmacao de significado.");
        }

        ImportAnalysisDTO analysis = new ImportAnalysisDTO(
                document.format(),
                layouts.isEmpty() ? "NAO_IDENTIFICADO" : String.join(" + ", layouts),
                processedRows,
                transactions.size(),
                document.sheets().stream().map(TabularSheet::name).toList(),
                warnings.stream().distinct().toList()
        );

        return new UniversalImportResult(transactions, analysis);
    }

    private ReadDocument readDelimited(MultipartFile file, Character forcedSeparator) throws IOException, CsvException {
        byte[] bytes = file.getBytes();
        Charset charset = isValidUtf8(bytes) ? StandardCharsets.UTF_8 : WINDOWS_1252;
        String content = new String(bytes, charset).replace("\uFEFF", "");
        char separator = forcedSeparator != null ? forcedSeparator : detectSeparator(content);
        List<List<String>> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(content))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build()) {
            String[] row;
            while ((row = reader.readNext()) != null && rows.size() < MAX_ROWS_PER_SHEET) {
                rows.add(limitColumns(List.of(row)));
            }
        }

        List<String> warnings = new ArrayList<>();
        if (!StandardCharsets.UTF_8.equals(charset)) {
            warnings.add("A codificacao Windows-1252 foi reconhecida e convertida sem perder acentos.");
        }
        warnings.add("Separador detectado: " + printableSeparator(separator) + ".");
        return new ReadDocument("CSV", List.of(new TabularSheet("Dados", rows)), warnings);
    }

    private ReadDocument readWorkbook(MultipartFile file, String format) throws IOException {
        List<TabularSheet> sheets = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(new Locale("pt", "BR"), true);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            int sheetLimit = Math.min(workbook.getNumberOfSheets(), MAX_SHEETS);
            for (int sheetIndex = 0; sheetIndex < sheetLimit; sheetIndex++) {
                if (workbook.isSheetHidden(sheetIndex) || workbook.isSheetVeryHidden(sheetIndex)) {
                    continue;
                }

                Sheet sheet = workbook.getSheetAt(sheetIndex);
                List<List<String>> rows = new ArrayList<>();
                int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS_PER_SHEET - 1);

                for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= lastRow; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        rows.add(List.of());
                        continue;
                    }

                    int lastCell = Math.min(Math.max(row.getLastCellNum(), 0), MAX_COLUMNS);
                    List<String> cells = new ArrayList<>(lastCell);
                    for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
                        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        cells.add(readCellWithoutEvaluation(cell, formatter));
                    }
                    rows.add(trimTrailingBlanks(cells));
                }

                sheets.add(new TabularSheet(sheet.getSheetName(), rows));
            }
        } catch (EncryptedDocumentException exception) {
            throw new IllegalArgumentException("Planilhas protegidas por senha nao podem ser importadas.", exception);
        }

        warnings.add("Formulas foram lidas apenas pelo valor armazenado; nenhuma formula ou macro foi executada.");
        return new ReadDocument(format, sheets, warnings);
    }

    private String readCellWithoutEvaluation(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> formatter.formatRawCellContents(
                        cell.getNumericCellValue(),
                        cell.getCellStyle().getDataFormat(),
                        cell.getCellStyle().getDataFormatString()
                );
                case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
                case ERROR -> "";
                default -> "";
            };
        }
        return formatter.formatCellValue(cell);
    }

    private SheetAnalysis analyzeSheet(TabularSheet sheet) {
        HeaderPlan headerPlan = findHeaderPlan(sheet.rows());
        MonthMatrixPlan monthMatrixPlan = findMonthMatrix(sheet.rows());

        if (headerPlan != null || monthMatrixPlan != null) {
            List<ImportPreviewTransactionDTO> transactions = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> layouts = new ArrayList<>();
            int processedRows = 0;

            if (headerPlan != null) {
                int endRowExclusive = monthMatrixPlan != null && monthMatrixPlan.rowIndex() > headerPlan.rowIndex()
                        ? monthMatrixPlan.rowIndex()
                        : sheet.rows().size();
                SheetAnalysis tableAnalysis = analyzeHeaderTable(sheet, headerPlan, endRowExclusive);
                transactions.addAll(tableAnalysis.transactions());
                warnings.addAll(tableAnalysis.warnings());
                layouts.add(tableAnalysis.layout());
                processedRows += tableAnalysis.processedRows();
            }

            if (monthMatrixPlan != null) {
                SheetAnalysis matrixAnalysis = analyzeMonthMatrix(sheet, monthMatrixPlan);
                transactions.addAll(matrixAnalysis.transactions());
                warnings.addAll(matrixAnalysis.warnings());
                layouts.add(matrixAnalysis.layout());
                processedRows += matrixAnalysis.processedRows();
            }

            return new SheetAnalysis(
                    transactions,
                    processedRows,
                    String.join(" + ", layouts.stream().distinct().toList()),
                    warnings.stream().distinct().toList()
            );
        }

        return analyzeBlocks(sheet);
    }

    private HeaderPlan findHeaderPlan(List<List<String>> rows) {
        HeaderPlan best = null;
        int bestScore = 0;
        int limit = Math.min(rows.size(), 40);

        for (int rowIndex = 0; rowIndex < limit; rowIndex++) {
            List<String> row = rows.get(rowIndex);
            int date = findHeader(row, "data", "date", "mes", "competencia", "periodo");
            int description = findHeader(row, "descricao", "description", "historico", "estabelecimento", "lancamento", "nome");
            int amount = findHeader(row, "valor", "amount", "quantia", "montante");
            int type = findHeader(row, "tipo", "type", "natureza");
            int category = findHeader(row, "categoria", "category");
            int payment = findHeader(row, "pagamento", "forma de pagamento", "payment", "meio");
            int installments = findHeader(row, "parcelas", "parcela", "installments");
            List<MetricColumn> metrics = new ArrayList<>();

            for (int column = 0; column < row.size(); column++) {
                if (column == date || column == description || column == amount || column == type
                        || column == category || column == payment || column == installments) {
                    continue;
                }
                MetricRule rule = classifyMetric(row.get(column));
                if (rule != null) {
                    metrics.add(new MetricColumn(column, cleanLabel(row.get(column)), rule));
                }
            }

            int score = (date >= 0 ? 4 : 0) + (description >= 0 ? 2 : 0) + (amount >= 0 ? 3 : 0) + metrics.size() * 2;
            boolean useful = date >= 0 && ((description >= 0 && amount >= 0) || !metrics.isEmpty());
            if (useful && score > bestScore) {
                bestScore = score;
                best = new HeaderPlan(rowIndex, date, description, amount, type, category, payment, installments, metrics);
            }
        }

        return best;
    }

    private SheetAnalysis analyzeHeaderTable(TabularSheet sheet, HeaderPlan plan, int endRowExclusive) {
        List<ImportPreviewTransactionDTO> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int processedRows = 0;

        for (int rowIndex = plan.rowIndex() + 1; rowIndex < endRowExclusive; rowIndex++) {
            List<String> row = sheet.rows().get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }
            processedRows++;
            LocalDate date = parseFlexibleDate(valueAt(row, plan.date()));

            if (plan.amount() >= 0) {
                BigDecimal signedAmount = parseAmount(valueAt(row, plan.amount()));
                String description = sanitizeDescription(valueAt(row, plan.description()));
                if (signedAmount != null && signedAmount.signum() != 0 && !isSummaryLabel(description)) {
                    TransactionType type = parseExplicitType(valueAt(row, plan.type()), signedAmount);
                    ImportConfidence confidence = date != null && !description.isBlank() ? ImportConfidence.ALTA : ImportConfidence.BAIXA;
                    transactions.add(new ImportPreviewTransactionDTO(
                            type,
                            description.isBlank() ? "Transacao importada" : description,
                            parseExplicitCategory(valueAt(row, plan.category()), description),
                            signedAmount.abs(),
                            parsePaymentMethod(valueAt(row, plan.payment()), description),
                            parseInstallments(valueAt(row, plan.installments()), description),
                            date,
                            confidence != ImportConfidence.BAIXA,
                            confidence,
                            source(sheet.name(), rowIndex),
                            confidence == ImportConfidence.ALTA
                                    ? "Data, descricao e valor reconhecidos pelo cabecalho."
                                    : "Valor reconhecido, mas a data ou descricao precisa ser revisada."
                    ));
                }
            }

            for (MetricColumn metric : plan.metrics()) {
                BigDecimal amount = parseAmount(valueAt(row, metric.column()));
                if (amount == null || amount.signum() == 0) {
                    continue;
                }
                ImportConfidence confidence = date == null ? ImportConfidence.BAIXA : ImportConfidence.ALTA;
                transactions.add(new ImportPreviewTransactionDTO(
                        metric.rule().type(),
                        metric.label(),
                        metric.rule().category(),
                        amount.abs(),
                        metric.rule().paymentMethod(),
                        inferInstallments(metric.label()),
                        date,
                        confidence != ImportConfidence.BAIXA,
                        confidence,
                        source(sheet.name(), rowIndex),
                        "A coluna '" + metric.label() + "' foi interpretada como " + typeLabel(metric.rule().type()) + "."
                ));
            }
        }

        if (!plan.metrics().isEmpty()) {
            warnings.add("Colunas de saldo, fluxo, patrimonio e totais calculados foram ignoradas para evitar duplicidade.");
        }
        String layout = plan.metrics().isEmpty() ? "TABELA_TRANSACIONAL" : "TABELA_FINANCEIRA_MULTICOLUNA";
        return new SheetAnalysis(transactions, processedRows, layout, warnings);
    }

    private MonthMatrixPlan findMonthMatrix(List<List<String>> rows) {
        int limit = rows.size();
        for (int rowIndex = 0; rowIndex < limit; rowIndex++) {
            List<MonthColumn> months = new ArrayList<>();
            List<String> row = rows.get(rowIndex);
            for (int column = 0; column < row.size(); column++) {
                LocalDate date = parseFlexibleDate(valueAt(row, column));
                if (date != null) {
                    months.add(new MonthColumn(column, date));
                }
            }
            if (months.size() >= 2) {
                return new MonthMatrixPlan(rowIndex, months);
            }
        }
        return null;
    }

    private SheetAnalysis analyzeMonthMatrix(TabularSheet sheet, MonthMatrixPlan plan) {
        List<ImportPreviewTransactionDTO> transactions = new ArrayList<>();
        int processedRows = 0;

        for (int rowIndex = plan.rowIndex() + 1; rowIndex < sheet.rows().size(); rowIndex++) {
            List<String> row = sheet.rows().get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }
            processedRows++;
            String description = firstMeaningfulText(row, plan.months().stream().map(MonthColumn::column).collect(java.util.stream.Collectors.toSet()));
            if (description.isBlank() || isSummaryLabel(description) || isMetadataLabel(description)) {
                continue;
            }
            InferredType inferred = inferType(description, null);
            for (MonthColumn month : plan.months()) {
                BigDecimal amount = parseAmount(valueAt(row, month.column()));
                if (amount == null || amount.signum() == 0) {
                    continue;
                }
                TransactionType type = amount.signum() < 0 ? TransactionType.DESPESA : inferred.type();
                ImportConfidence confidence = inferred.confident() || amount.signum() < 0 ? ImportConfidence.MEDIA : ImportConfidence.BAIXA;
                transactions.add(new ImportPreviewTransactionDTO(
                        type,
                        sanitizeDescription(description),
                        suggestCategory(description),
                        amount.abs(),
                        parsePaymentMethod("", description),
                        inferInstallments(description),
                        month.date(),
                        confidence != ImportConfidence.BAIXA,
                        confidence,
                        source(sheet.name(), rowIndex),
                        "O mes veio do cabecalho da coluna e o significado foi inferido pela descricao."
                ));
            }
        }

        return new SheetAnalysis(transactions, processedRows, "MATRIZ_MENSAL", List.of());
    }

    private SheetAnalysis analyzeBlocks(TabularSheet sheet) {
        List<ImportPreviewTransactionDTO> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String context = cleanLabel(sheet.name());
        LocalDate contextDate = parseMonth(sheet.name());
        int processedRows = 0;

        for (int rowIndex = 0; rowIndex < sheet.rows().size(); rowIndex++) {
            List<String> row = sheet.rows().get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }
            processedRows++;
            LocalDate rowDate = firstDate(row);
            List<Integer> monetaryColumns = findMonetaryColumns(row, rowDate);
            List<String> texts = meaningfulTexts(row, monetaryColumns);

            if (monetaryColumns.isEmpty()) {
                if (rowDate != null) {
                    contextDate = rowDate;
                }
                String title = String.join(" ", texts).trim();
                if (!title.isBlank() && title.length() <= 120) {
                    context = cleanLabel(title);
                    LocalDate titleMonth = parseMonth(title);
                    if (titleMonth != null) {
                        contextDate = titleMonth;
                    }
                }
                continue;
            }

            int amountColumn = monetaryColumns.getLast();
            BigDecimal signedAmount = parseAmount(valueAt(row, amountColumn));
            if (signedAmount == null || signedAmount.signum() == 0) {
                continue;
            }
            String rowDescription = sanitizeDescription(String.join(" ", texts));
            String description = rowDescription.isBlank() ? context : rowDescription;
            if (isSummaryLabel(description)) {
                continue;
            }

            String semanticText = context + " " + description;
            InferredType inferred = inferType(semanticText, signedAmount);
            LocalDate transactionDate = rowDate != null ? rowDate : contextDate;
            boolean hasSemanticContext = inferred.confident() || suggestCategory(semanticText) != TransactionCategory.OUTROS;
            ImportConfidence confidence = transactionDate == null || !hasSemanticContext
                    ? ImportConfidence.BAIXA
                    : ImportConfidence.MEDIA;

            transactions.add(new ImportPreviewTransactionDTO(
                    inferred.type(),
                    description,
                    suggestCategory(semanticText),
                    signedAmount.abs(),
                    parsePaymentMethod(context, description),
                    inferInstallments(semanticText),
                    transactionDate,
                    confidence != ImportConfidence.BAIXA,
                    confidence,
                    source(sheet.name(), rowIndex),
                    "Contexto herdado do bloco '" + context + "'."
            ));
        }

        if (!transactions.isEmpty()) {
            warnings.add("Blocos livres foram interpretados pelo titulo e pela proximidade. Revise principalmente as sugestoes de confianca baixa.");
        }
        return new SheetAnalysis(transactions, processedRows, "BLOCOS_SEMIESTRUTURADOS", warnings);
    }

    private List<Integer> findMonetaryColumns(List<String> row, LocalDate detectedDate) {
        List<Integer> columns = new ArrayList<>();
        int nonBlank = (int) row.stream().filter(value -> value != null && !value.isBlank()).count();
        for (int index = 0; index < row.size(); index++) {
            String value = valueAt(row, index);
            if (parseFlexibleDate(value) != null) {
                continue;
            }
            if (looksMonetary(value, nonBlank) && parseAmount(value) != null) {
                columns.add(index);
            }
        }
        return columns;
    }

    private boolean looksMonetary(String raw, int nonBlankCells) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || value.matches("\\d{1,3}/\\d{1,3}")) {
            return false;
        }
        boolean explicit = value.contains("R$") || value.contains(",") || value.matches(".*\\d+\\.\\d{1,5}.*")
                || value.startsWith("-") || (value.startsWith("(") && value.endsWith(")"));
        boolean integerWithContext = nonBlankCells >= 2 && value.matches("-?\\d{3,}");
        return explicit || integerWithContext;
    }

    private int findHeader(List<String> row, String... aliases) {
        for (int index = 0; index < row.size(); index++) {
            String normalized = normalizeText(valueAt(row, index));
            for (String alias : aliases) {
                if (normalized.equals(normalizeText(alias))) {
                    return index;
                }
            }
        }
        return -1;
    }

    private MetricRule classifyMetric(String header) {
        String normalized = normalizeText(header);
        if (normalized.isBlank()) {
            return null;
        }
        if (isSummaryLabel(header)) {
            return null;
        }
        if (containsAny(normalized, "renda", "salario", "receita", "entrada", "rendimento", "dividendo", "juros receb", "pix recebido")) {
            return new MetricRule(TransactionType.RECEITA, suggestCategory(header), parsePaymentMethod("", header));
        }
        if (containsAny(normalized, "custo", "despesa", "parcela", "aluguel", "mercado", "cartao", "conta", "gasto", "saida", "combustivel", "sem categoria")) {
            return new MetricRule(TransactionType.DESPESA, suggestCategory(header), parsePaymentMethod(header, ""));
        }
        return null;
    }

    private TransactionType parseExplicitType(String rawType, BigDecimal signedAmount) {
        String normalized = normalizeText(rawType);
        if (containsAny(normalized, "receita", "entrada", "credito", "credit")) {
            return TransactionType.RECEITA;
        }
        if (containsAny(normalized, "despesa", "saida", "debito", "debit")) {
            return TransactionType.DESPESA;
        }
        return signedAmount.signum() < 0 ? TransactionType.DESPESA : TransactionType.RECEITA;
    }

    private InferredType inferType(String text, BigDecimal signedAmount) {
        if (signedAmount != null && signedAmount.signum() < 0) {
            return new InferredType(TransactionType.DESPESA, true);
        }
        String normalized = normalizeText(text);
        if (containsAny(normalized, "receita", "entrada", "receb", "salario", "renda", "rendimento", "dividendo", "venda", "cliente")) {
            return new InferredType(TransactionType.RECEITA, true);
        }
        if (containsAny(normalized, "despesa", "saida", "parcela", "mercado", "aluguel", "conta", "cartao", "combustivel", "farmacia", "pagamento", "sem categoria")) {
            return new InferredType(TransactionType.DESPESA, true);
        }
        return new InferredType(TransactionType.DESPESA, false);
    }

    private TransactionCategory parseExplicitCategory(String rawCategory, String description) {
        String normalized = normalizeText(rawCategory).toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!normalized.isBlank()) {
            try {
                return TransactionCategory.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // Usa a descricao como fallback.
            }
        }
        return suggestCategory(description);
    }

    private TransactionCategory suggestCategory(String description) {
        String normalized = normalizeText(description);
        if (containsAny(normalized, "mercado", "atacadao", "lanche", "restaurante", "ifood", "padaria", "pizza", "aliment")) {
            return TransactionCategory.ALIMENTACAO;
        }
        if (containsAny(normalized, "uber", "onibus", "combustivel", "gasolina", "posto", "metro", "taxi", "carro", "transporte")) {
            return TransactionCategory.TRANSPORTE;
        }
        if (containsAny(normalized, "aluguel", "condominio", "internet", "luz", "energia", "agua", "casa", "iptu", "moradia")) {
            return TransactionCategory.MORADIA;
        }
        if (containsAny(normalized, "medico", "consulta", "farmacia", "remedio", "hospital", "saude")) {
            return TransactionCategory.SAUDE;
        }
        if (containsAny(normalized, "cinema", "jogo", "show", "viagem", "streaming", "lazer")) {
            return TransactionCategory.LAZER;
        }
        if (containsAny(normalized, "curso", "faculdade", "livro", "caderno", "estudo", "escola", "educacao")) {
            return TransactionCategory.EDUCACAO;
        }
        if (containsAny(normalized, "fone", "tenis", "camisa", "celular", "notebook", "ferramenta", "equipamento", "compra", "santander", "parcela")) {
            return TransactionCategory.COMPRAS;
        }
        return TransactionCategory.OUTROS;
    }

    private PaymentMethod parsePaymentMethod(String rawPayment, String description) {
        String normalized = normalizeText(rawPayment + " " + description);
        if (normalized.contains("parcelado") || INSTALLMENT_PATTERN.matcher(normalized).find()) {
            return PaymentMethod.CARTAO_CREDITO_PARCELADO;
        }
        if (containsAny(normalized, "debito", "cartao de debito")) {
            return PaymentMethod.CARTAO_DEBITO;
        }
        if (containsAny(normalized, "credito", "cartao", "santander", "nubank", "itau", "bradesco")) {
            return PaymentMethod.CARTAO_CREDITO_AVISTA;
        }
        if (normalized.contains("dinheiro")) {
            return PaymentMethod.DINHEIRO;
        }
        return PaymentMethod.PIX;
    }

    private int parseInstallments(String explicit, String description) {
        String value = explicit == null ? "" : explicit.trim();
        if (value.matches("\\d{1,3}")) {
            return Math.max(1, Integer.parseInt(value));
        }
        return inferInstallments(description);
    }

    private int inferInstallments(String text) {
        Matcher matcher = INSTALLMENT_PATTERN.matcher(text == null ? "" : text);
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(2)));
        }
        return 1;
    }

    /**
     * Reconhece parcelas ja realizadas sem transformar cada linha historica em uma nova compra futura.
     */
    private List<ImportPreviewTransactionDTO> inferHistoricalInstallmentSequences(
            List<ImportPreviewTransactionDTO> source,
            List<String> warnings
    ) {
        List<ImportPreviewTransactionDTO> result = new ArrayList<>(source);
        Map<String, List<IndexedPreview>> candidates = new LinkedHashMap<>();

        for (int index = 0; index < result.size(); index++) {
            ImportPreviewTransactionDTO transaction = result.get(index);
            if (transaction.installments() != null
                    && transaction.installments() >= 2
                    && transaction.paymentMethod() != PaymentMethod.CARTAO_CREDITO_PARCELADO) {
                transaction = copyPreview(
                        transaction,
                        transaction.description(),
                        PaymentMethod.CARTAO_CREDITO_PARCELADO,
                        transaction.installments(),
                        transaction.selectedByDefault(),
                        transaction.confidence(),
                        transaction.rationale() + " Quantidade de parcelas informada em coluna propria."
                );
                result.set(index, transaction);
            }

            if (transaction.type() != TransactionType.DESPESA || transaction.transactionDate() == null) {
                continue;
            }

            String normalizedDescription = normalizeText(transaction.description());
            boolean explicitInstallment = isExplicitInstallmentLabel(normalizedDescription);
            if (isAggregateInstallmentLabel(normalizedDescription)
                    || (!explicitInstallment && isKnownRecurringExpense(normalizedDescription))) {
                continue;
            }

            String amountKey = transaction.amount() == null
                    ? ""
                    : transaction.amount().stripTrailingZeros().toPlainString();
            String key = explicitInstallment
                    ? "explicit|" + normalizedDescription
                    : "implicit|" + normalizedDescription + "|" + amountKey;
            candidates.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new IndexedPreview(index, transaction, explicitInstallment));
        }

        int inferredSequences = 0;
        for (List<IndexedPreview> group : candidates.values()) {
            group.sort(Comparator.comparing(item -> item.transaction().transactionDate()));
            List<IndexedPreview> sequence = new ArrayList<>();

            for (IndexedPreview item : group) {
                if (!sequence.isEmpty()) {
                    YearMonth previous = YearMonth.from(sequence.getLast().transaction().transactionDate());
                    YearMonth current = YearMonth.from(item.transaction().transactionDate());
                    if (!current.equals(previous.plusMonths(1))) {
                        inferredSequences += applyInstallmentSequence(result, sequence);
                        sequence = new ArrayList<>();
                    }
                }
                sequence.add(item);
            }
            inferredSequences += applyInstallmentSequence(result, sequence);
        }

        for (int index = 0; index < result.size(); index++) {
            ImportPreviewTransactionDTO transaction = result.get(index);
            if (transaction.paymentMethod() == PaymentMethod.CARTAO_CREDITO_PARCELADO
                    && (transaction.installments() == null || transaction.installments() < 2)) {
                result.set(index, copyPreview(
                        transaction,
                        transaction.description(),
                        PaymentMethod.CARTAO_CREDITO_AVISTA,
                        1,
                        false,
                        ImportConfidence.BAIXA,
                        transaction.rationale() + " Quantidade de parcelas nao identificada; revise antes de selecionar."
                ));
            }
        }

        if (inferredSequences > 0) {
            warnings.add(inferredSequences + " sequencia(s) mensal(is) de parcelas historicas foram reconhecidas automaticamente.");
        }
        return result;
    }

    private int applyInstallmentSequence(List<ImportPreviewTransactionDTO> result, List<IndexedPreview> sequence) {
        if (sequence.isEmpty()) {
            return 0;
        }

        boolean explicit = sequence.getFirst().explicitInstallment();
        int minimumSize = explicit ? 2 : 3;
        if (sequence.size() < minimumSize || sequence.size() > 360) {
            return 0;
        }

        int installments = sequence.size();
        for (int installmentIndex = 0; installmentIndex < sequence.size(); installmentIndex++) {
            IndexedPreview indexed = sequence.get(installmentIndex);
            ImportPreviewTransactionDTO transaction = indexed.transaction();
            String baseDescription = transaction.description()
                    .replaceFirst("(?i)\\s*-?\\s*parcela\\s+\\d{1,3}\\s*/\\s*\\d{1,3}$", "")
                    .trim();
            String rationale = transaction.rationale()
                    + (explicit
                    ? " Sequencia mensal de parcelas historicas reconhecida pelo rotulo."
                    : " Repeticao mensal de descricao e valor reconhecida como parcela historica.");

            result.set(indexed.index(), copyPreview(
                    transaction,
                    baseDescription + " - Parcela " + (installmentIndex + 1) + "/" + installments,
                    PaymentMethod.CARTAO_CREDITO_PARCELADO,
                    installments,
                    transaction.selectedByDefault(),
                    explicit ? ImportConfidence.ALTA : ImportConfidence.MEDIA,
                    rationale
            ));
        }
        return 1;
    }

    private ImportPreviewTransactionDTO copyPreview(
            ImportPreviewTransactionDTO source,
            String description,
            PaymentMethod paymentMethod,
            int installments,
            boolean selectedByDefault,
            ImportConfidence confidence,
            String rationale
    ) {
        return new ImportPreviewTransactionDTO(
                source.type(),
                description,
                source.category(),
                source.amount(),
                paymentMethod,
                installments,
                source.transactionDate(),
                selectedByDefault,
                confidence,
                source.source(),
                rationale
        );
    }

    private boolean isExplicitInstallmentLabel(String normalized) {
        return Pattern.compile("(^|\\s)parcela($|\\s)").matcher(normalized).find()
                || INSTALLMENT_PATTERN.matcher(normalized).find();
    }

    private boolean isAggregateInstallmentLabel(String normalized) {
        return containsAny(normalized, "parcelas", "total parcelado", "fatura", "cartao parcelas");
    }

    private boolean isKnownRecurringExpense(String normalized) {
        return containsAny(
                normalized,
                "aluguel",
                "condominio",
                "energia",
                "luz",
                "agua",
                "internet",
                "telefone",
                "mensalidade",
                "assinatura",
                "streaming",
                "mercado",
                "custo de vida",
                "sem categoria"
        );
    }

    private LocalDate firstDate(List<String> row) {
        for (String value : row) {
            LocalDate date = parseFlexibleDate(value);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private LocalDate parseFlexibleDate(String raw) {
        String value = raw == null ? "" : raw.replace("\uFEFF", "").trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // Tenta formatos seguintes.
        }
        try {
            return LocalDateTime.parse(value, ISO_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Tenta formatos seguintes.
        }
        for (DateTimeFormatter formatter : List.of(BRAZILIAN_DATE, BRAZILIAN_DASH_DATE)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato.
            }
        }
        return parseMonth(value);
    }

    private LocalDate parseMonth(String raw) {
        String value = normalizeText(raw);
        if (value.isBlank()) {
            return null;
        }
        Matcher numeric = Pattern.compile("^(0?[1-9]|1[0-2])[/-](\\d{4})$").matcher(value);
        if (numeric.find()) {
            return YearMonth.of(Integer.parseInt(numeric.group(2)), Integer.parseInt(numeric.group(1))).atDay(1);
        }
        Matcher iso = Pattern.compile("^(\\d{4})-(0?[1-9]|1[0-2])$").matcher(value);
        if (iso.find()) {
            return YearMonth.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2))).atDay(1);
        }

        String[] months = {"janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};
        Matcher yearMatcher = Pattern.compile("(19|20)\\d{2}").matcher(value);
        if (!yearMatcher.find()) {
            return null;
        }
        int year = Integer.parseInt(yearMatcher.group());
        for (int index = 0; index < months.length; index++) {
            if (value.contains(months[index]) || value.contains(months[index].substring(0, 3))) {
                return YearMonth.of(year, index + 1).atDay(1);
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || parseFlexibleDate(value) != null) {
            return null;
        }
        boolean parenthesized = value.startsWith("(") && value.endsWith(")");
        value = value.replace("R$", "").replace("$", "").replace("\u00A0", "").replaceAll("\\s", "");
        if (value.matches(".*[A-Za-zÀ-ÿ].*")) {
            return null;
        }
        value = value.replaceAll("[^0-9,.-]", "");
        if (value.isBlank() || value.equals("-") || value.equals("." ) || value.equals(",")) {
            return null;
        }

        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                value = value.replace(".", "").replace(',', '.');
            } else {
                value = value.replace(",", "");
            }
        } else if (lastComma >= 0) {
            value = value.replace(".", "").replace(',', '.');
        }

        try {
            BigDecimal amount = new BigDecimal(value);
            return parenthesized ? amount.negate() : amount;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isValidUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private char detectSeparator(String content) {
        char[] candidates = {';', ',', '\t', '|'};
        char best = ',';
        double bestScore = -1;
        String[] lines = content.split("\\R", 20);
        for (char candidate : candidates) {
            int rowsWithSeparator = 0;
            int total = 0;
            for (String line : lines) {
                int count = countOutsideQuotes(line, candidate);
                if (count > 0) {
                    rowsWithSeparator++;
                    total += count;
                }
            }
            double score = rowsWithSeparator * 10.0 + total;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private int countOutsideQuotes(String line, char separator) {
        boolean quoted = false;
        int count = 0;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (!quoted && character == separator) {
                count++;
            }
        }
        return count;
    }

    private String printableSeparator(char separator) {
        return separator == '\t' ? "TAB" : "'" + separator + "'";
    }

    private List<String> limitColumns(List<String> row) {
        return trimTrailingBlanks(row.subList(0, Math.min(row.size(), MAX_COLUMNS)));
    }

    private List<String> trimTrailingBlanks(List<String> cells) {
        int end = cells.size();
        while (end > 0 && (cells.get(end - 1) == null || cells.get(end - 1).isBlank())) {
            end--;
        }
        return new ArrayList<>(cells.subList(0, end));
    }

    private List<String> meaningfulTexts(List<String> row, List<Integer> excludedColumns) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < row.size(); index++) {
            String value = valueAt(row, index).trim();
            if (value.isBlank() || excludedColumns.contains(index) || parseFlexibleDate(value) != null || value.matches("\\d{1,3}")) {
                continue;
            }
            if (value.matches(".*[A-Za-zÀ-ÿ].*")) {
                values.add(value);
            }
        }
        return values;
    }

    private String firstMeaningfulText(List<String> row, Set<Integer> excludedColumns) {
        for (int index = 0; index < row.size(); index++) {
            String value = valueAt(row, index).trim();
            if (!excludedColumns.contains(index) && value.matches(".*[A-Za-zÀ-ÿ].*")) {
                return value;
            }
        }
        return "";
    }

    private String sanitizeDescription(String value) {
        String sanitized = value == null ? "" : value
                .replaceAll("(?i)\\b(?:cpf|ag[eê]ncia|conta)\\b\\s*[:#-]?\\s*[a-z0-9.*\\-/]+", "")
                .replaceAll("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b", "")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return sanitized.substring(0, Math.min(sanitized.length(), 255));
    }

    private String cleanLabel(String value) {
        String label = sanitizeDescription(value);
        return label.isBlank() ? "Transacao importada" : label;
    }

    private boolean isSummaryLabel(String description) {
        String normalized = normalizeText(description);
        return containsAny(
                normalized,
                "saldo",
                "patrimonio",
                "fluxo do mes",
                "total geral",
                "subtotal",
                "acumulado",
                "saidas totais",
                "despesas totais",
                "reserva operacional"
        );
    }

    private boolean isMetadataLabel(String description) {
        String normalized = normalizeText(description);
        return containsAny(normalized, "idade", "fase de vida") || normalized.equals("fase");
    }

    private boolean isBlankRow(List<String> row) {
        return row == null || row.stream().allMatch(value -> value == null || value.isBlank());
    }

    private String valueAt(List<String> row, int index) {
        return index < 0 || index >= row.size() || row.get(index) == null ? "" : row.get(index);
    }

    private String source(String sheet, int zeroBasedRow) {
        return sheet + " · linha " + (zeroBasedRow + 1);
    }

    private String typeLabel(TransactionType type) {
        return type == TransactionType.RECEITA ? "receita" : "despesa";
    }

    private boolean containsAny(String normalized, String... keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[_-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ReadDocument(String format, List<TabularSheet> sheets, List<String> warnings) {
    }

    private record TabularSheet(String name, List<List<String>> rows) {
    }

    private record HeaderPlan(
            int rowIndex,
            int date,
            int description,
            int amount,
            int type,
            int category,
            int payment,
            int installments,
            List<MetricColumn> metrics
    ) {
    }

    private record MetricColumn(int column, String label, MetricRule rule) {
    }

    private record MetricRule(TransactionType type, TransactionCategory category, PaymentMethod paymentMethod) {
    }

    private record MonthMatrixPlan(int rowIndex, List<MonthColumn> months) {
    }

    private record MonthColumn(int column, LocalDate date) {
    }

    private record InferredType(TransactionType type, boolean confident) {
    }

    private record IndexedPreview(int index, ImportPreviewTransactionDTO transaction, boolean explicitInstallment) {
    }

    private record SheetAnalysis(
            List<ImportPreviewTransactionDTO> transactions,
            int processedRows,
            String layout,
            List<String> warnings
    ) {
    }
}
