package com.controledegastos.backend.dashboard; // Declares the package for the dashboard business logic.

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO; // Imports the DTO used for grouped category totals.
import com.controledegastos.backend.dashboard.dto.DashboardResponseDTO; // Imports the DTO returned by the dashboard endpoint.
import com.controledegastos.backend.security.AuthenticatedUserService; // Imports the shared helper that resolves the current authenticated user.
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO; // Imports the DTO reused for recent transactions.
import com.controledegastos.backend.transactions.Transaction; // Imports the transaction entity and its enums.
import com.controledegastos.backend.transactions.TransactionCategorySummaryProjection; // Imports the projection used for grouped expense queries.
import com.controledegastos.backend.transactions.TransactionRepository; // Imports the repository that supplies the dashboard data.
import com.controledegastos.backend.user.User; // Imports the authenticated user entity.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for injected dependencies.
import org.springframework.stereotype.Service; // Marks the class as a Spring service.
import org.springframework.transaction.annotation.Transactional; // Marks the read operation as transactional and read-only.

import java.math.BigDecimal; // Imports the precise type used for money calculations.
import java.time.DateTimeException; // Imports the exception type used when invalid year or month values are informed.
import java.time.LocalDate; // Imports the date type used to define the current-month interval.
import java.time.YearMonth; // Imports the type used to represent the current dashboard month.
import java.util.List; // Imports the list type used in the response.

@Service // Registers this class as a Spring-managed service bean.
@RequiredArgsConstructor // Generates a constructor with the required final dependencies.
public class DashboardService { // Declares the service responsible for assembling the dashboard.

    private final TransactionRepository transactionRepository; // Stores access to transaction data and aggregates.
    private final AuthenticatedUserService authenticatedUserService; // Stores the shared helper used to resolve the current user safely.

    // Extracts the authenticated user from the shared helper service.
    private User getAuthenticatedUser() { // Starts the helper that resolves who is making the request.
        return authenticatedUserService.getAuthenticatedUser(); // Delegates the responsibility to the shared authenticated user helper.
    } // Closes the authenticated-user helper.

    // Converts the transaction entity into the DTO already used by the transactions module.
    private TransactionResponseDTO toTransactionResponseDTO(Transaction transaction) { // Starts the mapper for recent transactions.
        return new TransactionResponseDTO( // Builds the response DTO returned to the client.
                transaction.getId(), // Copies the database identifier.
                transaction.getType(), // Copies the transaction type.
                transaction.getDescription(), // Copies the transaction description.
                transaction.getCategory(), // Copies the transaction category.
                transaction.getAmount(), // Copies the transaction amount.
                transaction.getPaymentMethod(), // Copies the payment method.
                transaction.getTransactionDate(), // Copies the business date of the transaction.
                transaction.getCreatedAt() // Copies the creation timestamp for traceability.
        ); // Finishes the DTO creation.
    } // Closes the transaction mapper.

    // Converts the projection result into a DTO that is easier for the frontend to consume.
    private DashboardCategorySummaryDTO toCategorySummaryDTO(TransactionCategorySummaryProjection projection) { // Starts the mapper for grouped categories.
        return new DashboardCategorySummaryDTO( // Builds the grouped category DTO.
                projection.getCategory(), // Copies the grouped category.
                projection.getTotalAmount() // Copies the summed amount for that category.
        ); // Finishes the grouped DTO creation.
    } // Closes the category mapper.

    // Validates and resolves the reference month used by the dashboard.
    private YearMonth resolveReferencePeriod(Integer year, Integer month) { // Starts the helper that decides which period the dashboard should use.
        if (year == null && month == null) { // Checks whether the client omitted both optional parameters.
            return YearMonth.now(); // Defaults to the real current month when no explicit period is informed.
        } // Closes the default-period branch.
        if (year == null || month == null) { // Checks whether only one part of the period was informed.
            throw new IllegalArgumentException("Both year and month must be informed together for the dashboard reference period"); // Prevents partial period requests that would create ambiguous results.
        } // Closes the partial-period validation branch.
        try { // Starts the protected conversion of the raw values into a YearMonth object.
            return YearMonth.of(year, month); // Builds the requested reference period when the values are valid.
        } catch (DateTimeException exception) { // Captures invalid year or month values.
            throw new IllegalArgumentException("Invalid year or month informed for dashboard reference period", exception); // Translates the low-level error into a domain-friendly validation message.
        } // Closes the invalid-period handling branch.
    } // Closes the period-resolution helper.

