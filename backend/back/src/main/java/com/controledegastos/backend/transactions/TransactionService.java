package com.controledegastos.backend.transactions;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
     * Garante que apenas transacoes parceladas usem mais de uma parcela.
     */
    private Integer normalizeInstallments(TransactionRequestDTO dto) {
        if (dto.paymentMethod() == Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO) {
            if (dto.installments() == null || dto.installments() < 2) {
                throw new IllegalArgumentException("Compras parceladas devem ter pelo menos 2 parcelas");
            }

            return dto.installments();
        }

        if (dto.installments() != null && dto.installments() > 1) {
            throw new IllegalArgumentException("Apenas cartao de credito parcelado pode usar mais de 1 parcela");
        }

        return 1;
    }

    /**
     * Divide uma transacao parcelada em lancamentos mensais para refletir o impacto real no periodo.
     */
    private List<Transaction> buildInstallmentTransactions(User user, TransactionRequestDTO dto, Integer installments) {
        List<Transaction> transactions = new ArrayList<>();
        BigDecimal installmentAmount = dto.amount()
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal distributedAmount = installmentAmount.multiply(BigDecimal.valueOf(installments));
        BigDecimal remainder = dto.amount().subtract(distributedAmount);

        for (int installmentNumber = 1; installmentNumber <= installments; installmentNumber++) {
            LocalDate installmentDate = dto.transactionDate().plusMonths(installmentNumber - 1L);
            BigDecimal currentAmount = installmentNumber == installments
                    ? installmentAmount.add(remainder)
                    : installmentAmount;

            transactions.add(Transaction.builder()
                    .user(user)
                    .type(dto.type())
                    .description(dto.description() + " - Parcela " + installmentNumber + "/" + installments)
                    .category(dto.category())
                    .amount(currentAmount)
                    .paymentMethod(dto.paymentMethod())
                    .installments(installments)
                    .transactionDate(installmentDate)
                    .build());
        }

        return transactions;
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
                transaction.getInstallments(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }

    /**
     * Cria uma nova transacao pertencente ao usuario autenticado.
     */
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();
        Integer installments = normalizeInstallments(dto);

        if (dto.paymentMethod() == Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO) {
            List<Transaction> savedTransactions = transactionRepository.saveAll(buildInstallmentTransactions(user, dto, installments));
            return toResponseDTO(savedTransactions.getFirst());
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(dto.type())
                .description(dto.description())
                .category(dto.category())
                .amount(dto.amount())
                .paymentMethod(dto.paymentMethod())
                .installments(installments)
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
        Integer installments = normalizeInstallments(dto);
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setType(dto.type());
        transaction.setDescription(dto.description());
        transaction.setCategory(dto.category());
        transaction.setAmount(dto.amount());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setInstallments(installments);
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
