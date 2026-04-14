package com.controledegastos.backend.monthlyanalysis; // Declares the package for the monthly analysis business logic.

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO; // Reuses the grouped category DTO from the dashboard module.
import com.controledegastos.backend.monthlyanalysis.dto.AnalysisTrend; // Imports the trend enum used to interpret comparisons.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO; // Imports the main monthly analysis response DTO.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyComparisonDTO; // Imports the previous-month comparison DTO.
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyHighestExpenseDTO; // Imports the biggest-expense DTO.
import com.controledegastos.backend.monthlyanalysis.dto.YearToDateComparisonDTO; // Imports the DTO used to compare year-to-date snapshots.
import com.controledegastos.backend.monthlyanalysis.dto.YearToDateSummaryDTO; // Imports the DTO used to summarize year-to-date totals.
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
        YearMonth sameMonthLastYearPeriod = requestedPeriod.minusYears(1); // Calculates the same month of the previous year for historical comparison.
        LocalDate requestedStartDate = requestedPeriod.atDay(1); // Calculates the first day of the requested month.
        LocalDate requestedEndDate = requestedPeriod.atEndOfMonth(); // Calculates the last day of the requested month.
        LocalDate previousStartDate = previousPeriod.atDay(1); // Calculates the first day of the previous month.
        LocalDate previousEndDate = previousPeriod.atEndOfMonth(); // Calculates the last day of the previous month.
        LocalDate sameMonthLastYearStartDate = sameMonthLastYearPeriod.atDay(1); // Calculates the first day of the same month in the previous year.
        LocalDate sameMonthLastYearEndDate = sameMonthLastYearPeriod.atEndOfMonth(); // Calculates the last day of the same month in the previous year.
        LocalDate currentYearStartDate = requestedPeriod.atDay(1).withDayOfYear(1); // Calculates the first day of the selected year for year-to-date accumulation.
        LocalDate previousYearStartDate = sameMonthLastYearPeriod.atDay(1).withDayOfYear(1); // Calculates the first day of the previous year for the year-to-date comparison.
        User user = authenticatedUserService.getAuthenticatedUser(); // Resolves which user owns the analysis being requested.
        BigDecimal totalReceitas = sumByType(user, Transaction.TransactionType.RECEITA, requestedStartDate, requestedEndDate); // Loads the income total of the selected month.
        BigDecimal totalDespesas = sumByType(user, Transaction.TransactionType.DESPESA, requestedStartDate, requestedEndDate); // Loads the expense total of the selected month.
        BigDecimal saldo = totalReceitas.subtract(totalDespesas); // Calculates the balance of the selected month.
        MonthlyHighestExpenseDTO maiorGasto = findHighestExpense(user, requestedStartDate, requestedEndDate); // Resolves the biggest expense of the selected month.
        List<DashboardCategorySummaryDTO> gastosPorCategoria = findGroupedExpenses(user, requestedStartDate, requestedEndDate); // Resolves the grouped expenses by category of the selected month.
        MonthlyComparisonDTO comparativoMesAnterior = buildMonthlyComparison( // Builds the previous-month comparison snapshot.
                user, // Passes the authenticated user used to load the compared period.
                previousPeriod, // Passes the period that should be compared against the selected month.
                previousStartDate, // Passes the start date of the compared period.
                previousEndDate, // Passes the end date of the compared period.
                totalReceitas, // Passes the selected-month income total so the comparison can calculate differences.
                totalDespesas, // Passes the selected-month expense total so the comparison can calculate differences.
                saldo // Passes the selected-month balance so the comparison can calculate differences.
        ); // Finishes the previous-month comparison.
        MonthlyComparisonDTO comparativoMesmoMesAnoAnterior = buildMonthlyComparison( // Builds the same-month-last-year comparison snapshot.
                user, // Passes the authenticated user used to load the compared period.
                sameMonthLastYearPeriod, // Passes the historical period that should be compared.
                sameMonthLastYearStartDate, // Passes the start date of the historical period.
                sameMonthLastYearEndDate, // Passes the end date of the historical period.
                totalReceitas, // Passes the selected-month income total so the comparison can calculate differences.
                totalDespesas, // Passes the selected-month expense total so the comparison can calculate differences.
                saldo // Passes the selected-month balance so the comparison can calculate differences.
        ); // Finishes the same-month-last-year comparison.
        YearToDateSummaryDTO acumuladoAnoAtual = buildYearToDateSummary( // Builds the current year-to-date snapshot of the selected year.
                user, // Passes the authenticated user used to load the accumulation.
                requestedPeriod.getYear(), // Passes the selected analysis year.
                requestedPeriod.getMonthValue(), // Passes the selected analysis month as the accumulation limit.
                currentYearStartDate, // Passes the first day of the selected year.
                requestedEndDate // Passes the last day of the selected month.
        ); // Finishes the current year-to-date snapshot.
        YearToDateComparisonDTO comparativoAcumuladoAnoAnterior = buildYearToDateComparison( // Builds the year-to-date comparison against the previous year.
                user, // Passes the authenticated user used to load the two accumulated snapshots.
                acumuladoAnoAtual, // Passes the already built current year-to-date snapshot.
                sameMonthLastYearPeriod.getYear(), // Passes the previous year used in the comparison.
                requestedPeriod.getMonthValue(), // Passes the selected month as the shared accumulation limit.
                previousYearStartDate, // Passes the first day of the previous year.
                sameMonthLastYearEndDate // Passes the last day of the same month in the previous year.
        ); // Finishes the year-to-date comparison.
        return new MonthlyAnalysisResponseDTO( // Creates the final response payload returned to the controller.
                requestedPeriod.getYear(), // Stores the selected year in the response.
                requestedPeriod.getMonthValue(), // Stores the selected month in the response.
                totalReceitas, // Stores the income total of the selected month.
                totalDespesas, // Stores the expense total of the selected month.
                saldo, // Stores the calculated balance of the selected month.
                maiorGasto, // Stores the biggest expense of the selected month.
                gastosPorCategoria, // Stores the grouped expenses by category.
                comparativoMesAnterior, // Stores the previous-month comparison snapshot.
                comparativoMesmoMesAnoAnterior, // Stores the same-month-last-year comparison snapshot.
                acumuladoAnoAtual, // Stores the selected year-to-date snapshot.
                comparativoAcumuladoAnoAnterior // Stores the year-to-date comparison against the previous year.
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
    private MonthlyComparisonDTO buildMonthlyComparison( // Starts the helper that builds any month-against-month comparison.
            User user, // Receives the authenticated user used as the owner filter.
            YearMonth comparedPeriod, // Receives the period that should be compared against the selected month.
            LocalDate startDate, // Receives the first day of the previous-month interval.
            LocalDate endDate, // Receives the last day of the previous-month interval.
            BigDecimal selectedReceitas, // Receives the selected-month income total used to calculate differences.
            BigDecimal selectedDespesas, // Receives the selected-month expense total used to calculate differences.
            BigDecimal selectedSaldo // Receives the selected-month balance used to calculate differences.
    ) { // Closes the method signature.
        BigDecimal comparedReceitas = sumByType(user, Transaction.TransactionType.RECEITA, startDate, endDate); // Loads the income total of the compared month.
        BigDecimal comparedDespesas = sumByType(user, Transaction.TransactionType.DESPESA, startDate, endDate); // Loads the expense total of the compared month.
        BigDecimal comparedSaldo = comparedReceitas.subtract(comparedDespesas); // Calculates the balance of the compared month.
        BigDecimal diferencaReceitas = selectedReceitas.subtract(comparedReceitas); // Calculates how much the selected-month income changed against the compared month.
        BigDecimal diferencaDespesas = selectedDespesas.subtract(comparedDespesas); // Calculates how much the selected-month expense changed against the compared month.
        BigDecimal diferencaSaldo = selectedSaldo.subtract(comparedSaldo); // Calculates how much the selected-month balance changed against the compared month.
        return new MonthlyComparisonDTO( // Creates the comparison DTO returned inside the main response.
                comparedPeriod.getYear(), // Stores the year of the compared month.
                comparedPeriod.getMonthValue(), // Stores the number of the compared month.
                comparedReceitas, // Stores the compared-month income total.
                comparedDespesas, // Stores the compared-month expense total.
                comparedSaldo, // Stores the compared-month balance.
                diferencaReceitas, // Stores the income difference against the compared month.
                diferencaDespesas, // Stores the expense difference against the compared month.
                diferencaSaldo, // Stores the balance difference against the compared month.
                evaluateIncomeTrend(diferencaReceitas), // Stores whether the selected month earned more, less or the same.
                evaluateExpenseTrend(diferencaDespesas), // Stores whether the selected month spent more, less or the same.
                evaluateBalanceTrend(diferencaSaldo), // Stores whether the selected month balance got better, worse or stayed equal.
                evaluateBalanceTrend(diferencaSaldo) // Stores the overall month comparison using the balance as the primary signal.
        ); // Finishes the comparison DTO creation.
    } // Closes the comparison helper.

    // Builds a year-to-date snapshot from January up to the selected month.
    private YearToDateSummaryDTO buildYearToDateSummary( // Starts the helper that assembles a year-to-date summary.
            User user, // Receives the authenticated user used as the owner filter.
            int year, // Receives the year represented by the accumulation.
            int monthLimit, // Receives the last month included in the accumulation.
            LocalDate startDate, // Receives the first day of the accumulation interval.
            LocalDate endDate // Receives the last day of the accumulation interval.
    ) { // Closes the method signature.
        BigDecimal totalReceitas = sumByType(user, Transaction.TransactionType.RECEITA, startDate, endDate); // Loads the accumulated income of the informed year-to-date interval.
        BigDecimal totalDespesas = sumByType(user, Transaction.TransactionType.DESPESA, startDate, endDate); // Loads the accumulated expenses of the informed year-to-date interval.
        BigDecimal saldo = totalReceitas.subtract(totalDespesas); // Calculates the accumulated balance of the informed year-to-date interval.
        return new YearToDateSummaryDTO( // Creates the year-to-date summary DTO.
                year, // Stores the year of the accumulation.
                monthLimit, // Stores the last month included in the accumulation.
                totalReceitas, // Stores the accumulated income total.
                totalDespesas, // Stores the accumulated expense total.
                saldo // Stores the accumulated balance.
        ); // Finishes the year-to-date summary creation.
    } // Closes the year-to-date summary helper.

    // Builds the comparison between the selected year-to-date snapshot and the previous year's equivalent interval.
    private YearToDateComparisonDTO buildYearToDateComparison( // Starts the helper that compares year-to-date snapshots.
            User user, // Receives the authenticated user used as the owner filter.
            YearToDateSummaryDTO currentYearSummary, // Receives the selected year-to-date snapshot already calculated.
            int previousYear, // Receives the previous year used in the comparison.
            int monthLimit, // Receives the last month included in both accumulated snapshots.
            LocalDate previousYearStartDate, // Receives the first day of the previous-year accumulation interval.
            LocalDate previousYearEndDate // Receives the last day of the previous-year accumulation interval.
    ) { // Closes the method signature.
        YearToDateSummaryDTO previousYearSummary = buildYearToDateSummary( // Builds the previous-year year-to-date snapshot using the same month limit.
                user, // Passes the authenticated user used to load the comparison snapshot.
                previousYear, // Passes the previous year represented by the snapshot.
                monthLimit, // Passes the same month limit used by the selected year.
                previousYearStartDate, // Passes the first day of the previous-year accumulation interval.
                previousYearEndDate // Passes the last day of the previous-year accumulation interval.
        ); // Finishes the previous-year snapshot creation.
        BigDecimal diferencaReceitas = currentYearSummary.totalReceitas().subtract(previousYearSummary.totalReceitas()); // Calculates how much the selected year-to-date income changed.
        BigDecimal diferencaDespesas = currentYearSummary.totalDespesas().subtract(previousYearSummary.totalDespesas()); // Calculates how much the selected year-to-date expenses changed.
        BigDecimal diferencaSaldo = currentYearSummary.saldo().subtract(previousYearSummary.saldo()); // Calculates how much the selected year-to-date balance changed.
        return new YearToDateComparisonDTO( // Creates the year-to-date comparison DTO returned in the main response.
                currentYearSummary, // Stores the selected year-to-date snapshot.
                previousYearSummary, // Stores the previous-year year-to-date snapshot.
                diferencaReceitas, // Stores the income difference between the two accumulations.
                diferencaDespesas, // Stores the expense difference between the two accumulations.
                diferencaSaldo, // Stores the balance difference between the two accumulations.
                evaluateIncomeTrend(diferencaReceitas), // Stores whether income improved, worsened or stayed equal.
                evaluateExpenseTrend(diferencaDespesas), // Stores whether spending improved, worsened or stayed equal.
                evaluateBalanceTrend(diferencaSaldo), // Stores whether the accumulated balance improved, worsened or stayed equal.
                evaluateBalanceTrend(diferencaSaldo) // Stores the overall year-to-date interpretation using the balance as the primary signal.
        ); // Finishes the year-to-date comparison creation.
    } // Closes the year-to-date comparison helper.

    // Interprets the income comparison where larger values are better.
    private AnalysisTrend evaluateIncomeTrend(BigDecimal difference) { // Starts the helper that interprets income differences.
        int comparison = difference.compareTo(BigDecimal.ZERO); // Compares the informed difference against zero to know its direction.
        if (comparison > 0) { // Checks whether the selected period earned more than the compared period.
            return AnalysisTrend.MELHOR; // Marks the income trend as better when revenue increased.
        } // Closes the positive-difference branch.
        if (comparison < 0) { // Checks whether the selected period earned less than the compared period.
            return AnalysisTrend.PIOR; // Marks the income trend as worse when revenue decreased.
        } // Closes the negative-difference branch.
        return AnalysisTrend.IGUAL; // Marks the income trend as equal when the values match exactly.
    } // Closes the income-trend helper.

    // Interprets the expense comparison where smaller values are better.
    private AnalysisTrend evaluateExpenseTrend(BigDecimal difference) { // Starts the helper that interprets expense differences.
        int comparison = difference.compareTo(BigDecimal.ZERO); // Compares the informed difference against zero to know its direction.
        if (comparison < 0) { // Checks whether the selected period spent less than the compared period.
            return AnalysisTrend.MELHOR; // Marks the expense trend as better when spending decreased.
        } // Closes the negative-difference branch.
        if (comparison > 0) { // Checks whether the selected period spent more than the compared period.
            return AnalysisTrend.PIOR; // Marks the expense trend as worse when spending increased.
        } // Closes the positive-difference branch.
        return AnalysisTrend.IGUAL; // Marks the expense trend as equal when the values match exactly.
    } // Closes the expense-trend helper.

    // Interprets the balance comparison where larger values are better.
    private AnalysisTrend evaluateBalanceTrend(BigDecimal difference) { // Starts the helper that interprets balance differences.
        int comparison = difference.compareTo(BigDecimal.ZERO); // Compares the informed difference against zero to know its direction.
        if (comparison > 0) { // Checks whether the selected period balance is higher than the compared period balance.
            return AnalysisTrend.MELHOR; // Marks the balance trend as better when the balance increased.
        } // Closes the positive-difference branch.
        if (comparison < 0) { // Checks whether the selected period balance is lower than the compared period balance.
            return AnalysisTrend.PIOR; // Marks the balance trend as worse when the balance decreased.
        } // Closes the negative-difference branch.
        return AnalysisTrend.IGUAL; // Marks the balance trend as equal when the values match exactly.
    } // Closes the balance-trend helper.
} // Closes the service class.
