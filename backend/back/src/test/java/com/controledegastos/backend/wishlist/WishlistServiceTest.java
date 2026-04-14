package com.controledegastos.backend.wishlist; // Declares the package for wishlist integration tests.

import com.controledegastos.backend.transactions.Transaction; // Imports the transaction entity used to verify generated purchase entries.
import com.controledegastos.backend.transactions.TransactionRepository; // Imports the repository used to inspect generated transactions.
import com.controledegastos.backend.user.User; // Imports the user entity used in the authenticated context.
import com.controledegastos.backend.user.UserRepository; // Imports the repository used to create test users.
import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO; // Imports the DTO used to mark an item as purchased.
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO; // Imports the DTO used to create wishlist items.
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO; // Imports the response DTO asserted by the tests.
import com.controledegastos.backend.wishlist.dto.WishlistSortBy; // Imports the sorting enum used by the list endpoint.
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter; // Imports the filter enum used by the list endpoint.
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO; // Imports the summary DTO asserted by the tests.
import org.junit.jupiter.api.AfterEach; // Imports the cleanup hook annotation.
import org.junit.jupiter.api.Test; // Imports the JUnit test annotation.
import org.springframework.beans.factory.annotation.Autowired; // Imports dependency injection for test beans.
import org.springframework.boot.test.context.SpringBootTest; // Boots the full Spring Boot application context.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Imports the authentication token used to seed the security context.
import org.springframework.security.core.context.SecurityContextHolder; // Imports access to the current security context.
import org.springframework.test.context.ActiveProfiles; // Activates the isolated test profile.

import java.math.BigDecimal; // Imports the exact numeric type used for money assertions.
import java.time.LocalDate; // Imports the date type used by the purchase flow.
import java.util.List; // Imports the list abstraction used by assertions.

import static org.junit.jupiter.api.Assertions.assertEquals; // Imports the equality assertion.
import static org.junit.jupiter.api.Assertions.assertFalse; // Imports the boolean-false assertion.

@SpringBootTest // Boots the full application so the wishlist module runs with real wiring.
@ActiveProfiles("test") // Forces the test to use the isolated test profile.
class WishlistServiceTest { // Declares the integration test for the wishlist v1 use cases.

    @Autowired // Injects the wishlist service from the application context.
    private WishlistService wishlistService; // Stores the service under test.

    @Autowired // Injects the user repository used to create the authenticated user.
    private UserRepository userRepository; // Stores the user repository used by the test setup.

    @Autowired // Injects the transaction repository used to inspect generated purchase entries.
    private TransactionRepository transactionRepository; // Stores the transaction repository used by the assertions.

    @Autowired // Injects the wishlist repository used for cleanup and direct verification.
    private WishlistRepository wishlistRepository; // Stores the wishlist repository used by the test setup.

    @AfterEach // Runs after each test so state does not leak between executions.
    void tearDown() { // Starts the cleanup hook.
        SecurityContextHolder.clearContext(); // Clears the authentication created during the test.
        transactionRepository.deleteAll(); // Deletes transactions first to respect foreign key relationships.
        wishlistRepository.deleteAll(); // Deletes wishlist items after dependent transactions are gone.
        userRepository.deleteAll(); // Deletes users after the dependent data is gone.
    } // Closes the cleanup hook.

    @Test // Marks the method as a test case.
    void shouldCreatePurchaseUndoAndSummarizeWishlistItem() { // Starts the main wishlist v1 scenario.
        User authenticatedUser = userRepository.save(User.builder() // Creates and persists the authenticated user.
                .name("Jorge") // Sets the display name used in the test data.
                .email("jorge-wishlist@test.com") // Sets the unique email of the authenticated user.
                .password("123456") // Sets a simple password because hashing is not relevant in this integration test.
                .role(User.Role.USER) // Sets the role required by the entity.
                .build()); // Finishes and saves the user.

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken( // Seeds the security context like the JWT filter would do in production.
                authenticatedUser, // Stores the authenticated user as the principal.
                null, // Omits credentials because authentication already happened.
                authenticatedUser.getAuthorities() // Reuses the authorities exposed by the user entity.
        )); // Finishes the authentication setup.

        WishlistResponseDTO created = wishlistService.create(new WishlistRequestDTO( // Creates a pending wishlist item through the service.
                "Notebook Gamer", // Sets the visible description of the wishlist item.
                new BigDecimal("5000.00"), // Sets the original price informed by the user.
                new BigDecimal("10.00"), // Sets the discount percentage applied to the wishlist item.
                WishlistItem.Priority.ALTO, // Sets the item as high priority.
                WishlistItem.WishlistCategory.COMPRAS, // Sets the category that matches the financial domain.
                "Comprar quando entrar uma promocao" // Sets an optional note for the wishlist item.
        )); // Finishes the create request.

        assertEquals(WishlistItem.WishlistStatus.PENDENTE, created.status()); // Confirms the item starts in the pending list.
        assertEquals(new BigDecimal("4500.00"), created.finalPrice()); // Confirms the final price is calculated from the discount.

