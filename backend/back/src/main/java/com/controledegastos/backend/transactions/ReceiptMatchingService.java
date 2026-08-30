package com.controledegastos.backend.transactions;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.DTO.ReceiptMatchCandidateDTO;
import com.controledegastos.backend.transactions.DTO.ReceiptMatchPreviewDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReceiptMatchingService {

    private static final int MAX_FILES = 30;
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final long MAX_BATCH_BYTES = 50 * 1024 * 1024;
    private static final Pattern MONEY_PATTERN = Pattern.compile("(?i)R\\$\\s*([0-9.]+,[0-9]{2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");
    private final AuthenticatedUserService authenticatedUserService;
    private final TransactionRepository transactionRepository;

    public ReceiptMatchingService(
            AuthenticatedUserService authenticatedUserService,
            TransactionRepository transactionRepository
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.transactionRepository = transactionRepository;
    }

    public List<ReceiptMatchPreviewDTO> preview(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma nota fiscal");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Analise no maximo 30 notas fiscais por vez");
        }
        if (files.stream().mapToLong(MultipartFile::getSize).sum() > MAX_BATCH_BYTES) {
            throw new IllegalArgumentException("O lote de notas fiscais pode ter no maximo 50 MB");
        }

        var user = authenticatedUserService.getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findAllByUserOrderByTransactionDateDesc(user);
        List<ReceiptMatchPreviewDTO> previews = new ArrayList<>();

        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            validate(file);
            String filename = file.getOriginalFilename() == null ? "nota-" + (index + 1) : file.getOriginalFilename();
            String content = extractSearchableContent(file, filename);
            Set<BigDecimal> amounts = extractAmounts(content);
            Set<LocalDate> dates = extractDates(content);
            String normalizedContent = normalize(content);

            List<ReceiptMatchCandidateDTO> candidates = transactions.stream()
                    .map(transaction -> score(transaction, amounts, dates, normalizedContent))
                    .filter(candidate -> candidate.score() > 0)
                    .sorted(Comparator.comparingInt(ReceiptMatchCandidateDTO::score).reversed())
                    .limit(5)
                    .toList();

            int bestScore = candidates.isEmpty() ? 0 : candidates.getFirst().score();
            String confidence = bestScore >= 75 ? "ALTA" : bestScore >= 45 ? "MEDIA" : "BAIXA";
            String rationale = isPdf(file)
                    ? "PDF textual analisado por valor, data e palavras da descricao. Confirme antes de anexar."
                    : "Imagem protegida sem OCR: sugestao baseada no nome do arquivo. Confirme manualmente.";
            previews.add(new ReceiptMatchPreviewDTO(index, filename, confidence, candidates, rationale));
        }
        return previews;
    }

    private ReceiptMatchCandidateDTO score(
            Transaction transaction,
            Set<BigDecimal> amounts,
            Set<LocalDate> dates,
            String content
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (amounts.stream().anyMatch(amount -> amount.compareTo(transaction.getAmount()) == 0)) {
            score += 50;
            reasons.add("mesmo valor");
        }
        if (dates.contains(transaction.getTransactionDate())) {
            score += 30;
            reasons.add("mesma data");
        } else if (dates.stream().anyMatch(date -> Math.abs(date.toEpochDay() - transaction.getTransactionDate().toEpochDay()) <= 3)) {
            score += 15;
            reasons.add("data proxima");
        }

        int matchedTokens = 0;
        for (String token : normalize(transaction.getDescription()).split(" ")) {
            if (token.length() >= 4 && content.contains(token)) {
                matchedTokens++;
            }
        }
        if (matchedTokens > 0) {
            score += Math.min(20, matchedTokens * 5);
            reasons.add("descricao semelhante");
        }
        return new ReceiptMatchCandidateDTO(transaction.getId(), score, String.join(", ", reasons));
    }

    private String extractSearchableContent(MultipartFile file, String filename) {
        if (!isPdf(file)) {
            return filename;
        }
        try (var document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(20);
            return filename + " " + stripper.getText(document);
        } catch (Exception exception) {
            return filename;
        }
    }

    private Set<BigDecimal> extractAmounts(String content) {
        Set<BigDecimal> amounts = new HashSet<>();
        Matcher matcher = MONEY_PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                amounts.add(new BigDecimal(matcher.group(1).replace(".", "").replace(',', '.')));
            } catch (NumberFormatException ignored) {
                // Ignora valores que nao representam moeda valida.
            }
        }
        return amounts;
    }

    private Set<LocalDate> extractDates(String content) {
        Set<LocalDate> dates = new HashSet<>();
        Matcher matcher = DATE_PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                dates.add(LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd/MM/uuuu")));
            } catch (DateTimeParseException ignored) {
                // Ignora datas invalidas encontradas no texto.
            }
        }
        return dates;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Uma das notas fiscais esta vazia");
        if (file.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("Cada nota fiscal pode ter no maximo 10 MB");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(filename.endsWith(".pdf") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))) {
            throw new IllegalArgumentException("Envie notas em PDF, JPG ou PNG");
        }
    }

    private boolean isPdf(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        return filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType());
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}
