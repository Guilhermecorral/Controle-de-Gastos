package com.controledegastos.backend.wishlist; // Declares the package for the wishlist business logic.

import com.controledegastos.backend.security.AuthenticatedUserService; // Imports the helper used to resolve the authenticated user.
import com.controledegastos.backend.transactions.Transaction; // Imports the financial transaction entity used when a purchase happens.
import com.controledegastos.backend.transactions.TransactionRepository; // Imports the repository used to persist auto-generated purchase transactions.
import com.controledegastos.backend.user.User; // Imports the authenticated user entity.
import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO; // Imports the DTO used when an item is marked as purchased.
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO; // Imports the DTO used to create or update items.
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO; // Imports the DTO returned by the wishlist endpoints.
import com.controledegastos.backend.wishlist.dto.WishlistSortBy; // Imports the enum used to decide how the list should be ordered.
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter; // Imports the enum used to decide how the list should be filtered.
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO; // Imports the DTO returned by the summary endpoint.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for injected dependencies.
import org.springframework.data.domain.Sort; // Imports Spring's dynamic sort abstraction.
import org.springframework.stereotype.Service; // Marks the class as a Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Marks methods as transactional to keep purchase flows consistent.

import java.math.BigDecimal; // Imports the numeric type used for money-safe calculations.
import java.math.RoundingMode; // Imports the rounding strategy used when splitting parcelled amounts.
import java.time.LocalDate; // Imports the date type used by purchase dates and installment schedules.
import java.util.ArrayList; // Imports the list implementation used when building parcelled transactions.
import java.util.List; // Imports the list abstraction used by service responses.

@Service // Registers the class as a Spring-managed service bean.
@RequiredArgsConstructor // Generates the constructor for the required final dependencies.
public class WishlistService { // Declares the service responsible for the wishlist v1 use cases.

    private final WishlistRepository wishlistRepository; // Stores access to wishlist persistence and summaries.
    private final TransactionRepository transactionRepository; // Stores access to transaction persistence for auto-generated purchase entries.
    private final AuthenticatedUserService authenticatedUserService; // Stores access to the authenticated-user helper shared by the backend.

    private User getAuthenticatedUser() { // Starts the helper that resolves who is making the request.
        return authenticatedUserService.getAuthenticatedUser(); // Delegates the responsibility to the shared authenticated-user helper.
    } // Closes the authenticated-user helper.

    private WishlistItem getOwnedWishlistItem(Long id, User user) { // Starts the helper that loads an item only if it belongs to the authenticated user.
        return wishlistRepository.findByIdAndUser(id, user) // Executes the owner-safe lookup in the repository.
                .orElseThrow(() -> new RuntimeException("Wishlist item not found")); // Throws a simple domain message when the item does not belong to the user.
    } // Closes the owner-safe lookup helper.

    private void validateWishlistRequest(WishlistRequestDTO dto) { // Starts the validation helper for create and update requests.
        if (dto.description() == null || dto.description().isBlank()) { // Checks whether the user forgot to inform the item name.
            throw new IllegalArgumentException("Wishlist item description is required"); // Blocks invalid input with a clear validation message.
        } // Closes the description validation branch.
        if (dto.originalPrice() == null || dto.originalPrice().compareTo(BigDecimal.ZERO) <= 0) { // Checks whether the original price is missing or invalid.
            throw new IllegalArgumentException("Wishlist item original price must be greater than zero"); // Blocks non-positive prices that would break the module logic.
        } // Closes the original-price validation branch.
        if (dto.discountPercent() != null && dto.discountPercent().compareTo(BigDecimal.ZERO) < 0) { // Checks whether the informed discount percentage is negative.
            throw new IllegalArgumentException("Wishlist item discount percent cannot be negative"); // Blocks invalid negative discounts.
        } // Closes the negative-discount validation branch.
        if (dto.discountPercent() != null && dto.discountPercent().compareTo(BigDecimal.valueOf(100)) > 0) { // Checks whether the informed discount percentage exceeds 100%.
            throw new IllegalArgumentException("Wishlist item discount percent cannot be greater than 100"); // Blocks impossible discounts above 100%.
        } // Closes the upper-bound discount validation branch.
    } // Closes the wishlist-request validation helper.

