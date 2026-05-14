package com.controledegastos.backend.dashboard; // Declares the package for dashboard integration tests.

import com.controledegastos.backend.dashboard.dto.DashboardResponseDTO; // Imports the dashboard response DTO for assertions.
import com.controledegastos.backend.transactions.Transaction; // Imports the entity used to seed the test data.
import com.controledegastos.backend.transactions.Repository.TransactionRepository; // Imports the repository used to persist test transactions.
import com.controledegastos.backend.user.User; // Imports the user entity used in the authenticated context.
import com.controledegastos.backend.user.Repository.UserRepository; // Imports the repository used to persist the test users.
import org.junit.jupiter.api.AfterEach; // Imports the cleanup hook annotation.
import org.junit.jupiter.api.Test; // Imports the JUnit test annotation.
import org.springframework.beans.factory.annotation.Autowired; // Imports dependency injection for test beans.
import org.springframework.boot.test.context.SpringBootTest; // Loads the full Spring Boot application context.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Imports the authentication token used to seed the security context.
import org.springframework.security.core.context.SecurityContextHolder; // Imports access to the current security context.
import org.springframework.test.context.ActiveProfiles; // Activates the in-memory dev profile.

import java.math.BigDecimal; // Imports the exact type used for money assertions.
import java.time.YearMonth; // Imports the type used to build current and previous month dates dynamically.

import static org.junit.jupiter.api.Assertions.assertEquals; // Imports the equality assertion.

@SpringBootTest // Boots the application so the service, repository and security stack work together.
@ActiveProfiles("test") // Forces the test to use the isolated H2-backed test profile.
class DashboardServiceTest { // Declares the dashboard integration test class.

    @Autowired // Injects the dashboard service from the application context.
    private DashboardService dashboardService; // Stores the service under test.

    @Autowired // Injects the user repository for test data setup.
    private UserRepository userRepository; // Stores the user repository.

    @Autowired // Injects the transaction repository for test data setup and cleanup.
    private TransactionRepository transactionRepository; // Stores the transaction repository.

    @AfterEach // Runs after every test to avoid state leaking between executions.
    void tearDown() { // Starts the cleanup hook.
        SecurityContextHolder.clearContext(); // Clears the authentication created during the test.
        transactionRepository.deleteAll(); // Deletes transactions first to respect foreign key relationships.
        userRepository.deleteAll(); // Deletes users after the dependent transactions are gone.
    } // Closes the cleanup hook.

    @Test // Marks the method as a test case.
    void shouldBuildDashboardSummaryForSelectedReferenceMonth() { // Starts the dashboard use case test.
        YearMonth mesReferencia = YearMonth.of(2026, 4); // Defines a fixed dashboard month so the assertions are stable and explicit.
        YearMonth mesAnterior = mesReferencia.minusMonths(1); // Defines the previous month used to validate accumulated carry-over.
        YearMonth proximoMes = mesReferencia.plusMonths(1); // Defines a future month used to prove the dashboard ignores future data.
        User authenticatedUser = userRepository.save(User.builder() // Creates and persists the authenticated user.
                .name("Jorge") // Sets the display name used in the test data.
                .email("jorge@test.com") // Sets the unique email of the authenticated user.
                .password("123456") // Sets a simple password because hashing is not relevant in this test.
                .role(User.Role.USER) // Sets the role required by the entity.
                .build()); // Finishes and saves the user.
        User otherUser = userRepository.save(User.builder() // Creates and persists a second user to prove the dashboard is scoped.
                .name("Outro") // Sets the second user's display name.
                .email("outro@test.com") // Sets the second user's email.
                .password("654321") // Sets the second user's password.
                .role(User.Role.USER) // Sets the role required by the entity.
                .build()); // Finishes and saves the second user.

        transactionRepository.save(Transaction.builder() // Persists the first income transaction.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the transaction as income.
                .description("Salario") // Sets the description shown in the dashboard.
                .category(Transaction.TransactionCategory.OUTROS) // Uses a category even for income because the entity requires it.
                .amount(new BigDecimal("3000.00")) // Sets the amount received.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(mesReferencia.atDay(1)) // Sets the business date inside the selected reference month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists the first expense transaction.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as an expense.
                .description("Mercado") // Sets the description shown in the dashboard.
                .category(Transaction.TransactionCategory.ALIMENTACAO) // Sets the expense category.
                .amount(new BigDecimal("250.00")) // Sets the amount spent.
                .paymentMethod(Transaction.PaymentMethod.CARTAO_DEBITO) // Sets the payment method.
                .transactionDate(mesReferencia.atDay(5)) // Sets the business date inside the selected reference month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists the second expense transaction.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as an expense.
                .description("Uber") // Sets the description shown in the dashboard.
                .category(Transaction.TransactionCategory.TRANSPORTE) // Sets the expense category.
                .amount(new BigDecimal("80.00")) // Sets the amount spent.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(mesReferencia.atDay(4)) // Sets the business date inside the selected reference month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists another expense in the same category to validate grouping.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as an expense.
                .description("Padaria") // Sets another description for the same category.
                .category(Transaction.TransactionCategory.ALIMENTACAO) // Reuses the category to validate the sum by category.
                .amount(new BigDecimal("50.00")) // Sets the additional amount spent.
                .paymentMethod(Transaction.PaymentMethod.DINHEIRO) // Sets the payment method.
                .transactionDate(mesReferencia.atDay(3)) // Sets the business date inside the selected reference month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an older expense to validate accumulated carry-over.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as an expense.
                .description("Parcela Antiga") // Sets the description of the previous-month expense.
                .category(Transaction.TransactionCategory.COMPRAS) // Uses another category so the current-month grouping does not include it.
                .amount(new BigDecimal("100.00")) // Sets the previous-month expense amount.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(mesAnterior.atDay(10)) // Sets the business date inside the previous month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists a future income to prove the selected dashboard month ignores later months.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the future transaction as income.
                .description("Receita Futura") // Sets the description used to identify the future transaction in assertions.
                .category(Transaction.TransactionCategory.OUTROS) // Uses the required enum category.
                .amount(new BigDecimal("5000.00")) // Sets an amount that would break the totals if the future filter failed.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(proximoMes.atDay(4)) // Places the transaction in the month after the selected reference month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists a transaction from another user to ensure isolation.
                .user(otherUser) // Associates the transaction with the second user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as an expense.
                .description("Nao deve aparecer") // Sets a description that should never reach the authenticated dashboard.
                .category(Transaction.TransactionCategory.LAZER) // Sets a different category for isolation validation.
                .amount(new BigDecimal("999.00")) // Sets an amount that would distort the totals if filtering failed.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(mesReferencia.atDay(6)) // Sets a very recent date to catch ordering/filter bugs.
                .build()); // Finishes the transaction creation.

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken( // Seeds the security context like the JWT filter would do.
                authenticatedUser, // Stores the authenticated user as the principal.
                null, // Omits credentials because authentication already happened.
                authenticatedUser.getAuthorities() // Reuses the authorities provided by the user entity.
        )); // Finishes the authentication setup.

