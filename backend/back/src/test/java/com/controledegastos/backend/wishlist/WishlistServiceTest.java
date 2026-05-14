package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.TransactionRepository;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import com.controledegastos.backend.wishlist.dto.WishlistHistoryResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistListRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistListResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistSortBy;
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter;
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WishlistServiceTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistListRepository wishlistListRepository;

    @Autowired
    private WishlistHistoryEntryRepository wishlistHistoryEntryRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionRepository.deleteAll();
        wishlistHistoryEntryRepository.deleteAll();
        wishlistRepository.deleteAll();
        wishlistListRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreatePurchaseUndoAndSummarizeWishlistItemInsideNamedList() {
        User authenticatedUser = authenticateDefaultUser();

        WishlistListResponseDTO technologyList = wishlistService.createList(
                new WishlistListRequestDTO("Tecnologia", "Itens de tecnologia")
        );

        WishlistResponseDTO created = wishlistService.create(new WishlistRequestDTO(
                "Notebook Gamer",
                new BigDecimal("5000.00"),
                new BigDecimal("10.00"),
                WishlistItem.Priority.ALTO,
                WishlistItem.WishlistCategory.COMPRAS,
                "Comprar quando entrar uma promocao",
                technologyList.id()
        ));

        assertEquals(WishlistItem.WishlistStatus.PENDENTE, created.status());
        assertEquals(new BigDecimal("4500.00"), created.finalPrice());
        assertEquals("Tecnologia", created.listName());

        WishlistSummaryDTO summaryBeforePurchase = wishlistService.getSummary();

        assertEquals(1L, summaryBeforePurchase.quantidadeItensDesejados());
        assertEquals(0L, summaryBeforePurchase.quantidadeItensComprados());
        assertEquals(new BigDecimal("4500.00"), summaryBeforePurchase.valorTotalDesejados());
        assertEquals(new BigDecimal("0"), summaryBeforePurchase.valorTotalComprados());

        WishlistResponseDTO purchased = wishlistService.markAsPurchased(created.id(), new WishlistPurchaseRequestDTO(
                LocalDate.of(2026, 7, 19),
                WishlistItem.PurchasePaymentMethod.CARTAO_CREDITO_PARCELADO,
                3,
                false
        ));

        assertEquals(WishlistItem.WishlistStatus.COMPRADO, purchased.status());
        assertEquals(LocalDate.of(2026, 7, 19), purchased.purchaseDate());
        assertEquals(3, purchased.installments());
        assertTrue(purchased.archivedAfterPurchase());

        WishlistItem persistedItem = wishlistRepository.findById(created.id()).orElseThrow();
        List<Transaction> generatedTransactions = transactionRepository.findAllByWishlistItemOrderByTransactionDateAscCreatedAtAsc(persistedItem);

        assertEquals(3, generatedTransactions.size());
        assertEquals(new BigDecimal("1500.00"), generatedTransactions.get(0).getAmount());
        assertEquals(LocalDate.of(2026, 7, 19), generatedTransactions.get(0).getTransactionDate());
        assertEquals(LocalDate.of(2026, 8, 19), generatedTransactions.get(1).getTransactionDate());
        assertEquals(LocalDate.of(2026, 9, 19), generatedTransactions.get(2).getTransactionDate());

        List<WishlistHistoryResponseDTO> historyAfterPurchase = wishlistService.getHistory(created.id());
        assertEquals(2, historyAfterPurchase.size());
        assertEquals(WishlistHistoryEntry.ActionType.PURCHASED, historyAfterPurchase.get(0).actionType());
        assertEquals(WishlistHistoryEntry.ActionType.CREATED, historyAfterPurchase.get(1).actionType());

        List<WishlistResponseDTO> purchasedItems = wishlistService.findAll(
                WishlistStatusFilter.COMPRADO,
                WishlistSortBy.ADICIONADOS_RECENTEMENTE,
                technologyList.id()
        );

        assertEquals(1, purchasedItems.size());
        assertEquals("Notebook Gamer", purchasedItems.get(0).description());

        WishlistResponseDTO undone = wishlistService.undoPurchase(created.id());

        assertEquals(WishlistItem.WishlistStatus.PENDENTE, undone.status());
        assertEquals(1, undone.installments());
        assertFalse(undone.archivedAfterPurchase());
        assertEquals(0, transactionRepository.findAll().size());

        List<WishlistHistoryResponseDTO> historyAfterUndo = wishlistService.getHistory(created.id());
        assertEquals(3, historyAfterUndo.size());
        assertEquals(WishlistHistoryEntry.ActionType.PURCHASE_UNDONE, historyAfterUndo.get(0).actionType());

        List<WishlistResponseDTO> pendingItems = wishlistService.findAll(
                WishlistStatusFilter.PENDENTE,
                WishlistSortBy.PERSONALIZADO,
                technologyList.id()
        );

        assertEquals(1, pendingItems.size());
        assertFalse(pendingItems.get(0).firstInstallmentNextMonth());
        assertEquals(authenticatedUser.getId(), persistedItem.getUser().getId());
    }

    @Test
    void shouldMoveItemsToDefaultListWhenDeletingCustomList() {
        authenticateDefaultUser();

        WishlistListResponseDTO gamesList = wishlistService.createList(
                new WishlistListRequestDTO("Jogos", "Lista de desejos para games")
        );

        WishlistResponseDTO created = wishlistService.create(new WishlistRequestDTO(
                "Console",
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                WishlistItem.Priority.MEDIA,
                WishlistItem.WishlistCategory.LAZER,
                "Comprar no fim do ano",
                gamesList.id()
        ));

        wishlistService.deleteList(gamesList.id());

        List<WishlistListResponseDTO> lists = wishlistService.findAllLists();
        assertEquals(1, lists.size());
        assertTrue(lists.get(0).isDefault());

        List<WishlistResponseDTO> items = wishlistService.findAll(WishlistStatusFilter.TODOS, WishlistSortBy.PERSONALIZADO, null);
        assertEquals(1, items.size());
        assertEquals("Lista Principal", items.get(0).listName());

        List<WishlistHistoryResponseDTO> history = wishlistService.getHistory(created.id());
        assertEquals(WishlistHistoryEntry.ActionType.MOVED, history.get(0).actionType());
    }

    private User authenticateDefaultUser() {
        User authenticatedUser = userRepository.save(User.builder()
                .name("Jorge")
                .email("jorge-wishlist@test.com")
                .password("123456")
                .role(User.Role.USER)
                .build());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities()
        ));

        return authenticatedUser;
    }
}