    private void validatePurchaseRequest(WishlistPurchaseRequestDTO dto) { // Starts the validation helper used when a purchase happens.
        if (dto.purchaseDate() == null) { // Checks whether the purchase date was omitted.
            throw new IllegalArgumentException("Purchase date is required"); // Blocks purchases without a real purchase date.
        } // Closes the purchase-date validation branch.
        if (dto.paymentMethod() == null) { // Checks whether the purchase payment method was omitted.
            throw new IllegalArgumentException("Purchase payment method is required"); // Blocks purchases without a payment method.
        } // Closes the payment-method validation branch.
        if (dto.paymentMethod() == WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO) { // Checks whether the purchase is parcelled on the credit card.
            if (dto.installments() == null || dto.installments() < 2) { // Checks whether the installment count is missing or too small for a parcelled purchase.
                throw new IllegalArgumentException("Parcelled purchases must have at least 2 installments"); // Blocks invalid parcelled requests.
            } // Closes the parcelled-installment validation branch.
        } else { // Starts the branch used by one-time payments.
            if (dto.installments() != null && dto.installments() != 1) { // Checks whether the client tried to use multiple installments with a one-time payment method.
                throw new IllegalArgumentException("Only parcelled credit-card purchases can use more than 1 installment"); // Blocks inconsistent combinations of payment method and installment count.
            } // Closes the invalid-one-time-installment validation branch.
        } // Closes the one-time payment branch.
    } // Closes the purchase-request validation helper.

    private WishlistResponseDTO toResponseDTO(WishlistItem item) { // Starts the mapper that converts the entity into the API response DTO.
        return new WishlistResponseDTO( // Builds the immutable response DTO returned to the client.
                item.getId(), // Copies the item id.
                item.getDescription(), // Copies the visible description of the item.
                item.getOriginalPrice(), // Copies the original price.
                item.getDiscountPercent(), // Copies the discount percentage.
                item.getFinalPrice(), // Copies the calculated final price.
                item.getPriority(), // Copies the priority.
                item.getCategory(), // Copies the category.
                item.getNotes(), // Copies the optional notes.
                item.getStatus(), // Copies the current wishlist status.
                item.getPurchaseDate(), // Copies the purchase date when it exists.
                item.getPaymentMethod(), // Copies the payment method when it exists.
                item.getInstallments(), // Copies the installment count.
                item.getFirstInstallmentNextMonth(), // Copies the flag that controls when the first installment starts.
                item.getCreatedAt(), // Copies the creation timestamp.
                item.getUpdatedAt() // Copies the update timestamp.
        ); // Finishes the response DTO creation.
    } // Closes the entity-to-response mapper.