        DashboardResponseDTO response = dashboardService.getDashboard(mesReferencia.getYear(), mesReferencia.getMonthValue()); // Executes the dashboard use case for the selected month.

        assertEquals(new BigDecimal("3000.00"), response.totalReceitas()); // Confirms the backward-compatible total income now mirrors the accumulated income.
        assertEquals(new BigDecimal("480.00"), response.totalDespesas()); // Confirms the backward-compatible total expenses now mirror the accumulated expenses, including previous months.
        assertEquals(new BigDecimal("2520.00"), response.saldo()); // Confirms the backward-compatible balance now mirrors the accumulated balance.
        assertEquals(new BigDecimal("3000.00"), response.totalReceitasAcumuladas()); // Confirms the explicit accumulated income total is correct.
        assertEquals(new BigDecimal("480.00"), response.totalDespesasAcumuladas()); // Confirms the explicit accumulated expense total is correct.
        assertEquals(new BigDecimal("2520.00"), response.saldoAcumulado()); // Confirms the explicit accumulated balance is correct.
        assertEquals(new BigDecimal("3000.00"), response.receitasAnoReferencia()); // Confirms the year-to-date income excludes future months.
        assertEquals(new BigDecimal("480.00"), response.despesasAnoReferencia()); // Confirms the year-to-date expenses include only months up to the reference month.
        assertEquals(new BigDecimal("2520.00"), response.resultadoAnoReferencia()); // Confirms the year-to-date result is calculated correctly.
        assertEquals(new BigDecimal("3000.00"), response.receitasMesAtual()); // Confirms the current-month income total is correct.
        assertEquals(new BigDecimal("380.00"), response.despesasMesAtual()); // Confirms the current-month expense total ignores previous-month carry-over.
        assertEquals(new BigDecimal("2620.00"), response.resultadoMesAtual()); // Confirms the pure current-month result is correct.
        assertEquals(mesReferencia.getYear(), response.anoReferencia()); // Confirms the dashboard exposes the selected reference year.
        assertEquals(mesReferencia.getMonthValue(), response.mesReferencia()); // Confirms the dashboard exposes the selected reference month.
        assertEquals(4, response.ultimasTransacoes().size()); // Confirms only transactions inside the selected reference month are returned in the recent list.
        assertEquals("Mercado", response.ultimasTransacoes().get(0).description()); // Confirms the most recent transaction comes first.
        assertEquals(2, response.gastosPorCategoria().size()); // Confirms only categories with expenses are grouped.
        assertEquals(Transaction.TransactionCategory.ALIMENTACAO, response.gastosPorCategoria().get(0).category()); // Confirms the highest-spend category comes first.
        assertEquals(new BigDecimal("300.00"), response.gastosPorCategoria().get(0).totalAmount()); // Confirms current-month expense values are grouped and summed correctly.
    } // Closes the test method.
} // Closes the test class.
