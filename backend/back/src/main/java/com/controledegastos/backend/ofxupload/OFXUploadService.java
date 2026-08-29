package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.Transaction;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.exceptions.CsvException;
import com.webcohesion.ofx4j.domain.data.MessageSetType;
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope;
import com.webcohesion.ofx4j.domain.data.ResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponseTransaction;
import com.webcohesion.ofx4j.domain.data.banking.BankingResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.common.StatementResponse;
import com.webcohesion.ofx4j.domain.data.common.TransactionList;
import com.webcohesion.ofx4j.domain.data.common.TransactionType;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardStatementResponseTransaction;
import com.webcohesion.ofx4j.io.AggregateUnmarshaller;
import com.webcohesion.ofx4j.io.OFXParseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parseia arquivos OFX e CSV e converte tudo em dados de transacao para a etapa de pre-visualizacao.
 */
@Service
public class OFXUploadService {

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter BRAZILIAN_CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public List<TransactionRequestDTO> parseOFX(MultipartFile file) throws IOException, OFXParseException {
        try (InputStream inputStream = file.getInputStream()) {
            ResponseEnvelope envelope = new AggregateUnmarshaller<>(ResponseEnvelope.class).unmarshal(inputStream);
            List<TransactionRequestDTO> transactions = new ArrayList<>();

            collectTransactions(envelope.getMessageSet(MessageSetType.banking), transactions);
            collectTransactions(envelope.getMessageSet(MessageSetType.creditcard), transactions);

            return transactions;
        }
    }

    public List<TransactionRequestDTO> parseCSV(MultipartFile file) throws IOException, CsvException {
        List<TransactionRequestDTO> transactions = new ArrayList<>();
        char separator = detectCsvSeparator(file);

        try (InputStream inputStream = file.getInputStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                     .build()) {
            String[] line;

            while ((line = csvReader.readNext()) != null) {
                if (line.length < 3) {
                    continue;
                }

                TransactionRequestDTO dto = convertCSVLineToDTO(line);
                if (dto != null) {
                    transactions.add(dto);
                }
            }
        }

        return transactions;
    }

    private char detectCsvSeparator(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return ',';
            }

