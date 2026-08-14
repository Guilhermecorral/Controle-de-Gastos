package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.Transaction;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.webcohesion.ofx4j.io.AggregateUnmarshaller;
import com.webcohesion.ofx4j.io.OFXParser;
import com.webcohesion.ofx4j.io.OFXParserImpl;
import com.webcohesion.ofx4j.io.StreamParserException;
import com.webcohesion.ofx4j.meta.CurrencyUnit;
import com.webcohesion.ofx4j.meta.ONE;
import com.webcohesion.ofx4j.meta.StringMeta;
import com.webcohesion.ofx4j.net.OFXSignonResponse;
import com.webcohesion.ofx4j.domain.data.banking.BankTransaction;
import com.webcohesion.ofx4j.domain.data.banking.BankTransactionResponse;
import com.webcohesion.ofx4j.domain.data.banking.impl.BankTransactionResponseImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing OFX and CSV files and converting to TransactionRequestDTOs.
 */
@Service
public class OFXUploadService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Parses an OFX file and returns a list of TransactionRequestDTOs.
     *
     * @param file the OFX file to parse
     * @return list of TransactionRequestDTOs
     * @throws IOException if there is an error reading the file
     * @throws StreamParserException if there is an error parsing the OFX
     */
    public List<TransactionRequestDTO> parseOFX(MultipartFile file) throws IOException, StreamParserException {
        List<TransactionRequestDTO> transactions = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            OFXParser parser = new OFXParserImpl();
            OFXSignonResponse signonResponse = parser.parse(inputStream);
            BankTransactionResponse<?> bankTransactionResponse = signonResponse.getBankTransactionResponse();
            if (bankTransactionResponse instanceof BankTransactionResponseImpl) {
                BankTransactionResponseImpl response = (BankTransactionResponseImpl) bankTransactionResponse;
                for (BankTransaction bankTransaction : response.getBankTransactionList()) {
                    TransactionRequestDTO dto = convertBankTransactionToDTO(bankTransaction);
                    if (dto != null) {
                        transactions.add(dto);
                    }
                }
            }
        }
        return transactions;
    }

    /**
     * Parses a CSV file and returns a list of TransactionRequestDTOs.
     * Expected CSV format: date,description,amount,type,category,paymentMethod,installments
     * Date format: yyyy-MM-dd
     * Amount: decimal number (e.g., 100.50)
     * Type: RECEITA or DESPESA
     * Category: one of the enum values (ALIMENTACAO, TRANSPORTE, etc.)
     * PaymentMethod: one of the enum values (PIX, CARTAO_DEBITO, etc.)
     * Installments: integer (optional, defaults to 1)
     *
     * @param file the CSV file to parse
     * @return list of TransactionRequestDTOs
     * @throws IOException if there is an error reading the file
     * @throws CsvException if there is an error parsing the CSV
     */
    public List<TransactionRequestDTO> parseCSV(MultipartFile file) throws IOException, CsvException {
        List<TransactionRequestDTO> transactions = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             Reader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {
            String[] line;
            // Skip header if present? We assume no header for simplicity.
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 6) {
                    continue; // Skip invalid lines
                }
                TransactionRequestDTO dto = convertCSVLineToDTO(line);
                if (dto != null) {
                    transactions.add(dto);
                }
            }
        }
        return transactions;
    }

    private TransactionRequestDTO convertBankTransactionToDTO(BankTransaction bankTransaction) {
        try {
            // Map OFX fields to our DTO
            StringMeta fitid = bankTransaction.getFitid();
            StringMeta memo = bankTransaction.getMemo();
            CurrencyUnit transactionAmount = bankTransaction.getTransactionAmount();
            ONE<LocalDate> datePosted = bankTransaction.getDatePosted();
            // Note: OFX does not have all the fields we need, so we set defaults or leave null for some.
            // We'll set:
            //   type: DESPESA if amount negative, RECEITA if positive (but OFX uses signed amounts)
            //   category: OUTROS (default)
            //   paymentMethod: PIX (default) - OFX doesn't specify payment method
            //   installments: 1 (default)
            //   description: memo or fitid
            //   amount: absolute value of transactionAmount

            if (transactionAmount == null || datePosted == null) {
                return null;
            }

            BigDecimal amount = transactionAmount.getValue().abs();
            Transaction.TransactionType type = transactionAmount.getValue().signum() >= 0 ?
                    Transaction.TransactionType.RECEITA : Transaction.TransactionType.DESPESA;

            String description = memo != null ? memo.getValue() : (fitid != null ? fitid.getValue() : "OFX Transaction");

            return new TransactionRequestDTO(
                    type,
                    description,
                    Transaction.TransactionCategory.OUTROS, // default category
                    amount,
                    Transaction.PaymentMethod.PIX, // default payment method
                    1, // default installments
                    datePosted.getValue()
            );
        } catch (Exception e) {
            // Log the error and skip this transaction
            return null;
        }
    }

    private TransactionRequestDTO convertCSVLineToDTO(String[] line) {
        try {
            // Expected: date,description,amount,type,category,paymentMethod,installments
            LocalDate date = LocalDate.parse(line[0].trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            String description = line[1].trim();
            BigDecimal amount = new BigDecimal(line[2].trim());
            Transaction.TransactionType type = Transaction.TransactionType.valueOf(line[3].trim());
            Transaction.TransactionCategory category = Transaction.TransactionCategory.valueOf(line[4].trim());
            Transaction.PaymentMethod paymentMethod = Transaction.PaymentMethod.valueOf(line[5].trim());
            int installments = 1;
            if (line.length > 6 && !line[6].trim().isEmpty()) {
                installments = Integer.parseInt(line[6].trim());
            }

            return new TransactionRequestDTO(
                    type,
                    description,
                    category,
                    amount,
                    paymentMethod,
                    installments,
                    date
            );
        } catch (Exception e) {
            // Log the error and skip this line
            return null;
        }
    }
}