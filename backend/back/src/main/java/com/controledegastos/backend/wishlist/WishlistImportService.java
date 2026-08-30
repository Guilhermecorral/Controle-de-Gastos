package com.controledegastos.backend.wishlist;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.controledegastos.backend.wishlist.dto.WishlistImportPreviewItemDTO;
import com.controledegastos.backend.wishlist.dto.WishlistImportPreviewResponseDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WishlistImportService {

    private static final int MAX_ITEMS = 500;
    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)R\\$\\s*([0-9.]+(?:,[0-9]{1,2})?)");

    public WishlistImportPreviewResponseDTO preview(MultipartFile file) throws IOException {
        validateFile(file);
        String filename = file.getOriginalFilename() == null ? "lista" : file.getOriginalFilename();
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        List<RawWish> rawItems;
        String format;

        if (lowerFilename.endsWith(".pdf")) {
            format = "PDF";
            rawItems = parseFreeText(extractPdfText(file), filename);
        } else if (lowerFilename.endsWith(".txt")) {
            format = "TXT";
            rawItems = parseFreeText(decodeText(file.getBytes()), filename);
        } else if (lowerFilename.endsWith(".csv") || lowerFilename.endsWith(".tsv")) {
            format = lowerFilename.endsWith(".tsv") ? "TSV" : "CSV";
            rawItems = parseDelimited(decodeText(file.getBytes()), lowerFilename.endsWith(".tsv") ? '\t' : detectSeparator(decodeText(file.getBytes())), filename);
        } else if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xls")) {
            format = lowerFilename.endsWith(".xlsx") ? "XLSX" : "XLS";
            rawItems = parseWorkbook(file, filename);
        } else {
            throw new IllegalArgumentException("Envie a lista em TXT, PDF, CSV, TSV, XLS ou XLSX");
        }

        Map<String, RawWish> unique = new LinkedHashMap<>();
        for (RawWish item : rawItems) {
            String key = normalize(item.description());
            if (!key.isBlank()) {
                unique.putIfAbsent(key, item);
            }
            if (unique.size() >= MAX_ITEMS) {
                break;
            }
        }

        List<WishlistImportPreviewItemDTO> items = new ArrayList<>();
        int index = 0;
        for (RawWish raw : unique.values()) {
            BigDecimal price = raw.price() == null ? BigDecimal.ZERO : raw.price();
            items.add(new WishlistImportPreviewItemDTO(
                    index++,
                    raw.description(),
                    price,
                    WishlistItem.Priority.MEDIA,
                    suggestCategory(raw.description()),
                    raw.notes(),
                    raw.section(),
                    true,
                    price.signum() > 0
                            ? "Nome e preco reconhecidos no arquivo."
                            : "Nome reconhecido; o preco pode ser informado agora ou depois."
            ));
        }

        List<String> warnings = new ArrayList<>();
        if (items.isEmpty()) {
            warnings.add("Nenhum desejo legivel foi encontrado. Em PDF escaneado, exporte com texto ou use TXT/planilha.");
        }
        if (rawItems.size() > MAX_ITEMS) {
            warnings.add("A previa foi limitada aos primeiros 500 desejos.");
        }
        long withoutPrice = items.stream().filter(item -> item.originalPrice().signum() == 0).count();
        if (withoutPrice > 0) {
            warnings.add(withoutPrice + " desejo(s) estao sem preco e poderao ser completados depois.");
        }
        return new WishlistImportPreviewResponseDTO(format, items, warnings);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo de lista de desejos");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("A lista excede o limite de 5 MB");
        }
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (var document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(30);
            return stripper.getText(document);
        }
    }

    private List<RawWish> parseWorkbook(MultipartFile file, String source) throws IOException {
        List<RawWish> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(new Locale("pt", "BR"));
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            for (int sheetIndex = 0; sheetIndex < Math.min(20, workbook.getNumberOfSheets()); sheetIndex++) {
                var sheet = workbook.getSheetAt(sheetIndex);
                List<List<String>> rows = new ArrayList<>();
                for (var row : sheet) {
                    List<String> values = new ArrayList<>();
                    for (int column = 0; column < Math.min(30, row.getLastCellNum()); column++) {
                        values.add(formatter.formatCellValue(row.getCell(column)));
                    }
                    rows.add(values);
                }
                result.addAll(parseRows(rows, sheet.getSheetName(), source));
            }
        }
        return result;
    }

    private List<RawWish> parseDelimited(String content, char separator, String source) {
        try (var reader = new CSVReaderBuilder(new StringReader(content))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build()) {
            List<List<String>> rows = reader.readAll().stream()
                    .map(values -> List.of(values))
                    .toList();
            return parseRows(rows, "Lista importada", source);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Nao foi possivel interpretar a lista CSV/TSV", exception);
        }
    }

    private List<RawWish> parseRows(List<List<String>> rows, String defaultSection, String source) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.getFirst().stream().map(this::normalize).toList();
        int descriptionColumn = findColumn(header, "descricao", "item", "desejo", "produto", "nome");
        int priceColumn = findColumn(header, "preco", "valor", "custo");
        int notesColumn = findColumn(header, "observacao", "notas", "detalhes");
        boolean hasHeader = descriptionColumn >= 0;
        List<RawWish> result = new ArrayList<>();

        for (int rowIndex = hasHeader ? 1 : 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String description = hasHeader ? valueAt(row, descriptionColumn) : firstText(row);
            if (description.isBlank()) {
                continue;
            }
            BigDecimal price = hasHeader ? parsePrice(valueAt(row, priceColumn)) : findPrice(row);
            String notes = hasHeader ? valueAt(row, notesColumn) : source;
            result.add(new RawWish(cleanDescription(description), price, notes, defaultSection));
        }
        return result;
    }

    private List<RawWish> parseFreeText(String content, String source) {
        List<RawWish> result = new ArrayList<>();
        String section = "Lista Principal";
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.replaceFirst("^\\s*(?:[-*•]+|\\d{1,3}[.)-])\\s*", "").trim();
            if (line.isBlank()) {
                continue;
            }
            if (isSectionHeading(line)) {
                section = line.replaceFirst(":$", "").trim();
                continue;
            }
            BigDecimal price = parsePrice(line);
            String description = cleanDescription(PRICE_PATTERN.matcher(line).replaceAll("").replaceAll("[;|,-]+$", "").trim());
            if (!description.isBlank()) {
                result.add(new RawWish(description, price, source, section));
            }
        }
        return result;
    }

    private boolean isSectionHeading(String line) {
        return line.endsWith(":") || (line.length() <= 60 && line.equals(line.toUpperCase(Locale.ROOT)) && line.chars().anyMatch(Character::isLetter));
    }

    private char detectSeparator(String content) {
        String firstLine = content.lines().findFirst().orElse("");
        return firstLine.chars().filter(value -> value == ';').count() >= firstLine.chars().filter(value -> value == ',').count() ? ';' : ',';
    }

    private String decodeText(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        return utf8.contains("�") ? new String(bytes, WINDOWS_1252) : utf8;
    }

    private int findColumn(List<String> header, String... aliases) {
        for (int index = 0; index < header.size(); index++) {
            for (String alias : aliases) {
                if (header.get(index).contains(alias)) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String firstText(List<String> row) {
        return row.stream().filter(value -> value != null && !value.isBlank() && parsePrice(value) == null).findFirst().orElse("");
    }

    private BigDecimal findPrice(List<String> row) {
        return row.stream().map(this::parsePrice).filter(value -> value != null).findFirst().orElse(null);
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = PRICE_PATTERN.matcher(raw);
        boolean foundCurrency = matcher.find();
        String value = foundCurrency ? matcher.group(1) : raw.trim();
        if (!foundCurrency && !raw.matches("[0-9., ]+")) {
            return null;
        }
        value = value.replace(" ", "");
        if (value.contains(",")) {
            value = value.replace(".", "").replace(',', '.');
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            return parsed.signum() >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private WishlistItem.WishlistCategory suggestCategory(String description) {
        String value = normalize(description);
        if (containsAny(value, "viagem", "cinema", "jogo", "show", "lazer")) return WishlistItem.WishlistCategory.LAZER;
        if (containsAny(value, "curso", "livro", "faculdade", "estudo")) return WishlistItem.WishlistCategory.EDUCACAO;
        if (containsAny(value, "casa", "quarto", "moveis", "reforma")) return WishlistItem.WishlistCategory.MORADIA;
        if (containsAny(value, "bicicleta", "carro", "moto", "transporte")) return WishlistItem.WishlistCategory.TRANSPORTE;
        if (containsAny(value, "academia", "consulta", "saude")) return WishlistItem.WishlistCategory.SAUDE;
        return WishlistItem.WishlistCategory.COMPRAS;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private String cleanDescription(String value) {
        String cleaned = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return cleaned.substring(0, Math.min(cleaned.length(), 255));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String valueAt(List<String> row, int index) {
        return index < 0 || index >= row.size() || row.get(index) == null ? "" : row.get(index).trim();
    }

    private record RawWish(String description, BigDecimal price, String notes, String section) {
    }
}
