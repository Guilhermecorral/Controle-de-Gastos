package com.controledegastos.backend.monthlyanalysis; // Declares the package for the monthly analysis integration tests.

import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO; // Imports the response DTO asserted by the tests.
import com.controledegastos.backend.transactions.Transaction; // Imports the entity used to seed test data.
import com.controledegastos.backend.transactions.TransactionRepository; // Imports the repository used to persist test transactions.
import com.controledegastos.backend.user.User; // Imports the user entity used in the authenticated test context.
import com.controledegastos.backend.user.UserRepository; // Imports the repository used to persist the test users.
import org.junit.jupiter.api.AfterEach; // Imports the cleanup hook annotation.
import org.junit.jupiter.api.Test; // Imports the JUnit test annotation.
import org.springframework.beans.factory.annotation.Autowired; // Imports dependency injection for test beans.
import org.springframework.boot.test.context.SpringBootTest; // Loads the full Spring Boot application context.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Imports the authentication token used to seed the security context.
import org.springframework.security.core.context.SecurityContextHolder; // Imports access to the current security context.
import org.springframework.test.context.ActiveProfiles; // Activates the isolated test profile.

import java.math.BigDecimal; // Imports the exact numeric type used for money assertions.
import java.time.LocalDate; // Imports the date type used by the seeded transactions.

import static org.junit.jupiter.api.Assertions.assertEquals; // Imports the equality assertion.
import static org.junit.jupiter.api.Assertions.assertNotNull; // Imports the assertion used when a value must exist.
import static org.junit.jupiter.api.Assertions.assertNull; // Imports the assertion used when a value must be absent.

@SpringBootTest // Boots the full application so the monthly analysis runs with real wiring.
@ActiveProfiles("test") // Forces the test to use the isolated test profile.
class MonthlyAnalysisServiceTest { // Declares the integration test for the monthly analysis use case.

    @Autowired // Injects the monthly analysis service from the application context.
    private MonthlyAnalysisService monthlyAnalysisService; // Stores the service under test.

    @Autowired // Injects the user repository used to create the authenticated user.
    private UserRepository userRepository; // Stores the user repository used by the test setup.

    @Autowired // Injects the transaction repository used to seed transactions.
    private TransactionRepository transactionRepository; // Stores the transaction repository used by the test setup.

    @AfterEach // Runs after each test so state does not leak between executions.
    void tearDown() { // Starts the cleanup hook.
        SecurityContextHolder.clearContext(); // Clears the authentication created during the test.
        transactionRepository.deleteAll(); // Deletes all transactions first to respect foreign key relationships.
        userRepository.deleteAll(); // Deletes all users after the dependent transactions are gone.
    } // Closes the cleanup hook.

