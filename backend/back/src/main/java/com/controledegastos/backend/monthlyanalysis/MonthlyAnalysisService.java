package com.controledegastos.backend.monthlyanalysis; // Declares the package for the monthly analysis business logic.

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO; // Reuses the grouped category DTO from the dashboard module.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO; // Imports the main monthly analysis response DTO.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyComparisonDTO; // Imports the previous-month comparison DTO.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyHighestExpenseDTO; // Imports the biggest-expense DTO.
import com.controledegastos.backend.security.AuthenticatedUserService; // Imports the shared helper that resolves the authenticated user.
import com.controledegastos.backend.transactions.Transaction; // Imports the transaction entity and its enums.
import com.controledegastos.backend.transactions.TransactionCategorySummaryProjection; // Imports the projection used to map grouped category totals.
import com.controledegastos.backend.transactions.TransactionRepository; // Imports the repository that provides the monthly aggregates.
import com.controledegastos.backend.user.User; // Imports the authenticated user entity.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for dependencies.
import org.springframework.stereotype.Service; // Registers the class as a Spring service.
import org.springframework.transaction.annotation.Transactional; // Marks the public use case as read-only transactional.

import java.math.BigDecimal; // Imports the exact numeric type used for money.
import java.time.DateTimeException; // Imports the exception type used when month/year are invalid.
import java.time.LocalDate; // Imports the date type used to define the monthly interval.
import java.time.YearMonth; // Imports the type used to represent a year-month period.
import java.util.List; // Imports the list type used by grouped categories.

@Service // Registers this class as a Spring-managed service bean.
@RequiredArgsConstructor // Generates the constructor for the required dependencies.
public class MonthlyAnalysisService { // Declares the service responsible for the monthly analysis use case.

    private final TransactionRepository transactionRepository; // Stores access to transaction aggregates filtered by period.
    private final AuthenticatedUserService authenticatedUserService; // Stores access to the authenticated user helper.

    // Builds the monthly analysis payload for the authenticated user and informed period.
    @Transactional(readOnly = true) // Marks the use case as read-only to avoid accidental writes and keep it efficient.
    public MonthlyAnalysisResponseDTO getMonthlyAnalysis(int year, int month) { // Starts the main monthly analysis use case.
        YearMonth requestedPeriod = buildYearMonth(year, month); // Validates and creates the requested year-month object.
        YearMonth previousPeriod = requestedPeriod.minusMonths(1); // Calculates the immediately previous month, including year rollover.
        LocalDate requestedStartDate = requestedPeriod.atDay(1); // Calculates the first day of the requested month.
        LocalDate requestedEndDate = requestedPeriod.atEndOfMonth(); // Calculates the last day of the requested month.
        LocalDate previousStartDate = previousPeriod.atDay(1); // Calculates the first day of the previous month.
        LocalDate previousEndDate = previousPeriod.atEndOfMonth(); // Calculates the last day of the previous month.
        User user = authenticatedUserService.getAuthenticatedUser(); // Resolves which user owns the analysis being requested.
        BigDecimal totalReceitas = sumByType(user, Transaction.TransactionType.RECEITA, requestedStartDate, requestedEndDate); // Loads the income total of the selected month.
        BigDecimal totalDespesas = sumByType(user, Transaction.TransactionType.DESPESA, requestedStartDate, requestedEndDate); // Loads the expense total of the selected month.
        BigDecimal saldo = totalReceitas.subtract(totalDespesas); // Calculates the balance of the selected month.
        MonthlyHighestExpenseDTO maiorGasto = findHighestExpense(user, requestedStartDate, requestedEndDate); // Resolves the biggest expense of the selected month.
        List<DashboardCategorySummaryDTO> gastosPorCategoria = findGroupedExpenses(user, requestedStartDate, requestedEndDate); // Resolves the grouped expenses by category of the selected month.
        MonthlyComparisonDTO comparativoMesAnterior = buildComparison(user, previousStartDate, previousEndDate); // Builds the previous-month comparison snapshot.
        return new MonthlyAnalysisResponseDTO( // Creates the final response payload returned to the controller.
                requestedPeriod.getYear(), // Stores the selected year in the response.
                requestedPeriod.getMonthValue(), // Stores the selected month in the response.
                totalReceitas, // Stores the income total of the selected month.
                totalDespesas, // Stores the expense total of the selected month.
                saldo, // Stores the calculated balance of the selected month.
                maiorGasto, // Stores the biggest expense of the selected month.
                gastosPorCategoria, // Stores the grouped expenses by category.
                comparativoMesAnterior // Stores the previous-month comparison snapshot.
        ); // Finishes the response creation.
    } // Closes the main monthly analysis use case.

    // Validates the informed year and month and converts them into a YearMonth object.
    private YearMonth buildYearMonth(int year, int month) { // Starts the validation helper for year and month.
        try { // Starts the protected block that converts raw integers into a valid YearMonth.
            return YearMonth.of(year, month); // Creates the YearMonth when the informed values are valid.
        } catch (DateTimeException exception) { // Captures invalid month or year values.
            throw new IllegalArgumentException("Invalid year or month informed for monthly analysis", exception); // Throws a domain-friendly validation error.
        } // Closes the validation branch.
    } // Closes the YearMonth builder helper.

