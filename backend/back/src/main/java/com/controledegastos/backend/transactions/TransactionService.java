package com.controledegastos.backend.transactions;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.DTO.TransactionReceiptResponseDTO;
import com.controledegastos.backend.transactions.DTO.TransactionReceiptSummaryDTO;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra as regras de negocio do modulo de transacoes.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final TransactionReceiptStorageService transactionReceiptStorageService;

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
     * Remove o sufixo de parcela ao reconstruir grupos parcelados.
     */
    private String normalizeBaseDescription(String description) {
        return description.replaceFirst("\\s-\\sParcela\\s\\d+/\\d+$", "");
    }

    /**
     * Divide uma transacao parcelada em lancamentos mensais para refletir o impacto real no periodo.
     */
    private List<Transaction> buildInstallmentTransactions(
            User user,
            TransactionRequestDTO dto,
            Integer installments,
            UUID transactionGroupId,
            LocalDate baseDate
    ) {
        List<Transaction> transactions = new ArrayList<>();
        BigDecimal installmentAmount = dto.amount()
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal distributedAmount = installmentAmount.multiply(BigDecimal.valueOf(installments));
        BigDecimal remainder = dto.amount().subtract(distributedAmount);
        String baseDescription = normalizeBaseDescription(dto.description());

        for (int installmentNumber = 1; installmentNumber <= installments; installmentNumber++) {
            LocalDate installmentDate = baseDate.plusMonths(installmentNumber - 1L);
            BigDecimal currentAmount = installmentNumber == installments
                    ? installmentAmount.add(remainder)
                    : installmentAmount;

            transactions.add(Transaction.builder()
                    .user(user)
                    .type(dto.type())
                    .description(baseDescription + " - Parcela " + installmentNumber + "/" + installments)
                    .category(dto.category())
                    .amount(currentAmount)
                    .paymentMethod(dto.paymentMethod())
                    .installments(installments)
                    .transactionGroupId(transactionGroupId)
                    .transactionDate(installmentDate)
                    .build());
        }

        return transactions;
    }

    /**
     * Recria um grupo parcelado inteiro para manter a contabilidade consistente ao editar qualquer parcela.
     */
    private List<Transaction> replaceInstallmentGroup(
            User user,
            Transaction currentTransaction,
            TransactionRequestDTO dto,
            Integer installments
    ) {
        UUID groupId = currentTransaction.getTransactionGroupId() == null
                ? UUID.randomUUID()
                : currentTransaction.getTransactionGroupId();

        List<Transaction> groupedTransactions = transactionRepository
                .findAllByTransactionGroupIdAndUserOrderByTransactionDateAscCreatedAtAsc(groupId, user);

        List<Transaction> orderedTransactions = groupedTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).thenComparing(Transaction::getCreatedAt))
                .toList();

        int currentIndex = orderedTransactions.indexOf(currentTransaction);
        LocalDate baseDate = currentIndex >= 0
                ? dto.transactionDate().minusMonths(currentIndex)
                : dto.transactionDate();

        orderedTransactions.forEach(this::deleteReceiptIfPresent);
        transactionRepository.deleteAllByTransactionGroupIdAndUser(groupId, user);
        return transactionRepository.saveAll(buildInstallmentTransactions(user, dto, installments, groupId, baseDate));
    }

    /**
     * Converte os metadados do anexo fiscal em um resumo simples para a API.
     */
    private TransactionReceiptSummaryDTO toReceiptSummaryDTO(Transaction transaction) {
        if (transaction.getReceiptStorageName() == null || transaction.getReceiptUploadedAt() == null) {
            return null;
        }

        return new TransactionReceiptSummaryDTO(
                transaction.getReceiptOriginalFilename(),
                transaction.getReceiptContentType(),
                transaction.getReceiptSizeBytes(),
                transaction.getReceiptUploadedAt()
        );
    }

    /**
     * Remove o arquivo salvo e limpa os metadados da transacao.
     */
    private void deleteReceiptIfPresent(Transaction transaction) {
        if (transaction.getReceiptStorageName() == null) {
            return;
        }

        transactionReceiptStorageService.deleteReceipt(transaction.getReceiptStorageName(), transaction.getUser().getId());
        clearReceiptMetadata(transaction);
    }

    /**
     * Zera os campos relacionados ao anexo fiscal.
     */
    private void clearReceiptMetadata(Transaction transaction) {
        transaction.setReceiptOriginalFilename(null);
        transaction.setReceiptStorageName(null);
        transaction.setReceiptContentType(null);
        transaction.setReceiptSizeBytes(null);
        transaction.setReceiptUploadedAt(null);
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
                transaction.getCreatedAt(),
                toReceiptSummaryDTO(transaction)
        );
    }

    /**
     * Cria uma nova transacao pertencente ao usuario autenticado.
     */
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        User user = getAuthenticatedUser();
        Integer installments = normalizeInstallments(dto);

        if (dto.paymentMethod() == Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO) {
            List<Transaction> savedTransactions = transactionRepository.saveAll(
                    buildInstallmentTransactions(user, dto, installments, UUID.randomUUID(), dto.transactionDate())
            );
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
                .transactionGroupId(null)
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
                .orElseThrow(() -> new ResourceNotFoundException("Transacao nao encontrada"));

        if (transaction.getTransactionGroupId() != null || dto.paymentMethod() == Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO) {
            List<Transaction> recreatedGroup = replaceInstallmentGroup(user, transaction, dto, installments);
            return toResponseDTO(recreatedGroup.getFirst());
        }

        transaction.setType(dto.type());
        transaction.setDescription(dto.description());
        transaction.setCategory(dto.category());
        transaction.setAmount(dto.amount());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setInstallments(installments);
        transaction.setTransactionGroupId(null);
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
                .orElseThrow(() -> new ResourceNotFoundException("Transacao nao encontrada"));

        if (transaction.getTransactionGroupId() != null) {
            transactionRepository.findAllByTransactionGroupIdAndUserOrderByTransactionDateAscCreatedAtAsc(transaction.getTransactionGroupId(), user)
                    .forEach(this::deleteReceiptIfPresent);
            transactionRepository.deleteAllByTransactionGroupIdAndUser(transaction.getTransactionGroupId(), user);
            return;
        }

        deleteReceiptIfPresent(transaction);
        transactionRepository.delete(transaction);
    }

    /**
     * Anexa ou substitui uma nota fiscal na transacao escolhida.
     */
    public TransactionResponseDTO attachReceipt(Long id, MultipartFile file) {
        User user = getAuthenticatedUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transacao nao encontrada"));

        List<Transaction> targetTransactions = transaction.getTransactionGroupId() == null
                ? List.of(transaction)
                : transactionRepository.findAllByTransactionGroupIdAndUserOrderByTransactionDateAscCreatedAtAsc(
                        transaction.getTransactionGroupId(),
                        user
                );

        targetTransactions.forEach(this::deleteReceiptIfPresent);

        TransactionReceiptStorageService.StoredReceipt storedReceipt = transactionReceiptStorageService
                .saveReceipt(user.getId(), transaction.getId(), file);

        LocalDateTime uploadedAt = LocalDateTime.now();
        targetTransactions.forEach(targetTransaction -> {
            targetTransaction.setReceiptOriginalFilename(storedReceipt.originalFilename());
            targetTransaction.setReceiptStorageName(storedReceipt.storageName());
            targetTransaction.setReceiptContentType(storedReceipt.contentType());
            targetTransaction.setReceiptSizeBytes(storedReceipt.sizeBytes());
            targetTransaction.setReceiptUploadedAt(uploadedAt);
        });

        List<Transaction> savedTransactions = transactionRepository.saveAll(targetTransactions);
        Transaction savedTransaction = savedTransactions.stream()
                .filter(saved -> saved.getId().equals(transaction.getId()))
                .findFirst()
                .orElse(savedTransactions.getFirst());

        return toResponseDTO(savedTransaction);
    }

    /**
     * Lista as notas fiscais anexadas no ano e mes escolhidos pelo usuario.
     */
    @Transactional(readOnly = true)
    public List<TransactionReceiptResponseDTO> listReceiptsByPeriod(int year, int month) {
        User user = getAuthenticatedUser();
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Informe um mes valido para consultar as notas fiscais");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Transaction> transactions = transactionRepository
                .findAllByUserAndReceiptStorageNameIsNotNullAndTransactionDateBetweenOrderByTransactionDateDescReceiptUploadedAtDesc(
                        user,
                        startDate,
                        endDate
                );

        Map<String, List<Transaction>> groupedTransactions = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            String groupKey = transaction.getReceiptStorageName() == null
                    ? "transaction-" + transaction.getId()
                    : transaction.getReceiptStorageName();
            groupedTransactions.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(transaction);
        }

        return groupedTransactions.values().stream()
                .map(group -> {
                    Transaction transaction = group.getFirst();
                    return new TransactionReceiptResponseDTO(
                            transaction.getId(),
                            transaction.getType(),
                            transaction.getDescription(),
                            transaction.getCategory(),
                            transaction.getAmount(),
                            transaction.getPaymentMethod(),
                            transaction.getInstallments(),
                            transaction.getTransactionDate(),
                            transaction.getReceiptOriginalFilename(),
                            transaction.getReceiptContentType(),
                            transaction.getReceiptSizeBytes(),
                            transaction.getReceiptUploadedAt(),
                            group.size()
                    );
                })
                .toList();
    }

    /**
     * Carrega o arquivo salvo de uma nota fiscal para download autenticado.
     */
    @Transactional(readOnly = true)
    public ReceiptDownloadPayload loadReceiptForDownload(Long id) {
        User user = getAuthenticatedUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transacao nao encontrada"));

        if (transaction.getReceiptStorageName() == null) {
            throw new ResourceNotFoundException("Nenhuma nota fiscal foi anexada a esta transacao");
        }

        Resource resource = transactionReceiptStorageService.loadReceipt(transaction.getReceiptStorageName(), user.getId());
        return new ReceiptDownloadPayload(
                resource,
                transaction.getReceiptOriginalFilename(),
                transaction.getReceiptContentType()
        );
    }

    /**
     * Agrupa o arquivo e seus metadados para o endpoint de download.
     */
    public record ReceiptDownloadPayload(
            Resource resource,
            String originalFilename,
            String contentType
    ) {
    }
}
