package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // Helper method — extracts the authenticated user from the SecurityContext
    // Centralized here to avoid repeating in each method (DRY principle)
    private User getAuthenticatedUser() {
        // getname() return email
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        // If email in token, the user must exist in the database
        // orElseThrow: else to find, throw an exception (should never happen if token is valid)
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // Convert entity Transaction -> DTO answer
    private TransactionResponseDTO toResponseDTO(Transaction t) {
        return new TransactionResponseDTO(
                t.getId(),
                t.getType(),
                t.getDescription(),
                t.getCategory(),
                t.getAmount(),
                t.getPaymentMethod(),
                t.getTransactionDate(),
                t.getCreatedAt()
        );
    }

    // Create a new transaction for user authenticated
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();

        // Build the entity from DTO used Builder Lombok
        Transaction transaction = Transaction.builder()
                .user(user)
                .type(dto.type())
                .description(dto.description())
                .category(dto.category())
                .amount(dto.amount())
                .paymentMethod(dto.paymentMethod())
                .TransactionDate(dto.transactionDate())
                .build();

        //Save in database
        Transaction saved = transactionRepository.save(transaction);
                return toResponseDTO(transaction);
    }

    // List transaction with filters optional type and category
    public List<TransactionResponseDTO> findAll(
            Transaction.TransactionType type,
            Transaction.TransactionCategory category
    ) {
        User user = getAuthenticatedUser();
        List<Transaction> transactions;

        //
        if (type != null && category != null) {
            // Filter combined: type AND category
            transactions = transactionRepository
                    .findAllByUserAndTypeAndCategoryOrderTransactionsDateDesc(user, type, category);
        } else if (type != null) {
            // Filter of the type
            transactions = transactionRepository
                    .findAllByUserAndTypeOrderTransactionsDateDesc(user, type);
        } else if (category != null) {
            // Filter of the category
            transactions = transactionRepository
                    .findAllByUserAndCategoryOrderTransactionsDateDesc(user, category);
        } else {
            // No used filters, return all transactions of the user
            transactions = transactionRepository
                .findAllByUserOrderByDateDesc(user);
        }

        return transactions.stream()
                .map(this::toResponseDTO).toList();
    }

    // Update a transaction existing - verify if belong to the user
    public TransactionResponseDTO findById(Long id, TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();
        // findByIdANDUser: Ensure that the ID belong to this user
        // if not found, the user is trying to edit someone else's transaction
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Update exactly fields - Don't create a new object
        transaction.setType(dto.type());
        transaction.setDescription(dto.description());
        transaction.setCategory(dto.category());
        transaction.setAmount(dto.amount());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setTransactionDate(dto.transactionDate());

        Transaction updated = transactionRepository.save(transaction);
        return toResponseDTO(updated);
    }

    // Delete transaction - verify belong to the user old
    public void delete(Long id) {
        User user = getAuthenticatedUser();

        // same protection as the update - never delete without checking the owner
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transactionRepository.delete(transaction);
    }
}
