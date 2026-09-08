package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.CorporateEventPreviewResponse;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorporateEventServiceTest {
    private final CorporateEventRepository corporateEventRepository = mock(CorporateEventRepository.class);
    private final WalletEarningRepository walletEarningRepository = mock(WalletEarningRepository.class);
    private final InvestmentPositionRepository positionRepository = mock(InvestmentPositionRepository.class);
    private final InvestmentMovementRepository movementRepository = mock(InvestmentMovementRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MarketDataProvider marketDataProvider = mock(MarketDataProvider.class);
    private final CorporateEventPilotAccess pilotAccess = mock(CorporateEventPilotAccess.class);
    private final AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
    private final CorporateEventService service = new CorporateEventService(corporateEventRepository, walletEarningRepository,
            positionRepository, movementRepository, transactionRepository, userRepository, marketDataProvider,
            pilotAccess, authenticatedUserService);

    @Test
    void classifiesEventsAlreadyPublishedToTheAgendaWithoutOfferingThemAgain() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.ACAO).symbol("PETR4").name("Petrobras PN")
                .quantity(BigDecimal.ONE).averagePrice(new BigDecimal("30")).build();
        LocalDate exDate = LocalDate.now().minusDays(1);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(BigDecimal.ONE).eventDate(exDate).build();
        var published = event("b3:petr4:published", exDate);
        var newEvent = new MarketDataProvider.CorporateEventData("b3:petr4:new", "PETR4", "BRPETRACNPR6", CorporateEvent.EventType.DIVIDENDO,
                null, new BigDecimal("0.11"), BigDecimal.ZERO, exDate, exDate.plusDays(5), "B3_EXPERIMENTAL", "VALIDO");

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(published, newEvent));
        CorporateEvent publishedEvent = CorporateEvent.builder().sourceReference(published.sourceReference()).symbol("PETR4")
                .eventType(CorporateEvent.EventType.DIVIDENDO).amountPerUnit(new BigDecimal("0.10"))
                .taxRate(BigDecimal.ZERO).exDate(exDate).paymentDate(exDate.plusDays(5)).source("B3_EXPERIMENTAL").build();
        WalletEarning publishedEarning = WalletEarning.builder().id(9L).user(user).position(position).corporateEvent(publishedEvent)
                .quantityEligible(BigDecimal.ONE).grossAmount(new BigDecimal("0.10")).withheldAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("0.10")).status(WalletEarning.Status.PROVISIONADO).build();
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of(publishedEarning));

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).extracting(CorporateEventPreviewResponse::sourceReference)
                .containsExactly(published.sourceReference(), newEvent.sourceReference());
        assertThat(preview.events()).filteredOn(item -> item.sourceReference().equals(published.sourceReference()))
                .singleElement().satisfies(item -> {
                    assertThat(item.status()).isEqualTo("JA_NA_AGENDA");
                    assertThat(item.walletEarningId()).isEqualTo(9L);
                });
        assertThat(preview.coverage()).singleElement().satisfies(item -> {
            assertThat(item.symbol()).isEqualTo("PETR4");
            assertThat(item.eventCount()).isEqualTo(2);
            assertThat(item.earliestExDate()).isEqualTo(exDate);
            assertThat(item.latestExDate()).isEqualTo(exDate);
        });
    }

    @Test
    void includesEventsSinceTheFirstPositionMovementInsteadOfOnlyTheLastNinetyDays() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.FII).symbol("MXRF11").name("Maxi Renda")
                .quantity(new BigDecimal("100")).averagePrice(new BigDecimal("9.20")).build();
        LocalDate today = LocalDate.now();
        LocalDate purchaseDate = today.minusMonths(8);
        LocalDate exDate = today.minusMonths(5);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(new BigDecimal("100")).eventDate(purchaseDate).build();
        var event = new MarketDataProvider.CorporateEventData("b3:mxrf11:historic", "MXRF11", "BRMXRFCTF008", CorporateEvent.EventType.RENDIMENTO,
                null, new BigDecimal("0.10"), BigDecimal.ZERO, exDate, exDate.plusDays(14), "B3_EXPERIMENTAL", "VALIDO");

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(event));
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of());

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("NOVO");
            assertThat(item.quantityEligible()).isEqualByComparingTo("100");
            assertThat(item.grossAmount()).isEqualByComparingTo("10.00");
            assertThat(item.eligibilityStartDate()).isEqualTo(purchaseDate);
        });
    }

    @Test
    void presentsCancelledEventsAsRestorableInsteadOfSilentlyDiscardingThem() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.FII).symbol("MXRF11").name("Maxi Renda")
                .quantity(new BigDecimal("100")).averagePrice(new BigDecimal("9.20")).build();
        LocalDate exDate = LocalDate.now().minusDays(10);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(new BigDecimal("100")).eventDate(exDate.minusDays(1)).build();
        var event = new MarketDataProvider.CorporateEventData("b3:mxrf11:cancelled", "MXRF11", "BRMXRFCTF008", CorporateEvent.EventType.RENDIMENTO,
                null, new BigDecimal("0.10"), BigDecimal.ZERO, exDate, exDate.plusDays(14), "B3_EXPERIMENTAL", "VALIDO");
        CorporateEvent persistedEvent = CorporateEvent.builder().sourceReference(event.sourceReference()).symbol("MXRF11")
                .eventType(CorporateEvent.EventType.RENDIMENTO).amountPerUnit(new BigDecimal("0.10")).taxRate(BigDecimal.ZERO)
                .exDate(exDate).paymentDate(exDate.plusDays(14)).source("B3_EXPERIMENTAL").build();
        WalletEarning cancelled = WalletEarning.builder().id(15L).user(user).position(position).corporateEvent(persistedEvent)
                .quantityEligible(new BigDecimal("100")).grossAmount(new BigDecimal("10.00")).withheldAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("10.00")).status(WalletEarning.Status.CANCELADO).build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(event));
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of(cancelled));

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("CANCELADO");
            assertThat(item.walletEarningId()).isEqualTo(15L);
            assertThat(item.reason()).contains("Restaure");
        });
    }

    @Test
    void deduplicatesIdenticalMarketEventsBeforeReturningThePreview() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.ACAO).symbol("PETR4").name("Petrobras PN")
                .quantity(BigDecimal.ONE).averagePrice(new BigDecimal("30")).build();
        LocalDate exDate = LocalDate.now().minusDays(1);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(BigDecimal.ONE).eventDate(exDate.minusDays(1)).build();
        var original = event("b3:petr4:original", exDate);
        var duplicate = event("b3:petr4:duplicate", exDate);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(original, duplicate));
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of());

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).singleElement().satisfies(item -> assertThat(item.sourceReference()).isEqualTo(original.sourceReference()));
        assertThat(preview.coverage()).singleElement().satisfies(item -> assertThat(item.eventCount()).isEqualTo(1));
    }

    @Test
    void classifiesAPositionWithoutSharesOnTheRecordDateAsInformational() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.ACAO).symbol("PETR4").name("Petrobras PN")
                .quantity(BigDecimal.ONE).averagePrice(new BigDecimal("30")).build();
        LocalDate exDate = LocalDate.now().minusDays(10);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(BigDecimal.ONE).eventDate(exDate.plusDays(1)).build();
        var event = event("b3:petr4:before-purchase", exDate);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(event));
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of());

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("INELEGIVEL_NA_DATA_COM");
            assertThat(item.reason()).contains("posição ainda não existia");
        });
    }

    @Test
    void exposesAnUnresolvedTickerAsAnAmbiguousActionItem() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        LocalDate exDate = LocalDate.now().minusDays(1);
        var unresolved = new MarketDataProvider.CorporateEventData("b3:bbdc:ambiguous", null, "BRBBDCACNPR8", CorporateEvent.EventType.DIVIDENDO,
                null, new BigDecimal("0.10"), BigDecimal.ZERO, exDate, exDate.plusDays(5), "B3_EXPERIMENTAL", "TICKER_AMBIGUO");

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of());
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(unresolved));
        when(walletEarningRepository.findExistingByUserAndSourceReferences(any(), anySet())).thenReturn(List.of());

        var preview = service.previewCurrentUser();

        assertThat(preview.events()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("AMBIGUO");
            assertThat(item.reason()).contains("não pôde ser vinculada");
        });
    }

    private MarketDataProvider.CorporateEventData event(String sourceReference, LocalDate exDate) {
        return new MarketDataProvider.CorporateEventData(sourceReference, "PETR4", "BRPETRACNPR6", CorporateEvent.EventType.DIVIDENDO,
                null, new BigDecimal("0.10"), BigDecimal.ZERO, exDate, exDate.plusDays(5), "B3_EXPERIMENTAL", "VALIDO");
    }
}