            long semicolons = firstLine.chars().filter(character -> character == ';').count();
            long commas = firstLine.chars().filter(character -> character == ',').count();
            return semicolons > commas ? ';' : ',';
        }
    }

    private void collectTransactions(ResponseMessageSet messageSet, List<TransactionRequestDTO> output) {
        if (messageSet == null) {
            return;
        }

        if (messageSet instanceof BankingResponseMessageSet bankingResponseMessageSet) {
            for (BankStatementResponseTransaction transaction : bankingResponseMessageSet.getStatementResponses()) {
                appendStatementTransactions(transaction != null ? transaction.getMessage() : null, output);
            }
            return;
        }

        if (messageSet instanceof CreditCardResponseMessageSet creditCardResponseMessageSet) {
            for (CreditCardStatementResponseTransaction transaction : creditCardResponseMessageSet.getStatementResponses()) {
                appendStatementTransactions(transaction != null ? transaction.getMessage() : null, output);
            }
        }
    }

    private void appendStatementTransactions(StatementResponse statementResponse, List<TransactionRequestDTO> output) {
        if (statementResponse == null) {
            return;
        }

        TransactionList transactionList = statementResponse.getTransactionList();
        if (transactionList == null || transactionList.getTransactions() == null) {
            return;
        }

        transactionList.getTransactions().forEach(ofxTransaction -> {
            TransactionRequestDTO dto = convertOfxTransactionToDTO(ofxTransaction);
            if (dto != null) {
                output.add(dto);
            }
        });
    }

    private TransactionRequestDTO convertOfxTransactionToDTO(com.webcohesion.ofx4j.domain.data.common.Transaction ofxTransaction) {
        if (ofxTransaction == null || ofxTransaction.getDatePosted() == null || ofxTransaction.getAmount() == null) {
            return null;
        }

        BigDecimal amount = BigDecimal.valueOf(ofxTransaction.getAmount()).abs();
        Transaction.TransactionType type = inferType(ofxTransaction.getTransactionType(), ofxTransaction.getAmount());
        String description = sanitizeDescription(firstNonBlank(ofxTransaction.getName(), ofxTransaction.getMemo(), "Transacao importada"));

        return new TransactionRequestDTO(
                type,
                description,
                suggestCategory(description),
                amount,
                Transaction.PaymentMethod.PIX,
                1,
                ofxTransaction.getDatePosted().toInstant().atZone(ZoneOffset.UTC).toLocalDate()
        );
    }

    private Transaction.TransactionType inferType(TransactionType ofxType, Double amount) {
        if (ofxType != null) {
            return switch (ofxType) {
                case CREDIT, DEP, DIV, IN -> Transaction.TransactionType.RECEITA;
                case DEBIT, FEE, SRVCHG, XFER, PAYMENT, CASH, DIRECTDEBIT, OUT -> Transaction.TransactionType.DESPESA;
                default -> amount != null && amount >= 0 ? Transaction.TransactionType.RECEITA : Transaction.TransactionType.DESPESA;
            };
        }

        return amount != null && amount >= 0 ? Transaction.TransactionType.RECEITA : Transaction.TransactionType.DESPESA;
    }

    private TransactionRequestDTO convertCSVLineToDTO(String[] line) {
        try {
            LocalDate date = parseDate(line[0]);
            String description = sanitizeDescription(line.length > 1 ? line[1] : "Transacao importada");
            BigDecimal parsedAmount = parseAmount(line[2]);
            BigDecimal amount = parsedAmount.abs();
            Transaction.TransactionType type = parseType(line, parsedAmount);
            Transaction.TransactionCategory category = parseCategory(line, description);
            Transaction.PaymentMethod paymentMethod = parsePaymentMethod(line);
            int installments = parseInstallments(line);

            return new TransactionRequestDTO(
                    type,
                    description,
                    category,
                    amount,
                    paymentMethod,
                    installments,
                    date
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseDate(String rawDate) {
        String value = rawDate == null ? "" : rawDate.replace("\uFEFF", "").trim();
        try {
            return LocalDate.parse(value, CSV_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value, BRAZILIAN_CSV_DATE_FORMAT);
        }
    }

    private BigDecimal parseAmount(String rawAmount) {
        String value = rawAmount == null ? "" : rawAmount
                .replace("R$", "")
                .replaceAll("\\s", "")
                .trim();

        if (value.contains(",")) {
            value = value.replace(".", "").replace(',', '.');
        }

        return new BigDecimal(value);
    }

    private Transaction.TransactionType parseType(String[] line, BigDecimal amount) {
        String rawType = line.length > 3 ? line[3].trim() : "";
        if (rawType.isBlank()) {
            return amount.signum() >= 0 ? Transaction.TransactionType.RECEITA : Transaction.TransactionType.DESPESA;
        }

        String normalized = normalizeText(rawType);
        if (containsAny(normalized, "receita", "entrada", "credito", "credit")) {
            return Transaction.TransactionType.RECEITA;
        }
        if (containsAny(normalized, "despesa", "saida", "debito", "debit")) {
            return Transaction.TransactionType.DESPESA;
        }

        try {
            return Transaction.TransactionType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return amount.signum() >= 0 ? Transaction.TransactionType.RECEITA : Transaction.TransactionType.DESPESA;
        }
    }

    private Transaction.TransactionCategory parseCategory(String[] line, String description) {
        String rawCategory = line.length > 4 ? line[4].trim() : "";
        if (!rawCategory.isBlank()) {
            try {
                return Transaction.TransactionCategory.valueOf(normalizeText(rawCategory).toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                // cai para a sugestao inteligente
            }
        }

        return suggestCategory(description);
    }

    private Transaction.PaymentMethod parsePaymentMethod(String[] line) {
        String rawPaymentMethod = line.length > 5 ? line[5].trim() : "";
        if (!rawPaymentMethod.isBlank()) {
            String normalized = normalizeText(rawPaymentMethod).replace(' ', '_');
            if (normalized.contains("parcel")) {
                return Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO;
            }
            if (containsAny(normalized, "cartao_debito", "debito")) {
                return Transaction.PaymentMethod.CARTAO_DEBITO;
            }
            if (containsAny(normalized, "cartao_credito", "credito", "avista", "a_vista")) {
                return Transaction.PaymentMethod.CARTAO_CREDITO_AVISTA;
            }
            try {
                return Transaction.PaymentMethod.valueOf(normalized.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // cai para PIX
            }
        }

        return Transaction.PaymentMethod.PIX;
    }

    private int parseInstallments(String[] line) {
        if (line.length > 6 && !line[6].trim().isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(line[6].trim()));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }

        return 1;
    }

    private Transaction.TransactionCategory suggestCategory(String description) {
        String normalized = normalizeText(description);

        if (containsAny(normalized, "mercado", "lanche", "restaurante", "ifood", "padaria", "pizza", "hamburguer", "burger", "food")) {
            return Transaction.TransactionCategory.ALIMENTACAO;
        }

        if (containsAny(normalized, "uber", "99", "onibus", "combustivel", "gasolina", "metro", "taxi", "transporte")) {
            return Transaction.TransactionCategory.TRANSPORTE;
        }

        if (containsAny(normalized, "aluguel", "condominio", "internet", "luz", "agua", "casa", "iptu")) {
            return Transaction.TransactionCategory.MORADIA;
        }

        if (containsAny(normalized, "medico", "consulta", "farmacia", "remedio", "hospital", "saude")) {
            return Transaction.TransactionCategory.SAUDE;
        }

        if (containsAny(normalized, "cinema", "jogo", "show", "viagem", "streaming", "lazer")) {
            return Transaction.TransactionCategory.LAZER;
        }

        if (containsAny(normalized, "curso", "faculdade", "livro", "caderno", "notebook", "estudo", "escola")) {
            return Transaction.TransactionCategory.EDUCACAO;
        }

        if (containsAny(normalized, "fone", "tenis", "camisa", "celular", "mouse", "headset", "teclado")) {
            return Transaction.TransactionCategory.COMPRAS;
        }

        return Transaction.TransactionCategory.OUTROS;
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
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeDescription(String value) {
        String sanitized = firstNonBlank(value, "Transacao importada")
                .replaceAll("(?i)\\b(?:cpf|ag[eê]ncia|conta)\\b\\s*[:#-]?\\s*[a-z0-9.*\\-/]+", "")
                .replaceAll("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.isBlank()) {
            return "Transacao importada";
        }

        return sanitized.substring(0, Math.min(sanitized.length(), 255));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