        WishlistSummaryDTO summaryBeforePurchase = wishlistService.getSummary(); // Loads the wishlist summary before the purchase happens.

        assertEquals(1L, summaryBeforePurchase.quantidadeItensDesejados()); // Confirms the pending-item count is correct before purchase.
        assertEquals(0L, summaryBeforePurchase.quantidadeItensComprados()); // Confirms the purchased-item count is zero before purchase.
        assertEquals(new BigDecimal("4500.00"), summaryBeforePurchase.valorTotalDesejados()); // Confirms the pending total uses the final price.
        assertEquals(new BigDecimal("0"), summaryBeforePurchase.valorTotalComprados()); // Confirms the purchased total is zero before purchase.

        WishlistResponseDTO purchased = wishlistService.markAsPurchased(created.id(), new WishlistPurchaseRequestDTO( // Marks the wishlist item as purchased and generates parcelled transactions.
                LocalDate.of(2026, 7, 19), // Sets the purchase date used as the basis of the installment schedule.
                WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO, // Chooses the parcelled credit-card method.
                3, // Splits the purchase into three installments.
                false // Keeps the first installment in the same month of the purchase.
        )); // Finishes the purchase request.

        assertEquals(WishlistItem.WishlistStatus.COMPRADO, purchased.status()); // Confirms the item moved to the purchased list.
        assertEquals(LocalDate.of(2026, 7, 19), purchased.purchaseDate()); // Confirms the purchase date is stored correctly.
        assertEquals(3, purchased.installments()); // Confirms the installment count is stored correctly.

        List<Transaction> generatedTransactions = transactionRepository.findAllByWishlistItemOrderByTransactionDateAscCreatedAtAsc( // Loads the financial entries created from the wishlist purchase.
                wishlistRepository.findById(created.id()).orElseThrow() // Loads the persisted wishlist entity referenced by the generated transactions.
        ); // Finishes the generated-transaction lookup.

        assertEquals(3, generatedTransactions.size()); // Confirms one transaction was created for each installment.
        assertEquals(new BigDecimal("1500.00"), generatedTransactions.get(0).getAmount()); // Confirms the first installment amount is correct.
        assertEquals(LocalDate.of(2026, 7, 19), generatedTransactions.get(0).getTransactionDate()); // Confirms the first installment stays in the purchase month.
        assertEquals(LocalDate.of(2026, 8, 19), generatedTransactions.get(1).getTransactionDate()); // Confirms the second installment moves to the next month.
        assertEquals(LocalDate.of(2026, 9, 19), generatedTransactions.get(2).getTransactionDate()); // Confirms the third installment moves to the month after that.

        WishlistSummaryDTO summaryAfterPurchase = wishlistService.getSummary(); // Loads the wishlist summary after the purchase happens.

        assertEquals(0L, summaryAfterPurchase.quantidadeItensDesejados()); // Confirms the item no longer appears in the desired-items count.
        assertEquals(1L, summaryAfterPurchase.quantidadeItensComprados()); // Confirms the purchased-item count increased after the purchase.
        assertEquals(new BigDecimal("0"), summaryAfterPurchase.valorTotalDesejados()); // Confirms the pending total becomes zero after the purchase.
        assertEquals(new BigDecimal("4500.00"), summaryAfterPurchase.valorTotalComprados()); // Confirms the purchased total uses the final price of the item.

        List<WishlistResponseDTO> purchasedItems = wishlistService.findAll(WishlistStatusFilter.COMPRADO, WishlistSortBy.ADICIONADOS_RECENTEMENTE); // Loads only purchased items through the filter endpoint logic.

        assertEquals(1, purchasedItems.size()); // Confirms the purchased filter returns the bought item.
        assertEquals("Notebook Gamer", purchasedItems.get(0).description()); // Confirms the purchased item is the same one created earlier.

        WishlistResponseDTO undone = wishlistService.undoPurchase(created.id()); // Reverts the purchase and deletes the generated transactions.

        assertEquals(WishlistItem.WishlistStatus.PENDENTE, undone.status()); // Confirms the item returns to the pending list after undoing the purchase.
        assertEquals(1, undone.installments()); // Confirms the installment count resets to the default pending-item value.
        assertEquals(0, transactionRepository.findAll().size()); // Confirms every generated purchase transaction is removed by the undo flow.

        List<WishlistResponseDTO> pendingItems = wishlistService.findAll(WishlistStatusFilter.PENDENTE, WishlistSortBy.PERSONALIZADO); // Loads only pending items through the filter endpoint logic.

        assertEquals(1, pendingItems.size()); // Confirms the item appears again in the pending list after undo purchase.
        assertFalse(pendingItems.get(0).firstInstallmentNextMonth()); // Confirms the next-month flag resets to false after undo purchase.
    } // Closes the main wishlist v1 scenario.
} // Closes the test class.
