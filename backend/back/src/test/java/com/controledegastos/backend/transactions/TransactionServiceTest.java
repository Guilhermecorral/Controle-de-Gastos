package com.controledegastos.backend.transactions;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionImportResponseDTO;
import com.controledegastos.backend.transactions.DTO.TransactionReceiptResponseDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSplitParcelledTransactionAcrossMonths() {
        User user = authenticateDefaultUser();

        TransactionResponseDTO created = transactionService.create(new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Curso de arquitetura",
                Transaction.TransactionCategory.EDUCACAO,
                new BigDecimal("2400.00"),
                Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                24,
                LocalDate.of(2026, 6, 4)
        ));

        List<Transaction> savedTransactions = transactionRepository.findAllByUserOrderByTransactionDateDesc(user).stream()
                .sorted((left, right) -> left.getTransactionDate().compareTo(right.getTransactionDate()))
                .toList();

        assertEquals(24, savedTransactions.size());
        assertEquals("Curso de arquitetura - Parcela 1/24", savedTransactions.get(0).getDescription());
        assertEquals(new BigDecimal("100.00"), savedTransactions.get(0).getAmount());
        assertEquals(LocalDate.of(2026, 6, 4), savedTransactions.get(0).getTransactionDate());
        assertEquals(LocalDate.of(2026, 7, 4), savedTransactions.get(1).getTransactionDate());
        assertEquals(LocalDate.of(2028, 5, 4), savedTransactions.get(23).getTransactionDate());
        assertEquals(24, created.installments());
    }

    @Test
    void shouldRecreateEntireInstallmentGroupWhenEditingParcelledTransaction() {
        User user = authenticateDefaultUser();

        TransactionResponseDTO created = transactionService.create(new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Notebook",
                Transaction.TransactionCategory.EDUCACAO,
                new BigDecimal("1200.00"),
                Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                3,
                LocalDate.of(2026, 6, 10)
        ));

        transactionService.update(created.id(), new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Notebook atualizado",
                Transaction.TransactionCategory.EDUCACAO,
                new BigDecimal("1000.00"),
                Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                5,
                LocalDate.of(2026, 6, 10)
        ));

        List<Transaction> savedTransactions = transactionRepository.findAllByUserOrderByTransactionDateDesc(user).stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();

        assertEquals(5, savedTransactions.size());
        assertEquals("Notebook atualizado - Parcela 1/5", savedTransactions.get(0).getDescription());
        assertEquals(LocalDate.of(2026, 6, 10), savedTransactions.get(0).getTransactionDate());
        assertEquals(LocalDate.of(2026, 10, 10), savedTransactions.get(4).getTransactionDate());
        assertEquals(5, savedTransactions.get(0).getInstallments());
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingUnknownTransaction() {
        authenticateDefaultUser();

        assertThrows(ResourceNotFoundException.class, () -> transactionService.update(9999L, new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Nao existe",
                Transaction.TransactionCategory.OUTROS,
                new BigDecimal("10.00"),
                Transaction.PaymentMethod.PIX,
                1,
                LocalDate.of(2026, 6, 4)
        )));
    }

    @Test
    void shouldAttachAndListReceiptByTransactionPeriod() {
        authenticateDefaultUser();

        TransactionResponseDTO created = transactionService.create(new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Consulta medica",
                Transaction.TransactionCategory.SAUDE,
                new BigDecimal("350.00"),
                Transaction.PaymentMethod.PIX,
                1,
                LocalDate.of(2026, 2, 14)
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "nota-fiscal.pdf",
                "application/pdf",
                "%PDF-1.4 teste".getBytes()
        );

        TransactionResponseDTO updated = transactionService.attachReceipt(created.id(), file);
        List<TransactionReceiptResponseDTO> receipts = transactionService.listReceiptsByPeriod(
                2026,
                2
        );

        assertEquals("nota-fiscal.pdf", updated.receipt().originalFilename());
        assertEquals(1, receipts.size());
        assertEquals(created.id(), receipts.getFirst().transactionId());
        assertEquals("nota-fiscal.pdf", receipts.getFirst().originalFilename());
    }

    @Test
    void shouldImportReviewedTransactionsAsOneAtomicBatch() {
        User user = authenticateDefaultUser();

        TransactionImportResponseDTO response = transactionService.importTransactions(List.of(
                new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Uber",
                        Transaction.TransactionCategory.TRANSPORTE,
                        new BigDecimal("42.50"),
                        Transaction.PaymentMethod.PIX,
                        1,
                        LocalDate.of(2026, 8, 4)
                ),
                new TransactionRequestDTO(
                        Transaction.TransactionType.RECEITA,
                        "Salario",
                        Transaction.TransactionCategory.OUTROS,
                        new BigDecimal("2500.00"),
                        Transaction.PaymentMethod.PIX,
                        1,
                        LocalDate.of(2026, 8, 5)
                )
        ));

        assertEquals(2, response.importedTransactions());
        assertEquals(2, transactionRepository.findAllByUserOrderByTransactionDateDesc(user).size());
    }

    @Test
    void shouldKeepHistoricalInstallmentsWithoutCreatingFutureDuplicates() {
        User user = authenticateDefaultUser();

        transactionService.importTransactions(List.of(
                new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Notebook - Parcela 1/2",
                        Transaction.TransactionCategory.COMPRAS,
                        new BigDecimal("500.00"),
                        Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                        2,
                        LocalDate.of(2026, 7, 10)
                ),
                new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Notebook - Parcela 2/2",
                        Transaction.TransactionCategory.COMPRAS,
                        new BigDecimal("500.00"),
                        Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                        2,
                        LocalDate.of(2026, 8, 10)
                )
        ));

        List<Transaction> saved = transactionRepository.findAllByUserOrderByTransactionDateDesc(user);
        assertEquals(2, saved.size());
        assertEquals(new BigDecimal("500.00"), saved.getFirst().getAmount());
        assertEquals(2, saved.getFirst().getInstallments());
    }

    @Test
    void shouldAcceptMoreThanOneThousandHistoricalRows() {
        User user = authenticateDefaultUser();
        List<TransactionRequestDTO> transactions = IntStream.range(0, 1001)
                .mapToObj(index -> new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Historico " + index,
                        Transaction.TransactionCategory.OUTROS,
                        BigDecimal.ONE,
                        Transaction.PaymentMethod.PIX,
                        1,
                        LocalDate.of(2026, 1, 1).plusDays(index)
                ))
                .toList();

        TransactionImportResponseDTO response = transactionService.importTransactions(transactions);

        assertEquals(1001, response.importedTransactions());
        assertEquals(1001, transactionRepository.countByUser(user));
    }

    @Test
    void shouldRollbackTheWholeBatchWhenOneReviewedTransactionIsInvalid() {
        User user = authenticateDefaultUser();

        assertThrows(IllegalArgumentException.class, () -> transactionService.importTransactions(List.of(
                new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Compra valida",
                        Transaction.TransactionCategory.COMPRAS,
                        new BigDecimal("100.00"),
                        Transaction.PaymentMethod.PIX,
                        1,
                        LocalDate.of(2026, 8, 4)
                ),
                new TransactionRequestDTO(
                        Transaction.TransactionType.DESPESA,
                        "Parcelamento invalido",
                        Transaction.TransactionCategory.COMPRAS,
                        new BigDecimal("500.00"),
                        Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO,
                        1,
                        LocalDate.of(2026, 8, 5)
                )
        )));

        assertEquals(0, transactionRepository.findAllByUserOrderByTransactionDateDesc(user).size());
    }

    private User authenticateDefaultUser() {
        User authenticatedUser = userRepository.save(User.builder()
                .name("Jorge")
                .email("jorge-transaction@test.com")
                .password("123456")
                .role(User.Role.USER)
                .build());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities()
        ));

        return authenticatedUser;
    }
}
