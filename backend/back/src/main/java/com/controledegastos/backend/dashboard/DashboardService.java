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

    // Assembles the full dashboard payload for the authenticated user.
    @Transactional(readOnly = true) // Keeps the operation optimized for reading and protects against accidental writes.
    public DashboardResponseDTO getDashboard() { // Starts the main use case of the dashboard module.
        User user = getAuthenticatedUser(); // Resolves which user owns the dashboard being requested.
        BigDecimal totalReceitas = transactionRepository.sumAmountByUserAndType( // Loads the total income for the user.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.RECEITA // Filters only income transactions.
        ); // Finishes the income aggregate query.
        BigDecimal totalDespesas = transactionRepository.sumAmountByUserAndType( // Loads the total expenses for the user.
                user, // Passes the authenticated user filter.
                Transaction.TransactionType.DESPESA // Filters only expense transactions.
        ); // Finishes the expense aggregate query.
        BigDecimal saldo = totalReceitas.subtract(totalDespesas); // Calculates the remaining balance from income minus expenses.
        List<TransactionResponseDTO> ultimasTransacoes = transactionRepository // Starts loading the recent transactions list.
                .findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(user) // Queries the five most recent transactions for the user.
                .stream() // Opens a stream for mapping entities to DTOs.
                .map(this::toTransactionResponseDTO) // Converts each entity into the transaction response DTO.
                .toList(); // Materializes the mapped list.
        List<DashboardCategorySummaryDTO> gastosPorCategoria = transactionRepository // Starts loading the expense totals grouped by category.
                .findExpenseSummaryByCategory(user) // Executes the grouping query for the authenticated user.
                .stream() // Opens a stream for projection-to-DTO mapping.
                .map(this::toCategorySummaryDTO) // Converts each projection into a frontend-friendly DTO.
                .toList(); // Materializes the mapped list.
        return new DashboardResponseDTO( // Builds the final dashboard payload.
                totalReceitas, // Includes the total income.
                totalDespesas, // Includes the total expenses.
                saldo, // Includes the calculated balance.
                ultimasTransacoes, // Includes the recent transaction list.
                gastosPorCategoria // Includes the grouped expense analysis.
        ); // Finishes the response creation.
    } // Closes the dashboard use case.
} // Closes the service class.