    // Assembles the full dashboard payload for the authenticated user.
    @Transactional(readOnly = true) // Keeps the operation optimized for reading and protects against accidental writes.
    public DashboardResponseDTO getDashboard(Integer year, Integer month) { // Starts the main use case of the dashboard module.
        User user = getAuthenticatedUser(); // Resolves which user owns the dashboard being requested.
        YearMonth periodoReferencia = resolveReferencePeriod(year, month); // Resolves the month that should drive the entire dashboard calculation.
        LocalDate inicioMesReferencia = periodoReferencia.atDay(1); // Calculates the first day of the selected month.
        LocalDate fimMesReferencia = periodoReferencia.atEndOfMonth(); // Calculates the last day of the selected month.
        LocalDate inicioAnoReferencia = periodoReferencia.atDay(1).withDayOfYear(1); // Calculates the first day of the selected year for year-to-date totals.
        BigDecimal totalReceitas = transactionRepository.sumAmountByUserAndTypeUpToDate( // Loads the accumulated income only up to the end of the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.RECEITA, // Filters only income transactions.
                fimMesReferencia // Prevents future income from leaking into a past dashboard view.
        ); // Finishes the income aggregate query.
        BigDecimal totalDespesas = transactionRepository.sumAmountByUserAndTypeUpToDate( // Loads the accumulated expenses only up to the end of the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.DESPESA, // Filters only expense transactions.
                fimMesReferencia // Prevents future expenses from leaking into a past dashboard view.
        ); // Finishes the expense aggregate query.
        BigDecimal saldo = totalReceitas.subtract(totalDespesas); // Calculates the accumulated balance carried from month to month.
        BigDecimal receitasAnoReferencia = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween( // Loads the income accumulated from January up to the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.RECEITA, // Filters only income transactions of the reference year.
                inicioAnoReferencia, // Defines the first day of the selected year.
                fimMesReferencia // Defines the last day of the selected month inside that year.
        ); // Finishes the year-to-date income query.
        BigDecimal despesasAnoReferencia = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween( // Loads the expenses accumulated from January up to the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.DESPESA, // Filters only expense transactions of the reference year.
                inicioAnoReferencia, // Defines the first day of the selected year.
                fimMesReferencia // Defines the last day of the selected month inside that year.
        ); // Finishes the year-to-date expense query.
        BigDecimal resultadoAnoReferencia = receitasAnoReferencia.subtract(despesasAnoReferencia); // Calculates the year-to-date result up to the selected month.
        BigDecimal receitasMesAtual = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween( // Loads the income restricted to the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.RECEITA, // Filters only income transactions of the selected month.
                inicioMesReferencia, // Defines the first day of the selected month interval.
                fimMesReferencia // Defines the last day of the selected month interval.
        ); // Finishes the current-month income aggregate query.
        BigDecimal despesasMesAtual = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween( // Loads the expenses restricted to the selected month.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.DESPESA, // Filters only expense transactions of the selected month.
                inicioMesReferencia, // Defines the first day of the selected month interval.
                fimMesReferencia // Defines the last day of the selected month interval.
        ); // Finishes the current-month expense aggregate query.
        BigDecimal resultadoMesAtual = receitasMesAtual.subtract(despesasMesAtual); // Calculates the pure result of the selected month without historical carry-over.
        List<TransactionResponseDTO> ultimasTransacoes = transactionRepository // Starts loading the recent transactions list.
                .findTop5ByUserAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(user, inicioMesReferencia, fimMesReferencia) // Queries the five most recent transactions only inside the selected month.
                .stream() // Opens a stream for mapping entities to DTOs.
                .map(this::toTransactionResponseDTO) // Converts each entity into the transaction response DTO.
                .toList(); // Materializes the mapped list.
        List<DashboardCategorySummaryDTO> gastosPorCategoria = transactionRepository // Starts loading the expense totals grouped by category.
                .findExpenseSummaryByCategoryAndTransactionDateBetween(user, inicioMesReferencia, fimMesReferencia) // Executes the grouping query for the authenticated user restricted to the selected month.
                .stream() // Opens a stream for projection-to-DTO mapping.
                .map(this::toCategorySummaryDTO) // Converts each projection into a frontend-friendly DTO.
                .toList(); // Materializes the mapped list.
        return new DashboardResponseDTO( // Builds the final dashboard payload.
                totalReceitas, // Includes the accumulated income up to the selected month.
                totalDespesas, // Includes the accumulated expenses up to the selected month.
                saldo, // Includes the accumulated balance up to the selected month.
                totalReceitas, // Includes the explicit accumulated income total.
                totalDespesas, // Includes the explicit accumulated expense total.
                saldo, // Includes the explicit accumulated balance.
                receitasAnoReferencia, // Includes the year-to-date income total.
                despesasAnoReferencia, // Includes the year-to-date expense total.
                resultadoAnoReferencia, // Includes the year-to-date result.
                receitasMesAtual, // Includes the income of the current month.
                despesasMesAtual, // Includes the expenses of the current month.
                resultadoMesAtual, // Includes the result of the current month.
                periodoReferencia.getYear(), // Includes the selected reference year used by the dashboard.
                periodoReferencia.getMonthValue(), // Includes the selected reference month used by the dashboard.
                ultimasTransacoes, // Includes the recent transaction list.
                gastosPorCategoria // Includes the grouped expense analysis.
        ); // Finishes the response creation.
    } // Closes the dashboard use case.
} // Closes the service class.