    // Sums the amount of one transaction type inside the informed interval.
    private BigDecimal sumByType( // Starts the helper that delegates the sum query by type and date interval.
            User user, // Receives the authenticated user used as the query owner filter.
            Transaction.TransactionType type, // Receives the transaction type that must be summed.
            LocalDate startDate, // Receives the first day of the analysis interval.
            LocalDate endDate // Receives the last day of the analysis interval.
    ) { // Closes the method signature.
        return transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween( // Executes the sum query in the repository.
                user, // Filters the query by the authenticated user.
                type, // Filters the query by the requested transaction type.
                startDate, // Filters the query by the interval start date.
                endDate // Filters the query by the interval end date.
        ); // Returns the summed amount as a BigDecimal.
    } // Closes the type-sum helper.

    // Loads the expense totals grouped by category inside the informed interval.
    private List<DashboardCategorySummaryDTO> findGroupedExpenses( // Starts the helper that maps grouped category totals.
            User user, // Receives the authenticated user used as the owner filter.
            LocalDate startDate, // Receives the first day of the analysis interval.
            LocalDate endDate // Receives the last day of the analysis interval.
    ) { // Closes the method signature.
        return transactionRepository.findExpenseSummaryByCategoryAndTransactionDateBetween(user, startDate, endDate) // Executes the grouping query for the selected month.
                .stream() // Opens a stream to convert repository projections into response DTOs.
                .map(this::toCategorySummaryDTO) // Maps each projection into the grouped category DTO.
                .toList(); // Materializes the mapped list.
    } // Closes the grouped-expense helper.

    // Converts the repository projection into the category DTO used by the response.
    private DashboardCategorySummaryDTO toCategorySummaryDTO(TransactionCategorySummaryProjection projection) { // Starts the category-projection mapper.
        return new DashboardCategorySummaryDTO( // Creates the grouped category DTO.
                projection.getCategory(), // Copies the grouped category returned by the query.
                projection.getTotalAmount() // Copies the summed expense amount of that category.
        ); // Finishes the DTO creation.
    } // Closes the category-projection mapper.

    // Resolves the biggest expense of the selected month.
    private MonthlyHighestExpenseDTO findHighestExpense( // Starts the helper that resolves the biggest expense of the period.
            User user, // Receives the authenticated user used as the owner filter.
            LocalDate startDate, // Receives the first day of the analysis interval.
            LocalDate endDate // Receives the last day of the analysis interval.
    ) { // Closes the method signature.
        return transactionRepository.findTopByUserAndTypeAndTransactionDateBetweenOrderByAmountDescTransactionDateDescCreatedAtDesc( // Executes the repository query that finds the biggest expense of the interval.
                        user, // Filters the query by the authenticated user.
                        Transaction.TransactionType.DESPESA, // Restricts the query to expense transactions.
                        startDate, // Restricts the query to the interval start date.
                        endDate // Restricts the query to the interval end date.
                ) // Finishes the repository lookup.
                .map(this::toHighestExpenseDTO) // Converts the entity into a response DTO when an expense exists.
                .orElse(null); // Returns null when the selected month has no expenses.
    } // Closes the biggest-expense helper.

    // Converts a transaction entity into the biggest-expense DTO.
    private MonthlyHighestExpenseDTO toHighestExpenseDTO(Transaction transaction) { // Starts the mapper for the biggest-expense response.
        return new MonthlyHighestExpenseDTO( // Creates the response DTO for the biggest expense.
                transaction.getDescription(), // Copies the expense description.
                transaction.getAmount(), // Copies the expense amount.
                transaction.getCategory(), // Copies the expense category.
                transaction.getTransactionDate() // Copies the expense business date.
        ); // Finishes the DTO creation.
    } // Closes the biggest-expense mapper.

    // Builds the previous-month comparison snapshot.
    private MonthlyComparisonDTO buildComparison( // Starts the helper that builds the previous-month comparison.
            User user, // Receives the authenticated user used as the owner filter.
            LocalDate startDate, // Receives the first day of the previous-month interval.
            LocalDate endDate // Receives the last day of the previous-month interval.
    ) { // Closes the method signature.
        BigDecimal previousReceitas = sumByType(user, Transaction.TransactionType.RECEITA, startDate, endDate); // Loads the income total of the previous month.
        BigDecimal previousDespesas = sumByType(user, Transaction.TransactionType.DESPESA, startDate, endDate); // Loads the expense total of the previous month.
        BigDecimal previousSaldo = previousReceitas.subtract(previousDespesas); // Calculates the balance of the previous month.
        return new MonthlyComparisonDTO( // Creates the comparison DTO returned inside the main response.
                previousReceitas, // Stores the previous-month income total.
                previousDespesas, // Stores the previous-month expense total.
                previousSaldo // Stores the previous-month balance.
        ); // Finishes the comparison DTO creation.
    } // Closes the comparison helper.
} // Closes the service class.
