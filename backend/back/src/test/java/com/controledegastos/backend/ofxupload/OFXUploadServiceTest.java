package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OFXUploadServiceTest {

    private final OFXUploadService service = new OFXUploadService();

    @Test
    void shouldParseBrazilianCsvAndSuggestCategoriesWithoutSensitiveData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.csv",
                "text/csv",
                ("Data;Descricao;Valor;Tipo;Categoria;Pagamento;Parcelas\n"
                        + "04/08/2026;UBER TRIP CPF: 123.456.789-09;-45,90;;;;\n"
                        + "04/08/2026;IFOOD RESTAURANTE;-32,50;;;;\n"
                        + "05/08/2026;Salario;R$ 2.500,00;Entrada;;;\n")
                        .getBytes(StandardCharsets.UTF_8)
        );

        List<TransactionRequestDTO> transactions = service.parseCSV(file);

        assertEquals(3, transactions.size());
        assertEquals(LocalDate.of(2026, 8, 4), transactions.getFirst().transactionDate());
        assertEquals(Transaction.TransactionType.DESPESA, transactions.getFirst().type());
        assertEquals(Transaction.TransactionCategory.TRANSPORTE, transactions.getFirst().category());
        assertEquals("45.90", transactions.getFirst().amount().toPlainString());
        assertFalse(transactions.getFirst().description().toLowerCase().contains("cpf"));
        assertFalse(transactions.getFirst().description().contains("123.456.789-09"));
        assertEquals(Transaction.TransactionCategory.ALIMENTACAO, transactions.get(1).category());
        assertEquals(Transaction.TransactionType.RECEITA, transactions.get(2).type());
        assertEquals("2500.00", transactions.get(2).amount().toPlainString());
    }
}
