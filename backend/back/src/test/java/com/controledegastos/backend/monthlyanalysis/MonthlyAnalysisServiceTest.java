package com.controledegastos.backend.monthlyanalysis; // Declares the package for the monthly analysis integration tests.

import com.controledegastos.backend.monthlyanalysis.dto.AnalysisTrend; // Imports the trend enum used in the new comparison assertions.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO; // Imports the response DTO asserted by the tests.
import com.controledegastos.backend.transactions.Transaction; // Imports the entity used to seed test data.
import com.controledegastos.backend.transactions.Repository.TransactionRepository; // Imports the repository used to persist test transactions.
import com.controledegastos.backend.user.User; // Imports the user entity used in the authenticated test context.
import com.controledegastos.backend.user.Repository.UserRepository; // Imports the repository used to persist the test users.
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
        transactionRepository.save(Transaction.builder() // Persists an income in the same month of the previous year for historical comparison.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the transaction as income.
                .description("Salario Abril Ano Passado") // Sets the description used in the historical comparison.
                .category(Transaction.TransactionCategory.OUTROS) // Uses the required category field of the entity.
                .amount(new BigDecimal("3800.00")) // Sets the income amount of April in the previous year.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2025, 4, 5)) // Places the transaction in the same month of the previous year.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an expense in the same month of the previous year for historical comparison.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Aluguel Ano Passado") // Sets the description used in the historical comparison.
                .category(Transaction.TransactionCategory.MORADIA) // Uses the same category to keep the comparison realistic.
                .amount(new BigDecimal("1600.00")) // Sets the expense amount of April in the previous year.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2025, 4, 10)) // Places the transaction in the same month of the previous year.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an earlier income of the previous year to validate the previous year year-to-date accumulation.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.RECEITA) // Marks the transaction as income.
                .description("Salario Janeiro Ano Passado") // Sets the description used in the year-to-date comparison.
                .category(Transaction.TransactionCategory.OUTROS) // Uses the required category field of the entity.
                .amount(new BigDecimal("2000.00")) // Sets the earlier income amount.
                .paymentMethod(Transaction.PaymentMethod.PIX) // Sets the payment method.
                .transactionDate(LocalDate.of(2025, 1, 7)) // Places the transaction in January of the previous year.
                .build()); // Finishes the transaction creation.
        transactionRepository.save(Transaction.builder() // Persists an earlier expense of the previous year to validate the previous year year-to-date accumulation.
                .user(authenticatedUser) // Associates the transaction with the authenticated user.
                .type(Transaction.TransactionType.DESPESA) // Marks the transaction as expense.
                .description("Conta Janeiro Ano Passado") // Sets the description used in the year-to-date comparison.
                .category(Transaction.TransactionCategory.COMPRAS) // Uses another category to diversify the previous-year data.
                .amount(new BigDecimal("1000.00")) // Sets the earlier expense amount.
                .paymentMethod(Transaction.PaymentMethod.DINHEIRO) // Sets the payment method.
                .transactionDate(LocalDate.of(2025, 1, 20)) // Places the transaction in January of the previous year.
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
        assertEquals(2026, response.comparativoMesAnterior().year()); // Confirms the previous-month comparison exposes the compared year.
        assertEquals(3, response.comparativoMesAnterior().month()); // Confirms the previous-month comparison exposes the compared month.
        assertEquals(new BigDecimal("3500.00"), response.comparativoMesAnterior().totalReceitas()); // Confirms the previous-month income total is correct.
        assertEquals(new BigDecimal("500.00"), response.comparativoMesAnterior().totalDespesas()); // Confirms the previous-month expense total is correct.
        assertEquals(new BigDecimal("3000.00"), response.comparativoMesAnterior().saldo()); // Confirms the previous-month balance is correct.
        assertEquals(new BigDecimal("500.00"), response.comparativoMesAnterior().diferencaReceitas()); // Confirms the income difference against the previous month is correct.
        assertEquals(new BigDecimal("1000.00"), response.comparativoMesAnterior().diferencaDespesas()); // Confirms the expense difference against the previous month is correct.
        assertEquals(new BigDecimal("-500.00"), response.comparativoMesAnterior().diferencaSaldo()); // Confirms the balance difference against the previous month is correct.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoMesAnterior().tendenciaReceitas()); // Confirms higher revenue is interpreted as better.
        assertEquals(AnalysisTrend.PIOR, response.comparativoMesAnterior().tendenciaDespesas()); // Confirms higher expenses are interpreted as worse.
        assertEquals(AnalysisTrend.PIOR, response.comparativoMesAnterior().tendenciaSaldo()); // Confirms a lower balance is interpreted as worse.
        assertEquals(AnalysisTrend.PIOR, response.comparativoMesAnterior().tendenciaGeral()); // Confirms the overall previous-month comparison follows the balance trend.
        assertEquals(2025, response.comparativoMesmoMesAnoAnterior().year()); // Confirms the historical comparison exposes the previous year.
        assertEquals(4, response.comparativoMesmoMesAnoAnterior().month()); // Confirms the historical comparison keeps the same month number.
        assertEquals(new BigDecimal("3800.00"), response.comparativoMesmoMesAnoAnterior().totalReceitas()); // Confirms the same-month-last-year income total is correct.
        assertEquals(new BigDecimal("1600.00"), response.comparativoMesmoMesAnoAnterior().totalDespesas()); // Confirms the same-month-last-year expense total is correct.
        assertEquals(new BigDecimal("2200.00"), response.comparativoMesmoMesAnoAnterior().saldo()); // Confirms the same-month-last-year balance is correct.
        assertEquals(new BigDecimal("200.00"), response.comparativoMesmoMesAnoAnterior().diferencaReceitas()); // Confirms the income difference against last year's same month is correct.
        assertEquals(new BigDecimal("-100.00"), response.comparativoMesmoMesAnoAnterior().diferencaDespesas()); // Confirms the expense difference against last year's same month is correct.
        assertEquals(new BigDecimal("300.00"), response.comparativoMesmoMesAnoAnterior().diferencaSaldo()); // Confirms the balance difference against last year's same month is correct.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoMesmoMesAnoAnterior().tendenciaReceitas()); // Confirms higher revenue than last year is interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoMesmoMesAnoAnterior().tendenciaDespesas()); // Confirms lower expenses than last year are interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoMesmoMesAnoAnterior().tendenciaSaldo()); // Confirms a stronger balance than last year is interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoMesmoMesAnoAnterior().tendenciaGeral()); // Confirms a stronger balance than last year is interpreted as better overall.
        assertEquals(2026, response.acumuladoAnoAtual().year()); // Confirms the selected year-to-date summary exposes the selected year.
        assertEquals(4, response.acumuladoAnoAtual().monthLimit()); // Confirms the selected year-to-date summary uses the selected month as its limit.
        assertEquals(new BigDecimal("7500.00"), response.acumuladoAnoAtual().totalReceitas()); // Confirms the selected year-to-date income total sums March and April.
        assertEquals(new BigDecimal("2000.00"), response.acumuladoAnoAtual().totalDespesas()); // Confirms the selected year-to-date expense total sums March and April.
        assertEquals(new BigDecimal("5500.00"), response.acumuladoAnoAtual().saldo()); // Confirms the selected year-to-date balance is correct.
        assertEquals(2026, response.comparativoAcumuladoAnoAnterior().anoAtual().year()); // Confirms the selected year snapshot is exposed inside the year-to-date comparison.
        assertEquals(2025, response.comparativoAcumuladoAnoAnterior().anoAnterior().year()); // Confirms the previous-year snapshot is exposed inside the year-to-date comparison.
        assertEquals(new BigDecimal("5800.00"), response.comparativoAcumuladoAnoAnterior().anoAnterior().totalReceitas()); // Confirms the previous-year year-to-date income total is correct.
        assertEquals(new BigDecimal("2600.00"), response.comparativoAcumuladoAnoAnterior().anoAnterior().totalDespesas()); // Confirms the previous-year year-to-date expense total is correct.
        assertEquals(new BigDecimal("3200.00"), response.comparativoAcumuladoAnoAnterior().anoAnterior().saldo()); // Confirms the previous-year year-to-date balance is correct.
        assertEquals(new BigDecimal("1700.00"), response.comparativoAcumuladoAnoAnterior().diferencaReceitas()); // Confirms the year-to-date income difference is correct.
        assertEquals(new BigDecimal("-600.00"), response.comparativoAcumuladoAnoAnterior().diferencaDespesas()); // Confirms the year-to-date expense difference is correct.
        assertEquals(new BigDecimal("2300.00"), response.comparativoAcumuladoAnoAnterior().diferencaSaldo()); // Confirms the year-to-date balance difference is correct.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoAcumuladoAnoAnterior().tendenciaReceitas()); // Confirms higher year-to-date revenue is interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoAcumuladoAnoAnterior().tendenciaDespesas()); // Confirms lower year-to-date expenses are interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoAcumuladoAnoAnterior().tendenciaSaldo()); // Confirms a stronger year-to-date balance is interpreted as better.
        assertEquals(AnalysisTrend.MELHOR, response.comparativoAcumuladoAnoAnterior().tendenciaGeral()); // Confirms a stronger year-to-date balance is interpreted as better overall.
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
        assertEquals(AnalysisTrend.IGUAL, response.comparativoMesAnterior().tendenciaGeral()); // Confirms an empty comparison is interpreted as equal.
        assertEquals(new BigDecimal("0"), response.comparativoMesmoMesAnoAnterior().totalReceitas()); // Confirms the same-month-last-year income total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoMesmoMesAnoAnterior().totalDespesas()); // Confirms the same-month-last-year expense total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoMesmoMesAnoAnterior().saldo()); // Confirms the same-month-last-year balance is zero when there is no data.
        assertEquals(AnalysisTrend.IGUAL, response.comparativoMesmoMesAnoAnterior().tendenciaGeral()); // Confirms an empty same-month-last-year comparison is interpreted as equal.
        assertEquals(new BigDecimal("0"), response.acumuladoAnoAtual().totalReceitas()); // Confirms the current year-to-date income total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.acumuladoAnoAtual().totalDespesas()); // Confirms the current year-to-date expense total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.acumuladoAnoAtual().saldo()); // Confirms the current year-to-date balance is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoAcumuladoAnoAnterior().anoAnterior().totalReceitas()); // Confirms the previous-year year-to-date income total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoAcumuladoAnoAnterior().anoAnterior().totalDespesas()); // Confirms the previous-year year-to-date expense total is zero when there is no data.
        assertEquals(new BigDecimal("0"), response.comparativoAcumuladoAnoAnterior().anoAnterior().saldo()); // Confirms the previous-year year-to-date balance is zero when there is no data.
        assertEquals(AnalysisTrend.IGUAL, response.comparativoAcumuladoAnoAnterior().tendenciaGeral()); // Confirms an empty year-to-date comparison is interpreted as equal.
    } // Closes the empty-month scenario.
} // Closes the test class.
