package com.controledegastos.backend.transactions;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
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
