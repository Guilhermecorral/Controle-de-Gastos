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
import java.util.Set;

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
    void excludesEventsAlreadyPublishedToTheAgendaFromLaterPreviews() {
        User user = User.builder().id(7L).email("pessoa@example.com").build();
        InvestmentPosition position = InvestmentPosition.builder().id(3L).user(user)
                .assetType(InvestmentPosition.AssetType.ACAO).symbol("PETR4").name("Petrobras PN")
                .quantity(BigDecimal.ONE).averagePrice(new BigDecimal("30")).build();
        LocalDate exDate = LocalDate.now().minusDays(1);
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(BigDecimal.ONE).eventDate(exDate).build();
        var published = event("b3:petr4:published", exDate);
        var newEvent = event("b3:petr4:new", exDate);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        doNothing().when(pilotAccess).require(user);
        when(positionRepository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase));
        when(marketDataProvider.corporateEvents(any(), anySet())).thenReturn(List.of(published, newEvent));
        when(walletEarningRepository.findSourceReferencesAlreadyInAgenda(any(), anySet())).thenReturn(Set.of(published.sourceReference()));

        List<CorporateEventPreviewResponse> preview = service.previewCurrentUser();

        assertThat(preview).extracting(CorporateEventPreviewResponse::sourceReference)
                .containsExactly(newEvent.sourceReference());
    }

    private MarketDataProvider.CorporateEventData event(String sourceReference, LocalDate exDate) {
        return new MarketDataProvider.CorporateEventData(sourceReference, "PETR4", "BRPETRACNPR6", CorporateEvent.EventType.DIVIDENDO,
                null, new BigDecimal("0.10"), BigDecimal.ZERO, exDate, exDate.plusDays(5), "B3_EXPERIMENTAL", "VALIDO");
    }
}
