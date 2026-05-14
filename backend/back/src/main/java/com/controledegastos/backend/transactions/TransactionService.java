package com.controledegastos.backend.transactions;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra as regras de negocio do modulo de transacoes.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Centraliza a leitura do usuario autenticado para todos os fluxos do modulo.
     */
    private User getAuthenticatedUser() {
        return authenticatedUserService.getAuthenticatedUser();
    }

    /**
     * Converte a entidade persistida no DTO devolvido para a API.
     */
    private TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }

    /**
     * Cria uma nova transacao pertencente ao usuario autenticado.
     */
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(dto.type())
                .description(dto.description())
                .category(dto.category())
                .amount(dto.amount())
                .paymentMethod(dto.paymentMethod())
                .transactionDate(dto.transactionDate())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return toResponseDTO(saved);
    }

    /**
     * Lista as transacoes do usuario aplicando os filtros opcionais recebidos pela API.
     */
    public List<TransactionResponseDTO> findAll(
            Transaction.TransactionType type,
            Transaction.TransactionCategory category
    ) {
        User user = getAuthenticatedUser();
        List<Transaction> transactions;

        if (type != null && category != null) {
            transactions = transactionRepository.findAllByUserAndTypeAndCategoryOrderByTransactionDateDesc(user, type, category);
        } else if (type != null) {
            transactions = transactionRepository.findAllByUserAndTypeOrderByTransactionDateDesc(user, type);
        } else if (category != null) {
            transactions = transactionRepository.findAllByUserAndCategoryOrderByTransactionDateDesc(user, category);
        } else {
            transactions = transactionRepository.findAllByUserOrderByTransactionDateDesc(user);
        }

        return transactions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Atualiza uma transacao existente apenas se ela pertencer ao usuario autenticado.
     */
    public TransactionResponseDTO update(Long id, TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setType(dto.type());
        transaction.setDescription(dto.description());
        transaction.setCategory(dto.category());
        transaction.setAmount(dto.amount());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setTransactionDate(dto.transactionDate());

        Transaction updated = transactionRepository.save(transaction);
        return toResponseDTO(updated);
    }

    /**
     * Remove uma transacao existente apenas se ela pertencer ao usuario autenticado.
     */
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transactionRepository.delete(transaction);
    }
}