    @Test // Marks the method as a test case.
    void shouldBuildMonthlyAnalysisWithPreviousMonthComparison() { // Starts the main monthly analysis scenario.
        User authenticatedUser = userRepository.save(User.builder() // Creates and persists the authenticated user.
                .name("Jorge") // Sets the display name used in the test data.
                .email("jorge-monthly@test.com") // Sets the unique email of the authenticated user.
                .password("123456") // Sets a simple password because hashing is not relevant to this integration test.
                .role(User.Role.USER) // Sets the required role of the user entity.
                .build()); // Finishes and saves the user.
        User otherUser = userRepository.save(User.builder() // Creates and persists another user to validate isolation.
                .name("Outro") // Sets the display name of the second user.
                .email("outro-monthly@test.com") // Sets the unique email of the second user.
                .password("654321") // Sets the password of the second user.
                .role(User.Role.USER) // Sets the required role of the second user.
                .build()); // Finishes and saves the second user.

        transactionRepository.save(Transaction.builder() // Persists an income of April for the authenticated user.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the transaction as income.
                .description("Salario Abril") // Sets the description visible in the analysis.
                .category(Transaction.TransactionCategory.OUTROS) // Uses the required category field of the entity.
                .amount(new BigDecimal("4000.00")) // Sets the income amount.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 4, 5)) // Sets the transaction inside the selected month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists the highest expense of April for the authenticated user.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Aluguel") // Sets the description of the highest expense.
                .category(Transaction.TransactionCategory.MORADIA) // Sets the expense category.
                .amount(new BigDecimal("1200.00")) // Sets the highest expense amount.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 4, 10)) // Sets the transaction inside the selected month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists another expense of April for category grouping.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Mercado") // Sets another description for April.
                .category(Transaction.TransactionCategory.ALIMENTACAO) // Sets another category for grouping validation.
                .amount(new BigDecimal("300.00")) // Sets the expense amount.
                .paymentMethod(Transaction.PaymentMethod.CARTAO_DEBITO) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 4, 12)) // Sets the transaction inside the selected month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an income in the previous month for comparison.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the transaction as income.
                .description("Salario Marco") // Sets the description of the previous month income.
                .category(Transaction.TransactionCategory.OUTROS) // Uses the required category field of the entity.
                .amount(new BigDecimal("3500.00")) // Sets the previous month income amount.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 3, 5)) // Sets the transaction inside the previous month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an expense in the previous month for comparison.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Mercado Marco") // Sets the description of the previous month expense.
                .category(Transaction.TransactionCategory.ALIMENTACAO) // Sets the category of the previous month expense.
                .amount(new BigDecimal("500.00")) // Sets the previous month expense amount.
                .paymentMethod(Transaction.PaymentMethod.DINHEIRO) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 3, 18)) // Sets the transaction inside the previous month.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists a very large expense for another user to prove isolation.
                .user(otherUser) // Associates the transaction with the second user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Nao deve aparecer") // Sets a description that should never be visible in the authenticated analysis.
                .category(Transaction.TransactionCategory.LAZER) // Sets a category that should not contaminate the grouped result.
                .amount(new BigDecimal("9999.00")) // Sets a large amount to expose any missing user filter.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2026, 4, 15)) // Places the transaction inside the selected month to make isolation meaningful.
                .build()); // Finishes the transaction creation.

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken( // Seeds the security context like the JWT filter would do in production.
                authenticatedUser, // Stores the authenticated user as the principal.
                null, // Omits credentials because authentication already happened.
                authenticatedUser.getAuthorities() // Reuses the authorities exposed by the user entity.
        )); // Finishes the authentication setup.

        MonthlyAnalysisResponseDTO response = monthlyAnalysisService.getMonthlyAnalysis(2026, 4); // Executes the monthly analysis use case for April 2026.

        assertEquals(2026, response.year()); // Confirms the response keeps the selected year.
        assertEquals(4, response.month()); // Confirms the response keeps the selected month.
        assertEquals(new BigDecimal("4000.00"), response.totalReceitas()); // Confirms the selected-month income total is correct.
        assertEquals(new BigDecimal("1500.00"), response.totalDespesas()); // Confirms the selected-month expense total is correct.
        assertEquals(new BigDecimal("2500.00"), response.saldo()); // Confirms the selected-month balance is correct.
        assertNotNull(response.maiorGasto()); // Confirms the biggest expense exists when the month has expenses.
        assertEquals("Aluguel", response.maiorGasto().description()); // Confirms the biggest expense description is correct.
        assertEquals(new BigDecimal("1200.00"), response.maiorGasto().amount()); // Confirms the biggest expense amount is correct.
        assertEquals(2, response.gastosPorCategoria().size()); // Confirms only the authenticated user's expense categories are returned.
        assertEquals(Transaction.TransactionCategory.MORADIA, response.gastosPorCategoria().get(0).category()); // Confirms the highest-spend category comes first.
        assertEquals(new BigDecimal("1200.00"), response.gastosPorCategoria().get(0).totalAmount()); // Confirms the category total is correct.
        assertEquals(new BigDecimal("3500.00"), response.comparativoMesAnterior().totalReceitas()); // Confirms the previous-month income total is correct.
        assertEquals(new BigDecimal("500.00"), response.comparativoMesAnterior().totalDespesas()); // Confirms the previous-month expense total is correct.
        assertEquals(new BigDecimal("3000.00"), response.comparativoMesAnterior().saldo()); // Confirms the previous-month balance is correct.
    } // Closes the main monthly analysis scenario.

    @Test // Marks the method as another test case.
    void shouldReturnZerosAndNullHighestExpenseWhenMonthHasNoTransactions() { // Starts the empty-month scenario.
        User authenticatedUser = userRepository.save(User.builder() // Creates and persists the authenticated user.
                .name("Jorge") // Sets the display name used in the test data.
                .email("jorge-empty@test.com") // Sets the unique email of the authenticated user.
                .password("123456") // Sets the password used only for entity completeness in this test.
                .role(User.Role.USER) // Sets the required role of the user entity.
                .build()); // Finishes and saves the user.

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken( // Seeds the security context like the JWT filter would do in production.
                authenticatedUser, // Stores the authenticated user as the principal.
                null, // Omits credentials because authentication already happened.
                authenticatedUser.getAuthorities() // Reuses the authorities exposed by the user entity.
        )); // Finishes the authentication setup.

        MonthlyAnalysisResponseDTO response = monthlyAnalysisService.getMonthlyAnalysis(2026, 4); // Executes the monthly analysis for a month without transactions.

        assertEquals(new BigDecimal("0"), response.totalReceitas()); // Confirms the empty-month income total is zero.
        assertEquals(new BigDecimal("0"), response.totalDespesas()); // Confirms the empty-month expense total is zero.
        assertEquals(new BigDecimal("0"), response.saldo()); // Confirms the empty-month balance is zero.
        assertNull(response.maiorGasto()); // Confirms there is no biggest expense when the month has no expenses.
        assertEquals(0, response.gastosPorCategoria().size()); // Confirms the grouped category list is empty when there are no expenses.
        assertEquals(new BigDecimal("0"), response.comparativoMesAnterior().totalReceitas()); // Confirms the previous-month income total is also zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoMesAnterior().totalDespesas()); // Confirms the previous-month expense total is also zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoMesAnterior().saldo()); // Confirms the previous-month balance is also zero when there is no data.
    } // Closes the empty-month scenario.
} // Closes the test class.
