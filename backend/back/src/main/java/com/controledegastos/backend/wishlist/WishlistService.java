package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.wishlist.Repository.WishlistHistoryEntryRepository;
import com.controledegastos.backend.wishlist.Repository.WishlistListRepository;
import com.controledegastos.backend.wishlist.Repository.WishlistRepository;
import com.controledegastos.backend.wishlist.dto.WishlistHistoryResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistListRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistListResponseDTO;
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
import java.util.UUID;

/**
 * Orquestra as regras de negocio da wishlist e sua integracao com transacoes.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistListRepository wishlistListRepository;
    private final WishlistHistoryEntryRepository wishlistHistoryEntryRepository;
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Centraliza a leitura do usuario autenticado para todos os fluxos da wishlist.
     */
    private User getAuthenticatedUser() {
        return authenticatedUserService.getAuthenticatedUser();
    }

    /**
     * Garante que o item consultado existe e pertence ao usuario autenticado.
     */
    private WishlistItem getOwnedWishlistItem(Long id, User user) {
        return wishlistRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
    }

    /**
     * Garante que a lista consultada existe e pertence ao usuario autenticado.
     */
    private WishlistList getOwnedWishlistList(Long id, User user) {
        return wishlistListRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Wishlist list not found"));
    }

    /**
     * Localiza a primeira transacao gerada por uma compra da wishlist para integrar anexos fiscais.
     */
    private Long resolveLinkedTransactionId(WishlistItem item) {
        return transactionRepository.findTopByWishlistItemOrderByTransactionDateAscCreatedAtAsc(item)
                .map(Transaction::getId)
                .orElse(null);
    }

    /**
     * Cria ou recupera a lista padrao do usuario, usada como fallback seguro da 6.1.
     */
    private WishlistList getOrCreateDefaultList(User user) {
        List<WishlistList> defaultLists = wishlistListRepository.findAllByUserAndIsDefaultTrueOrderByCreatedAtAsc(user);

        if (defaultLists.isEmpty()) {
            return wishlistListRepository.save(
                    WishlistList.builder()
                            .name("Lista Principal")
                            .description("Lista padrao criada automaticamente pelo sistema")
                            .isDefault(true)
                            .user(user)
                            .build()
            );
        }

        WishlistList canonicalDefaultList = defaultLists.getFirst();

        if (defaultLists.size() > 1) {
            defaultLists.stream()
                    .skip(1)
                    .forEach(list -> list.setIsDefault(false));

            wishlistListRepository.saveAll(defaultLists);
        }

        return canonicalDefaultList;
    }

    /**
     * Resolve a lista de destino recebida pela API ou usa a lista padrao quando nada foi informado.
     */
    private WishlistList resolveTargetList(Long listId, User user) {
        if (listId == null) {
            return getOrCreateDefaultList(user);
        }

        return getOwnedWishlistList(listId, user);
    }

    /**
     * Valida os campos minimos de criacao e edicao de um item da wishlist.
     */
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

    /**
     * Valida os dados exigidos ao concluir uma compra, especialmente no caso de parcelamento.
     */
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

    /**
     * Valida o nome usado para criar ou editar uma lista nomeada da wishlist.
     */
    private void validateWishlistListRequest(WishlistListRequestDTO dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("Wishlist list name is required");
        }
    }

    /**
     * Converte a entidade persistida no DTO devolvido pelos endpoints da wishlist.
     */
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
                item.getArchivedAfterPurchase(),
                resolveLinkedTransactionId(item),
                item.getWishlistList().getId(),
                item.getWishlistList().getName(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    /**
     * Converte a entidade de lista no DTO usado pelo frontend da wishlist.
     */
    private WishlistListResponseDTO toListResponseDTO(WishlistList list) {
        return new WishlistListResponseDTO(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getIsDefault(),
                wishlistRepository.countByWishlistList(list),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }

    /**
     * Converte um evento do historico no DTO devolvido ao cliente.
     */
    private WishlistHistoryResponseDTO toHistoryResponseDTO(WishlistHistoryEntry entry) {
        return new WishlistHistoryResponseDTO(
                entry.getId(),
                entry.getActionType(),
                entry.getDescription(),
                entry.getFinalPriceSnapshot(),
                entry.getListNameSnapshot(),
                entry.getCreatedAt()
        );
    }

    /**
     * Registra eventos importantes do ciclo de vida do item para auditoria funcional da wishlist.
     */
    private void registerHistory(WishlistItem item, WishlistHistoryEntry.ActionType actionType, String description) {
        wishlistHistoryEntryRepository.save(
                WishlistHistoryEntry.builder()
                        .wishlistItem(item)
                        .user(item.getUser())
                        .actionType(actionType)
                        .description(description)
                        .finalPriceSnapshot(item.getFinalPrice())
                        .listNameSnapshot(item.getWishlistList().getName())
                        .build()
        );
    }

    /**
     * Traduz a opcao de ordenacao da API no objeto Sort usado pelo repositorio.
     */
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

    /**
     * Reaproveita as categorias da transacao para espelhar compras no modulo financeiro.
     */
    private Transaction.TransactionCategory mapCategory(WishlistItem.WishlistCategory category) {
        return Transaction.TransactionCategory.valueOf(category.name());
    }

    /**
     * Reaproveita as formas de pagamento da transacao para a integracao financeira.
     */
    private Transaction.PaymentMethod mapPaymentMethod(WishlistItem.PurchasePaymentMethod paymentMethod) {
        return Transaction.PaymentMethod.valueOf(paymentMethod.name());
    }

    /**
     * Gera as transacoes que devem nascer quando um item e marcado como comprado.
     */
    private List<Transaction> buildPurchaseTransactions(WishlistItem item) {
        List<Transaction> generatedTransactions = new ArrayList<>();
        int installments = item.getInstallments() == null ? 1 : item.getInstallments();
        UUID transactionGroupId = item.getPaymentMethod() == WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO
                ? UUID.randomUUID()
                : null;

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
                    .transactionGroupId(transactionGroupId)
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
                    .transactionGroupId(transactionGroupId)
                    .transactionDate(installmentDate)
                    .build());
        }

        return generatedTransactions;
    }

    /**
     * Cria uma nova lista nomeada da wishlist.
     */
    @Transactional
    public WishlistListResponseDTO createList(WishlistListRequestDTO dto) {
        validateWishlistListRequest(dto);

        User user = getAuthenticatedUser();
        if (wishlistListRepository.existsByUserAndNameIgnoreCase(user, dto.name())) {
            throw new IllegalArgumentException("Wishlist list name already exists");
        }

        WishlistList createdList = wishlistListRepository.save(
                WishlistList.builder()
                        .name(dto.name())
                        .description(dto.description())
                        .user(user)
                        .build()
        );

        getOrCreateDefaultList(user);
        return toListResponseDTO(createdList);
    }

    /**
     * Lista todas as listas da wishlist do usuario, sempre incluindo a lista padrao.
     */
    @Transactional(readOnly = true)
    public List<WishlistListResponseDTO> findAllLists() {
        User user = getAuthenticatedUser();
        getOrCreateDefaultList(user);

        return wishlistListRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                .map(this::toListResponseDTO)
                .toList();
    }

    /**
     * Atualiza nome e descricao de uma lista nao padrao.
     */
    @Transactional
    public WishlistListResponseDTO updateList(Long id, WishlistListRequestDTO dto) {
        validateWishlistListRequest(dto);

        User user = getAuthenticatedUser();
        WishlistList list = getOwnedWishlistList(id, user);

        if (Boolean.TRUE.equals(list.getIsDefault())) {
            throw new IllegalArgumentException("The default wishlist list cannot be renamed");
        }

        if (!list.getName().equalsIgnoreCase(dto.name()) && wishlistListRepository.existsByUserAndNameIgnoreCase(user, dto.name())) {
            throw new IllegalArgumentException("Wishlist list name already exists");
        }

        list.setName(dto.name());
        list.setDescription(dto.description());
        return toListResponseDTO(wishlistListRepository.save(list));
    }

    /**
     * Remove uma lista personalizada e move seus itens para a lista padrao para evitar perda de dados.
     */
    @Transactional
    public void deleteList(Long id) {
        User user = getAuthenticatedUser();
        WishlistList list = getOwnedWishlistList(id, user);

        if (Boolean.TRUE.equals(list.getIsDefault())) {
            throw new IllegalArgumentException("The default wishlist list cannot be deleted");
        }

        WishlistList defaultList = getOrCreateDefaultList(user);
        List<WishlistItem> items = wishlistRepository.findAllByUserAndWishlistList(user, list, Sort.unsorted());

        for (WishlistItem item : items) {
            item.setWishlistList(defaultList);
            registerHistory(item, WishlistHistoryEntry.ActionType.MOVED, "Item moved to the default list after deleting its original list");
        }

        wishlistRepository.saveAll(items);
        wishlistListRepository.delete(list);
    }

    /**
     * Cria um item pendente na wishlist do usuario autenticado.
     */
    @Transactional
    public WishlistResponseDTO create(WishlistRequestDTO dto) {
        validateWishlistRequest(dto);

        User user = getAuthenticatedUser();
        WishlistList targetList = resolveTargetList(dto.listId(), user);

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
                .archivedAfterPurchase(false)
                .user(user)
                .wishlistList(targetList)
                .build();

        item.calculateFinalPrice();

        WishlistItem savedItem = wishlistRepository.save(item);
        registerHistory(savedItem, WishlistHistoryEntry.ActionType.CREATED, "Item added to wishlist");
        return toResponseDTO(savedItem);
    }

    /**
     * Lista os itens da wishlist com filtro de status, ordenacao e filtro opcional por lista.
     */
    @Transactional(readOnly = true)
    public List<WishlistResponseDTO> findAll(WishlistStatusFilter statusFilter, WishlistSortBy sortBy, Long listId) {
        User user = getAuthenticatedUser();
        Sort sort = buildSort(sortBy);
        WishlistStatusFilter effectiveFilter = statusFilter == null ? WishlistStatusFilter.TODOS : statusFilter;
        WishlistList targetList = listId == null ? null : getOwnedWishlistList(listId, user);

        List<WishlistItem> items;
        if (targetList != null) {
            items = switch (effectiveFilter) {
                case PENDENTE -> wishlistRepository.findAllByUserAndWishlistListAndStatus(user, targetList, WishlistItem.WishlistStatus.PENDENTE, sort);
                case COMPRADO -> wishlistRepository.findAllByUserAndWishlistListAndStatus(user, targetList, WishlistItem.WishlistStatus.COMPRADO, sort);
                case TODOS -> wishlistRepository.findAllByUserAndWishlistList(user, targetList, sort);
            };
        } else {
            items = switch (effectiveFilter) {
                case PENDENTE -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE, sort);
                case COMPRADO -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO, sort);
                case TODOS -> wishlistRepository.findAllByUser(user, sort);
            };
        }

        return items.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Calcula o resumo principal exibido na visao agregada da wishlist.
     */
    @Transactional(readOnly = true)
    public WishlistSummaryDTO getSummary() {
        User user = getAuthenticatedUser();
        getOrCreateDefaultList(user);

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

    /**
     * Atualiza um item ainda pendente sem permitir edicao generica de compras ja concluidas.
     */
    @Transactional
    public WishlistResponseDTO update(Long id, WishlistRequestDTO dto) {
        validateWishlistRequest(dto);

        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);
        WishlistList previousList = item.getWishlistList();
        WishlistList targetList = resolveTargetList(dto.listId(), user);

        if (item.getStatus() == WishlistItem.WishlistStatus.COMPRADO) {
            throw new IllegalArgumentException("Purchased wishlist items cannot be edited through the generic update flow");
        }

        item.setDescription(dto.description());
        item.setOriginalPrice(dto.originalPrice());
        item.setDiscountPercent(dto.discountPercent() == null ? BigDecimal.ZERO : dto.discountPercent());
        item.setPriority(dto.priority() == null ? WishlistItem.Priority.MEDIA : dto.priority());
        item.setCategory(dto.category() == null ? WishlistItem.WishlistCategory.COMPRAS : dto.category());
        item.setNotes(dto.notes());
        item.setWishlistList(targetList);
        item.calculateFinalPrice();

        WishlistItem savedItem = wishlistRepository.save(item);
        registerHistory(savedItem, WishlistHistoryEntry.ActionType.UPDATED, "Item details updated");

        if (!previousList.getId().equals(targetList.getId())) {
            registerHistory(savedItem, WishlistHistoryEntry.ActionType.MOVED, "Item moved between wishlist lists");
        }

        return toResponseDTO(savedItem);
    }

    /**
     * Marca um item como comprado e cria as transacoes correspondentes.
     */
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
        item.setArchivedAfterPurchase(true);

        WishlistItem savedItem = wishlistRepository.save(item);
        transactionRepository.saveAll(buildPurchaseTransactions(savedItem));
        registerHistory(savedItem, WishlistHistoryEntry.ActionType.PURCHASED, "Item marked as purchased");

        return toResponseDTO(savedItem);
    }

    /**
     * Reverte uma compra e apaga as transacoes geradas a partir dela.
     */
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
        item.setArchivedAfterPurchase(false);

        WishlistItem savedItem = wishlistRepository.save(item);
        registerHistory(savedItem, WishlistHistoryEntry.ActionType.PURCHASE_UNDONE, "Purchase undone and financial entries removed");
        return toResponseDTO(savedItem);
    }

    /**
     * Devolve o historico de alteracoes de um item da wishlist.
     */
    @Transactional(readOnly = true)
    public List<WishlistHistoryResponseDTO> getHistory(Long id) {
        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        return wishlistHistoryEntryRepository.findAllByWishlistItemAndUserOrderByCreatedAtDesc(item, user).stream()
                .map(this::toHistoryResponseDTO)
                .toList();
    }

    /**
     * Remove o item da wishlist e qualquer transacao vinculada a ele.
     */
    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        WishlistItem item = getOwnedWishlistItem(id, user);

        transactionRepository.deleteAllByWishlistItem(item);
        wishlistRepository.delete(item);
    }
}