    private Sort buildSort(WishlistSortBy sortBy) { // Starts the helper that translates the API sort option into a Spring Sort object.
        WishlistSortBy effectiveSort = sortBy == null ? WishlistSortBy.PERSONALIZADO : sortBy; // Applies the custom default strategy when the client omits the sorting option.
        return switch (effectiveSort) { // Selects the sort strategy requested by the client.
            case MENOR_PRECO -> Sort.by(Sort.Order.asc("finalPrice"), Sort.Order.desc("createdAt")); // Sorts by lower final price first.
            case MAIOR_PRECO -> Sort.by(Sort.Order.desc("finalPrice"), Sort.Order.desc("createdAt")); // Sorts by higher final price first.
            case PRIORIDADE -> Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("createdAt")); // Sorts by enum order where ALTO comes before MEDIA and BAIXO.
            case ADICIONADOS_RECENTEMENTE -> Sort.by(Sort.Order.desc("createdAt")); // Sorts by the most recently created items first.
            case PERSONALIZADO -> Sort.by( // Applies the product's custom v1 sorting strategy.
                    Sort.Order.asc("status"), // Keeps pending items naturally before purchased items when needed.
                    Sort.Order.asc("priority"), // Shows higher-priority items first.
                    Sort.Order.asc("finalPrice"), // Shows the cheaper item first when priorities tie.
                    Sort.Order.desc("createdAt") // Uses recent creation as the last tie-breaker.
            ); // Finishes the custom sort definition.
        }; // Closes the switch expression.
    } // Closes the sort builder helper.

    private List<WishlistResponseDTO> mapList(List<WishlistItem> items) { // Starts the helper that maps entity lists into response DTO lists.
        return items.stream() // Opens a stream over the loaded wishlist entities.
                .map(this::toResponseDTO) // Converts each entity into a response DTO.
                .toList(); // Materializes the mapped list.
    } // Closes the list-mapping helper.

    private Transaction.TransactionCategory mapCategory(WishlistItem.WishlistCategory category) { // Starts the helper that maps wishlist categories into transaction categories.
        return Transaction.TransactionCategory.valueOf(category.name()); // Reuses the shared enum names to keep both modules aligned.
    } // Closes the category-mapping helper.

    private Transaction.PaymentMethod mapPaymentMethod(WishlistItem.PurchasePaymentMethod paymentMethod) { // Starts the helper that maps wishlist payment methods into transaction payment methods.
        return Transaction.PaymentMethod.valueOf(paymentMethod.name()); // Reuses the shared enum names to keep both modules aligned.
    } // Closes the payment-method-mapping helper.

    private List<Transaction> buildPurchaseTransactions(WishlistItem item) { // Starts the helper that creates the financial transactions generated by a wishlist purchase.
        List<Transaction> generatedTransactions = new ArrayList<>(); // Creates the mutable list that will store the generated financial entries.
        int installments = item.getInstallments() == null ? 1 : item.getInstallments(); // Resolves the effective installment count using 1 as the default.
        LocalDate baseDate = Boolean.TRUE.equals(item.getFirstInstallmentNextMonth()) // Checks whether the first installment must start only in the next month.
                ? item.getPurchaseDate().plusMonths(1) // Moves the first installment one month forward when the card bill closes after the purchase.
                : item.getPurchaseDate(); // Keeps the purchase month as the base month when the installment starts immediately.
        if (item.getPaymentMethod() != WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO) { // Checks whether the purchase is a one-time payment.
            generatedTransactions.add(Transaction.builder() // Creates the single financial transaction generated by the purchase.
                    .user(item.getUser()) // Associates the transaction with the same authenticated user who owns the wishlist item.
                    .wishlistItem(item) // Links the transaction back to the wishlist item so undo purchase can delete it safely.
                    .type(Transaction.TransactionType.DESPESA) // Marks the generated entry as an expense.
                    .description(item.getDescription()) // Reuses the wishlist description as the expense description.
                    .category(mapCategory(item.getCategory())) // Maps the wishlist category into the transaction module category.
                    .amount(item.getFinalPrice()) // Uses the final price as the expense amount for one-time payments.
                    .paymentMethod(mapPaymentMethod(item.getPaymentMethod())) // Maps the purchase payment method into the transaction module enum.
                    .transactionDate(baseDate) // Uses the purchase date as the business date of the expense.
                    .build()); // Finishes the one-time transaction creation.
            return generatedTransactions; // Returns immediately because one-time payments generate only one transaction.
        } // Closes the one-time-payment branch.
        BigDecimal installmentsAsBigDecimal = BigDecimal.valueOf(installments); // Converts the installment count into BigDecimal for exact division.
        BigDecimal baseInstallmentAmount = item.getFinalPrice() // Starts the installment value calculation from the total final price.
                .divide(installmentsAsBigDecimal, 2, RoundingMode.DOWN); // Calculates the base installment value rounded down to two decimals.
        BigDecimal accumulatedAmount = BigDecimal.ZERO; // Starts the accumulator used to keep the final sum exact even after rounding.
        for (int installmentNumber = 1; installmentNumber <= installments; installmentNumber++) { // Iterates once for each parcel that must become a transaction.
            LocalDate installmentDate = baseDate.plusMonths((long) installmentNumber - 1L); // Places each parcel in its own month relative to the chosen base month.
            BigDecimal installmentAmount = installmentNumber == installments // Checks whether the loop is building the last installment of the purchase.
                    ? item.getFinalPrice().subtract(accumulatedAmount) // Uses the remainder on the last installment so the total sum matches the final price exactly.
                    : baseInstallmentAmount; // Uses the standard rounded-down amount for every earlier installment.
            accumulatedAmount = accumulatedAmount.add(installmentAmount); // Adds the generated installment amount to the running total used by the remainder logic.
            generatedTransactions.add(Transaction.builder() // Creates one financial transaction for the current parcel.
                    .user(item.getUser()) // Associates the transaction with the same authenticated user who owns the wishlist item.
                    .wishlistItem(item) // Links the transaction back to the wishlist item so undo purchase can delete it safely.
                    .type(Transaction.TransactionType.DESPESA) // Marks the generated entry as an expense.
                    .description(item.getDescription() + " - Parcela " + installmentNumber + "/" + installments) // Makes the generated expense explicit for the user and future audits.
                    .category(mapCategory(item.getCategory())) // Maps the wishlist category into the transaction module category.
                    .amount(installmentAmount) // Uses the calculated installment value as the amount of the generated expense.
                    .paymentMethod(Transaction.PaymentMethod.CARTAO_CREDITO_PARCELADO) // Persists the explicit parcelled-credit-card payment method.
                    .transactionDate(installmentDate) // Saves the month where the current installment should impact the financial module.
                    .build()); // Finishes the installment transaction creation.
        } // Closes the installment generation loop.
        return generatedTransactions; // Returns all generated installment transactions.
    } // Closes the purchase-transaction builder helper.

    @Transactional // Wraps the create flow in a transaction so persistence stays consistent.
    public WishlistResponseDTO create(WishlistRequestDTO dto) { // Starts the create use case of the wishlist module.
        validateWishlistRequest(dto); // Validates the user input before touching the database.
        User user = getAuthenticatedUser(); // Resolves which user owns the item being created.
        WishlistItem item = WishlistItem.builder() // Starts the entity creation using the builder for readability.
                .description(dto.description()) // Copies the visible description informed by the user.
                .originalPrice(dto.originalPrice()) // Copies the original price informed by the user.
                .discountPercent(dto.discountPercent() == null ? BigDecimal.ZERO : dto.discountPercent()) // Applies zero as the default discount when the field is omitted.
                .priority(dto.priority() == null ? WishlistItem.Priority.MEDIA : dto.priority()) // Applies the medium priority as the default when the field is omitted.
                .category(dto.category() == null ? WishlistItem.WishlistCategory.COMPRAS : dto.category()) // Applies the general shopping category as the default when the field is omitted.
                .notes(dto.notes()) // Copies the optional notes informed by the user.
                .status(WishlistItem.WishlistStatus.PENDENTE) // Forces the new item to start as pending.
                .installments(1) // Initializes the installment count to the simplest one-time default.
                .firstInstallmentNextMonth(false) // Initializes the next-month flag to false for new pending items.
                .user(user) // Associates the new item with the authenticated user.
                .build(); // Finishes the entity construction.
        item.calculateFinalPrice(); // Calculates the final price before persisting the item.
        WishlistItem saved = wishlistRepository.save(item); // Persists the new wishlist item.
        return toResponseDTO(saved); // Returns the created item as a response DTO.
    } // Closes the create use case.

    @Transactional(readOnly = true) // Marks the list flow as read-only because it does not change state.
    public List<WishlistResponseDTO> findAll(WishlistStatusFilter statusFilter, WishlistSortBy sortBy) { // Starts the list use case with filters and sorting.
        User user = getAuthenticatedUser(); // Resolves which user owns the list being requested.
        Sort sort = buildSort(sortBy); // Builds the sorting strategy requested by the client.
        WishlistStatusFilter effectiveFilter = statusFilter == null ? WishlistStatusFilter.TODOS : statusFilter; // Uses the all-items filter when the client omits it.
        List<WishlistItem> items = switch (effectiveFilter) { // Selects the repository query that matches the requested filter.
            case PENDENTE -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE, sort); // Loads only pending items.
            case COMPRADO -> wishlistRepository.findAllByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO, sort); // Loads only purchased items.
            case TODOS -> wishlistRepository.findAllByUser(user, sort); // Loads every item regardless of status.
        }; // Closes the switch expression.
        return mapList(items); // Maps the loaded entities into response DTOs.
    } // Closes the list use case.

    @Transactional(readOnly = true) // Marks the summary flow as read-only because it does not change state.
    public WishlistSummaryDTO getSummary() { // Starts the summary use case of the wishlist module.
        User user = getAuthenticatedUser(); // Resolves which user owns the summary being requested.
        long quantidadeItensDesejados = wishlistRepository.countByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE); // Counts the user's pending items.
        long quantidadeItensComprados = wishlistRepository.countByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO); // Counts the user's purchased items.
        BigDecimal valorTotalDesejados = wishlistRepository.sumFinalPriceByUserAndStatus(user, WishlistItem.WishlistStatus.PENDENTE); // Sums the final prices of the user's pending items.
        BigDecimal valorTotalComprados = wishlistRepository.sumFinalPriceByUserAndStatus(user, WishlistItem.WishlistStatus.COMPRADO); // Sums the final prices of the user's purchased items.
        return new WishlistSummaryDTO( // Builds the summary DTO returned to the client.
                quantidadeItensDesejados, // Includes the count of pending items.
                quantidadeItensComprados, // Includes the count of purchased items.
                valorTotalDesejados, // Includes the total value of pending items.
                valorTotalComprados // Includes the total value of purchased items.
        ); // Finishes the summary DTO creation.
    } // Closes the summary use case.

    @Transactional // Wraps the update flow in a transaction so the item change is persisted atomically.
    public WishlistResponseDTO update(Long id, WishlistRequestDTO dto) { // Starts the update use case of the wishlist module.
        validateWishlistRequest(dto); // Validates the update payload before touching the database.
        User user = getAuthenticatedUser(); // Resolves which user owns the item being edited.
        WishlistItem item = getOwnedWishlistItem(id, user); // Loads the item only if it belongs to the authenticated user.
        if (item.getStatus() == WishlistItem.WishlistStatus.COMPRADO) { // Checks whether the user is trying to edit a purchased item with the generic edit flow.
            throw new IllegalArgumentException("Purchased wishlist items cannot be edited through the generic update flow"); // Forces the user to undo purchase before changing core item data.
        } // Closes the purchased-item validation branch.
        item.setDescription(dto.description()); // Updates the visible description of the item.
        item.setOriginalPrice(dto.originalPrice()); // Updates the original price of the item.
        item.setDiscountPercent(dto.discountPercent() == null ? BigDecimal.ZERO : dto.discountPercent()); // Updates the discount percentage using zero as the default.
        item.setPriority(dto.priority() == null ? WishlistItem.Priority.MEDIA : dto.priority()); // Updates the priority using the default medium value when omitted.
        item.setCategory(dto.category() == null ? WishlistItem.WishlistCategory.COMPRAS : dto.category()); // Updates the category using the default shopping category when omitted.
        item.setNotes(dto.notes()); // Updates the optional notes of the item.
        item.calculateFinalPrice(); // Recalculates the final price after price and discount changes.
        WishlistItem updated = wishlistRepository.save(item); // Persists the updated entity.
        return toResponseDTO(updated); // Returns the updated item as a response DTO.
    } // Closes the update use case.

    @Transactional // Wraps the purchase flow in a transaction so the wishlist item and generated transactions stay consistent.
    public WishlistResponseDTO markAsPurchased(Long id, WishlistPurchaseRequestDTO dto) { // Starts the use case that marks a pending item as purchased.
        validatePurchaseRequest(dto); // Validates the purchase payload before touching the database.
        User user = getAuthenticatedUser(); // Resolves which user owns the item being purchased.
        WishlistItem item = getOwnedWishlistItem(id, user); // Loads the item only if it belongs to the authenticated user.
        if (item.getStatus() == WishlistItem.WishlistStatus.COMPRADO) { // Checks whether the item has already been purchased before.
            throw new IllegalArgumentException("Wishlist item is already marked as purchased"); // Blocks duplicate purchase actions on the same item.
        } // Closes the already-purchased validation branch.
        item.setStatus(WishlistItem.WishlistStatus.COMPRADO); // Moves the item from the desired list to the purchased list.
        item.setPurchaseDate(dto.purchaseDate()); // Stores when the purchase actually happened.
        item.setPaymentMethod(dto.paymentMethod()); // Stores the payment method used by the purchase.
        item.setInstallments(dto.installments() == null ? 1 : dto.installments()); // Stores the installment count using one-time payment as the default.
        item.setFirstInstallmentNextMonth(Boolean.TRUE.equals(dto.firstInstallmentNextMonth())); // Stores whether the first parcel starts only next month.
        WishlistItem savedItem = wishlistRepository.save(item); // Persists the purchased state before generating linked transactions.
        List<Transaction> generatedTransactions = buildPurchaseTransactions(savedItem); // Builds every financial transaction generated by this purchase.
        transactionRepository.saveAll(generatedTransactions); // Persists the generated expense transactions in one batch.
        return toResponseDTO(savedItem); // Returns the purchased item as a response DTO.
    } // Closes the purchase use case.

    @Transactional // Wraps the undo flow in a transaction so the item and linked transactions revert together.
    public WishlistResponseDTO undoPurchase(Long id) { // Starts the use case that moves a purchased item back to pending.
        User user = getAuthenticatedUser(); // Resolves which user owns the item being reverted.
        WishlistItem item = getOwnedWishlistItem(id, user); // Loads the item only if it belongs to the authenticated user.
        if (item.getStatus() != WishlistItem.WishlistStatus.COMPRADO) { // Checks whether the item is really purchased before trying to undo it.
            throw new IllegalArgumentException("Only purchased wishlist items can undo the purchase"); // Blocks invalid undo actions on pending items.
        } // Closes the invalid-undo validation branch.
        transactionRepository.deleteAllByWishlistItem(item); // Deletes every transaction generated by this wishlist purchase.
        item.setStatus(WishlistItem.WishlistStatus.PENDENTE); // Moves the item back to the desired-items list.
        item.setPurchaseDate(null); // Clears the purchase date because the purchase no longer exists.
        item.setPaymentMethod(null); // Clears the payment method because the purchase no longer exists.
        item.setInstallments(1); // Resets the installment count to the default pending-item value.
        item.setFirstInstallmentNextMonth(false); // Resets the next-month flag to the default pending-item value.
        WishlistItem savedItem = wishlistRepository.save(item); // Persists the reverted pending state of the item.
        return toResponseDTO(savedItem); // Returns the reverted item as a response DTO.
    } // Closes the undo-purchase use case.

    @Transactional // Wraps the delete flow in a transaction so linked transactions are cleaned safely when necessary.
    public void delete(Long id) { // Starts the delete use case of the wishlist module.
        User user = getAuthenticatedUser(); // Resolves which user owns the item being deleted.
        WishlistItem item = getOwnedWishlistItem(id, user); // Loads the item only if it belongs to the authenticated user.
        transactionRepository.deleteAllByWishlistItem(item); // Deletes linked auto-generated transactions first when they exist.
        wishlistRepository.delete(item); // Deletes the wishlist item after the dependent transactions are gone.
    } // Closes the delete use case.
} // Closes the service class.
