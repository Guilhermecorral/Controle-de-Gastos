package com.controledegastos.backend.investments;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-stage reader for native SINACOR notes. It extracts only a review draft;
 * field confidence is intentionally conservative because broker layouts vary.
 */
@Service
@ConditionalOnProperty(name = "app.investments.imports.pdf-enabled", havingValue = "true", matchIfMissing = true)
public class SinacorPdfParser {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern TRADE_DATE = Pattern.compile("(?im)Data\\s+(?:do\\s+)?preg[aã]o\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern TRADE_LINE = Pattern.compile(
            "(?im)^\\s*([CV])\\s+(?:VISTA\\s+)?([A-Z]{4}\\d{1,2})\\s+([0-9.,]+)\\s+([0-9.,]+)\\s+([0-9.,]+)\\s*(?:[DC])?\\s*$"
    );
    private static final Pattern BROKERAGE = Pattern.compile("(?im)CORRETAGEM\\s*:?\\s*([0-9.,]+)");
    private static final Pattern B3_FEES = Pattern.compile("(?im)(?:TAXA\\s+DE\\s+LIQUIDA[CÇ][AÃ]O|EMOLUMENTOS|TAXA\\s+DE\\s+REGISTRO)\\s*:?\\s*([0-9.,]+)");
    private static final Pattern WITHHELD_TAX = Pattern.compile("(?im)IRRF(?:\\s+SOBRE\\s+OPERA[CÇ][OÕ]ES)?\\s*:?\\s*([0-9.,]+)");

    List<InvestmentImportService.ParsedRow> parse(MultipartFile file, List<String> warnings) {
        String text = extractText(file);
        LocalDate eventDate = parseTradeDate(text);
        List<TradeDraft> drafts = parseTrades(text);
        if (drafts.isEmpty()) {
            warnings.add("Não identificamos operações SINACOR no PDF. Revise se o documento possui texto selecionável e a tabela de negócios realizados.");
            return List.of();
        }

        OperationCosts totalCosts = extractCosts(text);
        BigDecimal totalGross = drafts.stream().map(TradeDraft::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<InvestmentImportService.ParsedRow> rows = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            TradeDraft draft = drafts.get(index);
            OperationCosts allocatedCosts = allocateCosts(totalCosts, draft.grossAmount(), totalGross, index == drafts.size() - 1, rows);
            rows.add(new InvestmentImportService.ParsedRow(
                    index + 1,
                    draft.movementType(),
                    draft.symbol().endsWith("11") ? InvestmentPosition.AssetType.FII : InvestmentPosition.AssetType.ACAO,
                    draft.symbol(),
                    draft.symbol(),
                    "BR", "B3", "BRL",
                    draft.quantity(), draft.unitPrice(), allocatedCosts, BigDecimal.ONE, eventDate,
                    "Leitura experimental de PDF SINACOR. Revise ativo, data, valores e custos antes de salvar."
            ));
        }
        warnings.add("PDF SINACOR lido como prévia. Custos e IRRF foram rateados entre as operações quando o documento não os informa por linha.");
        return rows;
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Este PDF não possui texto selecionável. Envie a nota original baixada da corretora; PDF escaneado ainda não é suportado.");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o PDF de investimentos. Envie uma nota SINACOR em PDF nativo.");
        }
    }

    private LocalDate parseTradeDate(String text) {
        Matcher matcher = TRADE_DATE.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Não encontramos a data do pregão no PDF. Preencha a operação manualmente ou use CSV/Excel.");
        }
        try {
            return LocalDate.parse(matcher.group(1), BR_DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("A data do pregão no PDF é inválida.");
        }
    }

    private List<TradeDraft> parseTrades(String text) {
        List<TradeDraft> drafts = new ArrayList<>();
        Matcher matcher = TRADE_LINE.matcher(text);
        while (matcher.find()) {
            BigDecimal quantity = decimal(matcher.group(3));
            BigDecimal unitPrice = decimal(matcher.group(4));
            BigDecimal gross = decimal(matcher.group(5));
            if (quantity.signum() <= 0 || unitPrice.signum() <= 0 || gross.signum() <= 0) continue;
            drafts.add(new TradeDraft(
                    "V".equals(matcher.group(1)) ? InvestmentMovement.MovementType.VENDA : InvestmentMovement.MovementType.COMPRA,
                    matcher.group(2).toUpperCase(Locale.ROOT), quantity, unitPrice, gross
            ));
        }
        return drafts;
    }

    private OperationCosts extractCosts(String text) {
        return new OperationCosts(firstAmount(BROKERAGE, text), sumAmounts(B3_FEES, text), BigDecimal.ZERO, firstAmount(WITHHELD_TAX, text));
    }

    private OperationCosts allocateCosts(OperationCosts total, BigDecimal gross, BigDecimal totalGross, boolean lastRow,
                                         List<InvestmentImportService.ParsedRow> allocatedRows) {
        if (totalGross.signum() <= 0) return total;
        if (lastRow) {
            return new OperationCosts(
                    total.getBrokerageFee().subtract(sum(allocatedRows, OperationCosts::getBrokerageFee)),
                    total.getB3Fee().subtract(sum(allocatedRows, OperationCosts::getB3Fee)),
                    total.getOtherCosts().subtract(sum(allocatedRows, OperationCosts::getOtherCosts)),
                    total.getWithheldTax().subtract(sum(allocatedRows, OperationCosts::getWithheldTax))
            );
        }
        BigDecimal ratio = gross.divide(totalGross, 12, RoundingMode.HALF_UP);
        return new OperationCosts(
                money(total.getBrokerageFee().multiply(ratio)), money(total.getB3Fee().multiply(ratio)),
                money(total.getOtherCosts().multiply(ratio)), money(total.getWithheldTax().multiply(ratio))
        );
    }

    private BigDecimal sum(List<InvestmentImportService.ParsedRow> rows,
                           java.util.function.Function<OperationCosts, BigDecimal> field) {
        return rows.stream().map(InvestmentImportService.ParsedRow::costs).map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal firstAmount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? decimal(matcher.group(1)) : BigDecimal.ZERO;
    }

    private BigDecimal sumAmounts(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        BigDecimal result = BigDecimal.ZERO;
        while (matcher.find()) result = result.add(decimal(matcher.group(1)));
        return result;
    }

    private BigDecimal decimal(String value) {
        String normalized = value.replaceAll("[^0-9,.-]", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.lastIndexOf(',') > normalized.lastIndexOf('.')
                    ? normalized.replace(".", "").replace(',', '.') : normalized.replace(",", "");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(',', '.');
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record TradeDraft(InvestmentMovement.MovementType movementType, String symbol, BigDecimal quantity,
                              BigDecimal unitPrice, BigDecimal grossAmount) { }
}
