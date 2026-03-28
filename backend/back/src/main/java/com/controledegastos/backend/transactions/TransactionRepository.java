package com.controledegastos.backend.transactions;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends  JpaRepository<Transaction, Long> {

    // Search for a transaction by its id and user
    List<Transaction> findAllByUserOrderByDateDesc(User user);

    // Search for user AND type (RECEITA or DESPESA)
    List<Transaction> findAllByUserAndTypeOrderTransactionsDateDesc(
            User user,
            Transaction.TransactionType type
    );

    // Search for a user AND category
    List<Transaction> findAllByUserAndCategoryOrderTransactionsDateDesc(
            User user,
            Transaction.TransactionCategory category
    );

    // Search for user, type and category - filter combined
    List<Transaction> findAllByUserAndTypeAndCategoryOrderTransactionsDateDesc(
            User user,
            Transaction.TransactionType type,
            Transaction.TransactionCategory category
    );

    // Search for a specific transaction ensuring that it belongs to the user
    Optional<Transaction> findByIdAndUser(Long id, User user);

    //
    List<Transaction> findTop5ByUserOrderByTransactionsDateDesc(User user);
}
