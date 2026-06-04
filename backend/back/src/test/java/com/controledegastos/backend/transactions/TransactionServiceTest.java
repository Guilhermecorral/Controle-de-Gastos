package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
