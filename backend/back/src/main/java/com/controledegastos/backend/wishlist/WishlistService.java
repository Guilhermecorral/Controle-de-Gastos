package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.TransactionRepository;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistSortBy;
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter;
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    private User getAuthenticatedUser() {
        return authenticatedUserService.getAuthenticatedUser();
    }

    private WishlistItem getOwnedWishlistItem(Long id, User user) {
        return wishlistRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
    }

    private void validateWishlistRequest(WishlistRequestDTO dto) {
        if (dto.description() == null || dto.description().isBlank()) {
            throw new IllegalArgumentException("Wishlist item description is required");
        }

        if (dto.originalPrice() == null || dto.originalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Wishlist item original price must be greater than zero");
        }

        if (dto.discountPercent() != null && dto.discountPercent().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Wishlist item discount percent cannot be negative");
        }

        if (dto.discountPercent() != null && dto.discountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Wishlist item discount percent cannot be greater than 100");
        }
    }

    private void validatePurchaseRequest(WishlistPurchaseRequestDTO dto) {
        if (dto.purchaseDate() == null) {
            throw new IllegalArgumentException("Purchase date is required");
        }

        if (dto.paymentMethod() == null) {
            throw new IllegalArgumentException("Purchase payment method is required");
        }

        if (dto.paymentMethod() == WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO) {
            if (dto.installments() == null || dto.installments() < 2) {
                throw new IllegalArgumentException("Parcelled purchases must have at least 2 installments");
            }
            return;
        }

        if (dto.installments() != null && dto.installments() != 1) {
            throw new IllegalArgumentException("Only parcelled credit-card purchases can use more than 1 installment");
        }
    }

    private WishlistResponseDTO toResponseDTO(WishlistItem item) {
        return new WishlistResponseDTO(
                item.getId(),
                item.getDescription(),
                item.getOriginalPrice(),
                item.getDiscountPercent(),
                item.getFinalPrice(),
                item.getPriority(),
                item.getCategory(),
                item.getNotes(),
                item.getStatus(),
                item.getPurchaseDate(),
                item.getPaymentMethod(),
                item.getInstallments(),
                item.getFirstInstallmentNextMonth(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private Sort buildSort(WishlistSortBy sortBy) {
        WishlistSortBy effectiveSort = sortBy == null ? WishlistSortBy.PERSONALIZADO : sortBy;

        return switch (effectiveSort) {
            case MENOR_PRECO -> Sort.by(Sort.Order.asc("finalPrice"), Sort.Order.desc("createdAt"));
            case MAIOR_PRECO -> Sort.by(Sort.Order.desc("finalPrice"), Sort.Order.desc("createdAt"));
            case PRIORIDADE -> Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("createdAt"));
            case ADICIONADOS_RECENTEMENTE -> Sort.by(Sort.Order.desc("createdAt"));
            case PERSONALIZADO -> Sort.by(
                    Sort.Order.asc("status"),
                    Sort.Order.asc("priority"),
                    Sort.Order.asc("finalPrice"),
                    Sort.Order.desc("createdAt")
            );
        };
    }

    private List<WishlistResponseDTO> mapList(List<WishlistItem> items) {
        return items.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private Transaction.TransactionCategory mapCategory(WishlistItem.WishlistCategory category) {
        return Transaction.TransactionCategory.valueOf(category.name());
    }

    private Transaction.PaymentMethod mapPaymentMethod(WishlistItem.PurchasePaymentMethod paymentMethod) {
        return Transaction.PaymentMethod.valueOf(paymentMethod.name());
    }

    private List<Transaction> buildPurchaseTransactions(WishlistItem item) {
        List<Transaction> generatedTransactions = new ArrayList<>();
        int installments = item.getInstallments() == null ? 1 : item.getInstallments();

        LocalDate baseDate = Boolean.TRUE.equals(item.getFirstInstallmentNextMonth())
                ? item.getPurchaseDate().plusMonths(1)
                : item.getPurchaseDate();

        if (item.getPaymentMethod() != WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO) {
            generatedTransactions.add(Transaction.builder()
                    .user(item.getUser())
                    .wishlistItem(item)
                    .type(Transaction.TransactionType.DESPESA)
                    .description(item.getDescription())
                    .category(mapCategory(item.getCategory()))
                    .amount(item.getFinalPrice())
                    .paymentMethod(mapPaymentMethod(item.getPaymentMethod()))
                    .transactionDate(baseDate)
                    .build());
            return generatedTransactions;
        }

        BigDecimal baseInstallmentAmount = item.getFinalPrice()
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal accumulatedAmount = BigDecimal.ZERO;

        for (int installmentNumber = 1; installmentNumber <= installments; installmentNumber++) {
            LocalDate installmentDate = baseDate.plusMonths((long) installmentNumber - 1L);
            BigDecimal installmentAmount = installmentNumber == installments
                    ? item.getFinalPrice().subtract(accumulatedAmount)
                    : baseInstallmentAmount;

            accumulatedAmount = accumulatedAmount.add(installmentAmount);

            generatedTransactions.add(Transaction.builder()
                    .user(item.getUser())
                    .wishlistItem(item)
                    .type(Transaction.TransactionType.DESPESA)
                    .description(item.getDescription() + " - Parcela " + installmentNumber + "/" + installments)
                    .category(mapCategory(item.getCategory()))
                    .amount(installmentAmount)
                    .paymentMethod(Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO)
                    .transactionDate(installmentDate)
                    .build());
        }

        return generatedTransactions;
    }

    @Transactional
    public WishlistResponseDTO create(WishlistRequestDTO dto) {
        validateWishlistRequest(dto);

        User user = getAuthenticatedUser();

        WishlistItem item = WishlistItem.builder()
                .description(dto.description())
                .originalPrice(dto.originalPrice())
                .discountPercent(dto.discountPercent() == null ? BigDecimal.ZERO : dto.discountPercent())
                .priority(dto.priority() == null ? WishlistItem.Priority.MEDIA : dto.priority())
                .category(dto.category() == null ? WishlistItem.WishlistCategory.COMPRAS : dto.category())
                .notes(dto.notes())
                .status(WishlistItem.WishlistStatus.PENDENTE)
                .installments(1)
                .firstInstallmentNextMonth(false)
                .user(user)
                .build();

        item.calculateFinalPrice();

        return toResponseDTO(wishlistRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<WishlistResponseDTO> findAll(WishlistStatusFilter statusFilter, WishlistSortBy sortBy) {
        User user = getAuthenticatedUser();
        Sort sort = buildSort(sortBy);
        WishlistStatusFilter effectiveFilter = statusFilter == null ? WishlistStatusFilter.TODOS : statusFilter;

        List<WishlistItem> items = switch (effectiveFilter) {
            case PENDENTE -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE, sort);
            case COMPRADO -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO, sort);
            case TODOS -> wishlistRepository.findAllByUser(user, sort);
        };

        return mapList(items);
    }

    @Transactional(readOnly = true)
    public WishlistSummaryDTO getSummary() {
        User user = getAuthenticatedUser();

        long quantidadeItensDesejados = wishlistRepository.countByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE);
        long quantidadeItensComprados = wishlistRepository.countByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO);
        BigDecimal valorTotalDesejados = wishlistRepository.sumFinalPriceByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE);
        BigDecimal valorTotalComprados = wishlistRepository.sumFinalPriceByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO);

        return new WishlistSummaryDTO(
                quantidadeItensDesejados,
                quantidadeItensComprados,
                valorTotalDesejados,
                valorTotalComprados
        );
    }

    @Transactional
    public WishlistResponseDTO update(Long id, WishlistRequestDTO dto) {
        validateWishlistRequest(dto);

        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        if (item.getStatus() == WishlistItem.WishlistStatus.COMPRADO) {
            throw new IllegalArgumentException("Purchased wishlist items cannot be edited through the generic update flow");
        }

        item.setDescription(dto.description());
        item.setOriginalPrice(dto.originalPrice());
        item.setDiscountPercent(dto.discountPercent() == null ? BigDecimal.ZERO : dto.discountPercent());
        item.setPriority(dto.priority() == null ? WishlistItem.Priority.MEDIA : dto.priority());
        item.setCategory(dto.category() == null ? WishlistItem.WishlistCategory.COMPRAS : dto.category());
        item.setNotes(dto.notes());
        item.calculateFinalPrice();

        return toResponseDTO(wishlistRepository.save(item));
    }

    @Transactional
    public WishlistResponseDTO markAsPurchased(Long id, WishlistPurchaseRequestDTO dto) {
        validatePurchaseRequest(dto);

        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        if (item.getStatus() == WishlistItem.WishlistStatus.COMPRADO) {
            throw new IllegalArgumentException("Wishlist item is already marked as purchased");
        }

        item.setStatus(WishlistItem.WishlistStatus.COMPRADO);
        item.setPurchaseDate(dto.purchaseDate());
        item.setPaymentMethod(dto.paymentMethod());
        item.setInstallments(dto.installments() == null ? 1 : dto.installments());
        item.setFirstInstallmentNextMonth(Boolean.TRUE.equals(dto.firstInstallmentNextMonth()));

        WishlistItem savedItem = wishlistRepository.save(item);
        transactionRepository.saveAll(buildPurchaseTransactions(savedItem));

        return toResponseDTO(savedItem);
    }

    @Transactional
    public WishlistResponseDTO undoPurchase(Long id) {
        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        if (item.getStatus() != WishlistItem.WishlistStatus.COMPRADO) {
            throw new IllegalArgumentException("Only purchased wishlist items can undo the purchase");
        }

        transactionRepository.deleteAllByWishlistItem(item);

        item.setStatus(WishlistItem.WishlistStatus.PENDENTE);
        item.setPurchaseDate(null);
        item.setPaymentMethod(null);
        item.setInstallments(1);
        item.setFirstInstallmentNextMonth(false);

        return toResponseDTO(wishlistRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        transactionRepository.deleteAllByWishlistItem(item);
        wishlistRepository.delete(item);
    }
}
