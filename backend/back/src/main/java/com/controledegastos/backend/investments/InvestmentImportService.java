package com.controledegastos.backend.investments;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.InvestmentDtos.TradeRequest;
import com.controledegastos.backend.investments.InvestmentImportDtos.ConfirmItemRequest;
import com.controledegastos.backend.investments.InvestmentImportDtos.ConfirmRequest;
import com.controledegastos.backend.investments.InvestmentImportDtos.ConfirmResponse;
import com.controledegastos.backend.investments.InvestmentImportDtos.PreviewItemResponse;
import com.controledegastos.backend.investments.InvestmentImportDtos.PreviewResponse;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.user.User;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê arquivos de investimentos para staging, sem gravar na carteira até a confirmação explícita. */
@Service
@RequiredArgsConstructor
public class InvestmentImportService {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 5_000;
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final AuthenticatedUserService authenticatedUserService;
    private final InvestmentImportBatchRepository batchRepository;
    private final InvestmentImportItemRepository itemRepository;
    private final InvestmentMovementRepository movementRepository;
    private final InvestmentService investmentService;
    private final ObjectProvider<SinacorPdfParser> sinacorPdfParser;

    @Transactional
    public PreviewResponse preview(MultipartFile file) {
        validateFile(file);
        String filename = file.getOriginalFilename() == null ? "importacao" : file.getOriginalFilename();
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        List<String> warnings = new ArrayList<>();
        List<ParsedRow> parsed = switch (extension) {
            case "csv", "tsv" -> parseCsv(file, "tsv".equals(extension), warnings);
            case "xls", "xlsx" -> parseWorkbook(file, warnings);
            case "ofx" -> parseInvestmentOfx(file, warnings);
            case "pdf" -> parseSinacorPdf(file, warnings);
            default -> throw new IllegalArgumentException("Envie um arquivo CSV, XLS, XLSX, OFX ou PDF SINACOR.");
        };

        if (parsed.size() > MAX_ROWS) {
            throw new IllegalArgumentException("O arquivo possui mais de " + MAX_ROWS + " operações.");
        }

        User user = authenticatedUserService.getAuthenticatedUser();
        InvestmentImportBatch batch = batchRepository.save(InvestmentImportBatch.builder()
                .user(user).originalFilename(filename).sourceFormat(extension.toUpperCase(Locale.ROOT)).build());
        List<InvestmentMovement> history = movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user);
        List<InvestmentImportItem> items = parsed.stream()
                .map(row -> toStagingItem(batch, row, history))
                .toList();
        itemRepository.saveAll(items);
        if (items.isEmpty()) {
            warnings.add("Nenhuma operação de investimento foi identificada. Em OFX bancário comum, use a importação de extrato.");
        }
        return new PreviewResponse(batch.getId(), filename, batch.getSourceFormat(), items.stream().map(this::toResponse).toList(), warnings);
    }

    @Transactional
    public ConfirmResponse confirm(Long batchId, ConfirmRequest request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        InvestmentImportBatch batch = batchRepository.findByIdAndUser(batchId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de importação não encontrado"));
        if (batch.getStatus() != InvestmentImportBatch.Status.EM_REVISAO) {
            throw new IllegalArgumentException("Este lote já foi finalizado e não pode ser importado novamente");
        }
        Map<Long, InvestmentImportItem> stored = new HashMap<>();
        itemRepository.findAllByBatchOrderBySourceRowAsc(batch).forEach(item -> stored.put(item.getId(), item));
        int imported = 0;
        for (ConfirmItemRequest requestItem : request.items()) {
            if (!requestItem.selected()) continue;
            InvestmentImportItem item = stored.get(requestItem.id());
            if (item == null) throw new IllegalArgumentException("Uma linha não pertence a este lote de importação");
            applyReview(item, requestItem);
            validateItem(item);
            investmentService.recordTrade(new TradeRequest(
                    null, item.getMovementType(), item.getAssetType(), item.getSymbol(), null, item.getName(),
                    item.getMarket(), item.getExchange(), item.getCurrency(), item.getQuantity(), item.getUnitPrice(),
                    item.getCosts().total(), item.getEventDate(), item.getCosts(), item.getExchangeRate(),
                    UUID.nameUUIDFromBytes(("investment-import:" + batchId + ":" + item.getId()).getBytes(StandardCharsets.UTF_8)).toString()
            ));
            imported++;
        }
        if (imported == 0) throw new IllegalArgumentException("Selecione ao menos uma operação para salvar");
        batch.setStatus(InvestmentImportBatch.Status.CONFIRMADO);
        batch.setConfirmedAt(java.time.LocalDateTime.now());
        batchRepository.save(batch);
        return new ConfirmResponse(imported, imported == 1 ? "1 operação foi adicionada à carteira." : imported + " operações foram adicionadas à carteira.");
    }

    private InvestmentImportItem toStagingItem(InvestmentImportBatch batch, ParsedRow row, List<InvestmentMovement> history) {
        boolean duplicate = history.stream().anyMatch(item -> sameOperation(item, row));
        String warning = row.warning();
        if (duplicate) warning = joinWarning(warning, "Possível duplicidade: já existe uma operação com data, ticker, quantidade e preço iguais.");
        return InvestmentImportItem.builder()
                .batch(batch).sourceRow(row.sourceRow()).movementType(row.movementType()).assetType(row.assetType())
                .symbol(row.symbol()).name(row.name()).market(row.market()).exchange(row.exchange()).currency(row.currency())
                .quantity(row.quantity()).unitPrice(row.unitPrice()).costs(row.costs()).exchangeRate(row.exchangeRate())
                .eventDate(row.eventDate()).possibleDuplicate(duplicate).warning(warning == null ? "" : warning).build();
    }

    private boolean sameOperation(InvestmentMovement movement, ParsedRow row) {
        InvestmentPosition position = movement.getPosition();
        return movement.getMovementType() == row.movementType()
                && movement.getEventDate().equals(row.eventDate())
                && position.getSymbol() != null && position.getSymbol().equalsIgnoreCase(row.symbol())
                && movement.getQuantity() != null && movement.getQuantity().compareTo(row.quantity()) == 0
                && movement.getUnitPrice() != null && movement.getUnitPrice().compareTo(row.unitPrice()) == 0;
    }

    private void applyReview(InvestmentImportItem item, ConfirmItemRequest input) {
        if (input.movementType() != null) item.setMovementType(input.movementType());
        if (input.assetType() != null) item.setAssetType(input.assetType());
        if (input.symbol() != null) item.setSymbol(normalizeSymbol(input.symbol()));
        if (input.name() != null) item.setName(input.name().trim());
        if (input.market() != null) item.setMarket(input.market().trim().toUpperCase(Locale.ROOT));
        if (input.exchange() != null) item.setExchange(blankToNull(input.exchange()));
        if (input.currency() != null) item.setCurrency(input.currency().trim().toUpperCase(Locale.ROOT));
        if (input.quantity() != null) item.setQuantity(input.quantity());
        if (input.unitPrice() != null) item.setUnitPrice(input.unitPrice());
        if (input.costs() != null) item.setCosts(normalizeCosts(input.costs()));
        if (input.exchangeRate() != null) item.setExchangeRate(input.exchangeRate());
        if (input.eventDate() != null) item.setEventDate(input.eventDate());
    }

    private void validateItem(InvestmentImportItem item) {
        if (item.getMovementType() != InvestmentMovement.MovementType.COMPRA && item.getMovementType() != InvestmentMovement.MovementType.VENDA)
            throw new IllegalArgumentException("A linha " + item.getSourceRow() + " precisa ser compra ou venda");
        if (item.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA)
            throw new IllegalArgumentException("Use o fluxo de renda fixa para a linha " + item.getSourceRow());
        if (item.getSymbol() == null || item.getSymbol().isBlank() || item.getName().isBlank())
            throw new IllegalArgumentException("Informe ativo e nome na linha " + item.getSourceRow());
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0 || item.getUnitPrice() == null || item.getUnitPrice().signum() <= 0)
            throw new IllegalArgumentException("Quantidade e preço devem ser maiores que zero na linha " + item.getSourceRow());
        if (item.getEventDate() == null || item.getEventDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("A data da linha " + item.getSourceRow() + " deve ser hoje ou anterior");
        if (item.getExchangeRate() == null || item.getExchangeRate().signum() <= 0)
            throw new IllegalArgumentException("Informe o câmbio da linha " + item.getSourceRow());
    }

    private PreviewItemResponse toResponse(InvestmentImportItem item) {
        return new PreviewItemResponse(item.getId(), item.getSourceRow(), true, item.isPossibleDuplicate(), item.getWarning(),
                item.getMovementType(), item.getAssetType(), item.getSymbol(), item.getName(), item.getMarket(), item.getExchange(),
                item.getCurrency(), item.getQuantity(), item.getUnitPrice(), item.getCosts(), item.getExchangeRate(), item.getEventDate());
    }

    private List<ParsedRow> parseCsv(MultipartFile file, boolean tsv, List<String> warnings) {
        try (CSVReader reader = new CSVReaderBuilder(new StringReader(new String(file.getBytes(), StandardCharsets.UTF_8)))
                .withCSVParser(new CSVParserBuilder().withSeparator(tsv ? '\t' : detectSeparator(file.getBytes())).build()).build()) {
            List<String[]> rows = reader.readAll();
            return parseTable(rows, warnings);
        } catch (IOException | CsvException exception) {
            throw new IllegalArgumentException("Não foi possível ler o CSV: " + exception.getMessage());
        }
    }

    private List<ParsedRow> parseWorkbook(MultipartFile file, List<String> warnings) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            List<ParsedRow> result = new ArrayList<>();
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                List<String[]> rows = new ArrayList<>();
                for (Row row : workbook.getSheetAt(sheetIndex)) {
                    String[] values = new String[Math.max(1, row.getLastCellNum())];
                    for (int column = 0; column < values.length; column++) {
                        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        values[column] = cell == null ? "" : formatter.formatCellValue(cell);
                    }
                    rows.add(values);
                }
                result.addAll(parseTable(rows, warnings));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Não foi possível ler a planilha: " + exception.getMessage());
        }
    }

    private List<ParsedRow> parseTable(List<String[]> rows, List<String> warnings) {
        if (rows.isEmpty()) return List.of();
        Map<String, Integer> headers = headers(rows.getFirst());
        if (!headers.containsKey("ticker") && !headers.containsKey("ativo") && !headers.containsKey("symbol")) {
            warnings.add("Não encontramos a coluna Ticker/Ativo. Use cabeçalhos como Data, Ticker, Operação, Quantidade e Preço.");
            return List.of();
        }
        List<ParsedRow> parsed = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            String[] values = rows.get(index);
            String ticker = text(values, headers, "ticker", "ativo", "symbol", "codigo");
            if (ticker.isBlank()) continue;
            try {
                parsed.add(buildRow(index + 1, ticker, text(values, headers, "operacao", "tipo", "operation", "side"),
                        text(values, headers, "data", "date", "dataoperacao"), text(values, headers, "quantidade", "qtd", "quantity"),
                        text(values, headers, "preco", "precounitario", "unitprice", "valorunitario"),
                        text(values, headers, "corretagem", "brokerage"), text(values, headers, "taxab3", "b3fee", "emolumentos"),
                        text(values, headers, "outroscustos", "fees", "custos"), text(values, headers, "irrf", "withheldtax"),
                        text(values, headers, "cambio", "exchangerate"), text(values, headers, "nome", "name"),
                        text(values, headers, "classe", "assettype")));
            } catch (IllegalArgumentException exception) {
                warnings.add("Linha " + (index + 1) + " ignorada: " + exception.getMessage());
            }
        }
        return parsed;
    }

    private List<ParsedRow> parseInvestmentOfx(MultipartFile file, List<String> warnings) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
            Pattern operation = Pattern.compile("(?is)<(BUY(?:STOCK|MF|OPT)|SELL(?:STOCK|MF|OPT))>(.*?)(?=<(?:BUY|SELL)(?:STOCK|MF|OPT)>|</INVTRANLIST>|$)");
            Matcher matcher = operation.matcher(content);
            List<ParsedRow> rows = new ArrayList<>();
            int sourceRow = 1;
            while (matcher.find()) {
                String block = matcher.group(2);
                String ticker = tag(block, "TICKER", "UNIQUEID");
                if (ticker.isBlank()) { warnings.add("Uma operação OFX não informou ticker e foi ignorada."); continue; }
                rows.add(buildRow(sourceRow++, ticker, matcher.group(1).startsWith("BUY") ? "COMPRA" : "VENDA",
                        tag(block, "DTTRADE"), tag(block, "UNITS"), tag(block, "UNITPRICE"), tag(block, "COMMISSION"),
                        tag(block, "FEES"), tag(block, "LOAD"), tag(block, "WITHHOLDING", "TAXES"), "1",
                        ticker, ""));
            }
            if (rows.isEmpty()) warnings.add("Este OFX não contém blocos de investimento (BUY/SELL). Para extrato bancário, use Importar extrato.");
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o OFX: " + exception.getMessage());
        }
    }

    private List<ParsedRow> parseSinacorPdf(MultipartFile file, List<String> warnings) {
        SinacorPdfParser parser = sinacorPdfParser.getIfAvailable();
        if (parser == null) {
            throw new IllegalArgumentException("A leitura de PDF de investimentos está desativada neste ambiente.");
        }
        return parser.parse(file, warnings);
    }

    private ParsedRow buildRow(int sourceRow, String ticker, String operation, String date, String quantity, String price,
                               String brokerage, String b3Fee, String otherCosts, String irrf, String exchangeRate,
                               String name, String assetType) {
        InvestmentMovement.MovementType movementType = normalizeOperation(operation);
        String symbol = normalizeSymbol(ticker);
        return new ParsedRow(sourceRow, movementType, normalizeAssetType(assetType), symbol,
                blankToNull(name) == null ? symbol : name.trim(), "BR", "B3", "BRL", parseNumber(quantity), parseNumber(price),
                new OperationCosts(parseOptional(brokerage), parseOptional(b3Fee), parseOptional(otherCosts), parseOptional(irrf)),
                parseOptional(exchangeRate).signum() > 0 ? parseOptional(exchangeRate) : BigDecimal.ONE, parseDate(date), "");
    }

    private Map<String, Integer> headers(String[] row) {
        Map<String, Integer> result = new HashMap<>();
        for (int index = 0; index < row.length; index++) result.put(normalizeHeader(row[index]), index);
        return result;
    }

    private String text(String[] row, Map<String, Integer> headers, String... aliases) {
        for (String alias : aliases) {
            Integer index = headers.get(alias);
            if (index != null && index < row.length) return row[index] == null ? "" : row[index].trim();
        }
        return "";
    }

    private char detectSeparator(byte[] bytes) {
        String firstLine = new String(bytes, StandardCharsets.UTF_8).lines().findFirst().orElse("");
        return firstLine.chars().filter(value -> value == ';').count() > firstLine.chars().filter(value -> value == ',').count() ? ';' : ',';
    }

    private String tag(String block, String... names) {
        for (String name : names) {
            Matcher matcher = Pattern.compile("(?is)<" + name + ">(?:\\s*)?([^<\\r\\n]+)").matcher(block);
            if (matcher.find()) return matcher.group(1).trim();
        }
        return "";
    }

    private InvestmentMovement.MovementType normalizeOperation(String value) {
        String normalized = normalizeHeader(value);
        if (normalized.contains("venda") || normalized.contains("sell")) return InvestmentMovement.MovementType.VENDA;
        if (normalized.contains("compra") || normalized.contains("buy")) return InvestmentMovement.MovementType.COMPRA;
        throw new IllegalArgumentException("operação deve ser COMPRA ou VENDA");
    }

    private InvestmentPosition.AssetType normalizeAssetType(String value) {
        String normalized = normalizeHeader(value);
        if (normalized.contains("fiagro") || normalized.contains("fundoagro")) return InvestmentPosition.AssetType.FIAGRO;
        if (normalized.contains("fii") || normalized.contains("fundoimobiliario")) return InvestmentPosition.AssetType.FII;
        if (normalized.contains("cripto") || normalized.contains("crypto")) return InvestmentPosition.AssetType.CRIPTO;
        return InvestmentPosition.AssetType.ACAO;
    }

    private BigDecimal parseNumber(String value) {
        BigDecimal parsed = parseOptional(value);
        if (parsed.signum() <= 0) throw new IllegalArgumentException("quantidade e preço precisam ser maiores que zero");
        return parsed;
    }

    private BigDecimal parseOptional(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        String normalized = value.replaceAll("[^0-9,.-]", "").trim();
        if (normalized.contains(",") && normalized.contains(".")) normalized = normalized.lastIndexOf(',') > normalized.lastIndexOf('.')
                ? normalized.replace(".", "").replace(',', '.') : normalized.replace(",", "");
        else if (normalized.contains(",")) normalized = normalized.replace(',', '.');
        try { return new BigDecimal(normalized); } catch (NumberFormatException exception) { throw new IllegalArgumentException("número inválido: " + value); }
    }

    private LocalDate parseDate(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() >= 8 && normalized.matches("\\d{8}.*")) return LocalDate.parse(normalized.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        try { return LocalDate.parse(normalized, BR_DATE); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(normalized); } catch (DateTimeParseException exception) { throw new IllegalArgumentException("data inválida: " + value); }
    }

    private String normalizeSymbol(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", ""); }
    private String normalizeHeader(String value) { return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private OperationCosts normalizeCosts(OperationCosts costs) { return new OperationCosts(zero(costs.getBrokerageFee()), zero(costs.getB3Fee()), zero(costs.getOtherCosts()), zero(costs.getWithheldTax())); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String joinWarning(String first, String second) { return first == null || first.isBlank() ? second : first + " " + second; }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione um arquivo CSV, Excel, OFX ou PDF SINACOR.");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("O arquivo pode ter no máximo 5 MB.");
    }

    static record ParsedRow(int sourceRow, InvestmentMovement.MovementType movementType, InvestmentPosition.AssetType assetType,
                             String symbol, String name, String market, String exchange, String currency, BigDecimal quantity,
                             BigDecimal unitPrice, OperationCosts costs, BigDecimal exchangeRate, LocalDate eventDate, String warning) { }
}
