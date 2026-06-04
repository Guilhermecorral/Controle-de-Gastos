package com.controledegastos.backend.transactions.Repository;

import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.wishlist.WishlistItem;
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

    List<Transaction> findAllByUserOrderByTransactionDateDesc(User user);

    List<Transaction> findAllByUserAndTypeOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionType type
    );

    List<Transaction> findAllByUserAndCategoryOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionCategory category
    );

    List<Transaction> findAllByUserAndTypeAndCategoryOrderByTransactionDateDesc(
            User user,
            Transaction.TransactionType type,
            Transaction.TransactionCategory category
    );

    Optional<Transaction> findByIdAndUser(Long id, User user);

    List<Transaction> findAllByWishlistItemOrderByTransactionDateAscCreatedAtAsc(WishlistItem wishlistItem);

    void deleteAllByWishlistItem(WishlistItem wishlistItem);

    List<Transaction> findTop5ByUserOrderByTransactionDateDesc(User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = :type")
    BigDecimal sumAmountByUserAndType(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type
    );

    List<Transaction> findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(User user);

    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = com.controledegastos.backend.transactions.Transaction.TransactionType.DESPESA
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<TransactionCategorySummaryProjection> findExpenseSummaryByCategory(@Param("user") User user);

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

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.transactionDate <= :endDate
            """)
    BigDecimal sumAmountByUserAndTypeUpToDate(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS totalAmount
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<TransactionCategorySummaryProjection> findSummaryByCategoryAndTypeAndTransactionDateBetween(
            @Param("user") User user,
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

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

    List<Transaction> findTop5ByUserAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Transaction> findTopByUserAndTypeAndTransactionDateBetweenOrderByAmountDescTransactionDateDescCreatedAtDesc(
            User user,
            Transaction.TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    );
}
