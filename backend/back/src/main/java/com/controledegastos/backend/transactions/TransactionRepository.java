package com.controledegastos.backend.transactions;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends  JpaRepository<Transaction, Long> {

    // Search for a transaction by its id and user
    List<Transaction> findAllByUserOrderByTransactionDateDesc(User user);

    // Search for user AND type (RECEITA or DESPESA)
    List<Transaction> findAllByUserAndTypeOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionType type
    );

    // Search for a user AND category
    List<Transaction> findAllByUserAndCategoryOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionCategory category
    );

    // Search for user, type and category - filter combined
    List<Transaction> findAllByUserAndTypeAndCategoryOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionType type,
            Transaction.TransactionCategory category
    );

    // Search for a specific transaction ensuring that it belongs to the user
    Optional<Transaction> findByIdAndUser(Long id, User user);

    //
    List<Transaction> findTop5ByUserOrderByTransactionDateDesc(User user);

    // Returns the total amount of one transaction type for the authenticated user.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = :type")
    BigDecimal sumAmountByUserAndType(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type
    );

    // Returns the five most recent transactions using createdAt as a tie-breaker for same-day entries.
    List<Transaction> findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(User user);

    // Groups only expenses by category, which is exactly what the dashboard needs for spending analysis.
    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = com.controledegastos.backend.transactions.Transaction.TransactionType.DESPESA
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<TransactionCategorySummaryProjection> findExpenseSummaryByCategory(@Param("user") User user);

    // Returns the total amount of one transaction type limited to a date range.
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByUserAndTypeAndTransactionDateBetween(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Groups expenses by category limited to the informed date range.
    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = com.controledegastos.backend.transactions.Transaction.TransactionType.DESPESA
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<TransactionCategorySummaryProjection> findExpenseSummaryByCategoryAndTransactionDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Returns the biggest expense of the period, using date and creation time as tie-breakers.
    Optional<Transaction> findTopByUserAndTypeAndTransactionDateBetweenOrderByAmountDescTransactionDateDescCreatedAtDesc(
            User user,
            Transaction.TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    );
}
