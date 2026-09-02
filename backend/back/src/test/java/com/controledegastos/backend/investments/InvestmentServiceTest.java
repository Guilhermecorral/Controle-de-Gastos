package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.ProjectionResponse;
import com.controledegastos.backend.investments.InvestmentDtos.TradeRequest;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestmentServiceTest {

    private final InvestmentPositionRepository repository = mock(InvestmentPositionRepository.class);
    private final AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
    private final InvestmentMovementRepository movementRepository = mock(InvestmentMovementRepository.class);
    private final InvestmentService service = new InvestmentService(repository, authenticatedUserService,
            mock(MarketQuoteService.class), mock(AssetCatalogService.class), movementRepository,
            mock(TransactionRepository.class));

    @Test
    void shouldProjectTwelvePercentWithCompoundInterest() {
        ReflectionTestUtils.setField(service, "defaultAnnualRate", new BigDecimal("12.0"));

        ProjectionResponse result = service.projection(
                new BigDecimal("10000"),
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        assertThat(result.months()).isEqualTo(12);
        assertThat(result.projectedBalance()).isEqualByComparingTo("11200.00");
        assertThat(result.projectedEarnings()).isEqualByComparingTo("1200.00");
        assertThat(result.timeline()).hasSize(12);
    }

    @Test
    void shouldRejectInvalidProjectionPeriod() {
        assertThatThrownBy(() -> service.projection(
                new BigDecimal("1000"), new BigDecimal("12"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("posterior");
    }

    @Test
    void shouldRecalculateWeightedAveragePriceIncludingPurchaseFees() {
        User user = User.builder().id(7L).name("Pessoa").email("pessoa@example.com").password("secret").build();
        InvestmentPosition position = InvestmentPosition.builder()
                .id(10L).user(user).assetType(InvestmentPosition.AssetType.ACAO).symbol("BBAS3")
                .externalId("BBAS3.SA").name("Banco do Brasil ON").market("BR").exchange("B3").currency("BRL")
                .quantity(new BigDecimal("10")).averagePrice(new BigDecimal("10")).purchaseDate(LocalDate.of(2026, 1, 2)).build();
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findFirstByUserAndAssetTypeAndMarketAndSymbolIgnoreCase(user, InvestmentPosition.AssetType.ACAO, "BR", "BBAS3"))
                .thenReturn(Optional.of(position));
        when(repository.save(position)).thenReturn(position);
        when(movementRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("5")));

        assertThat(position.getQuantity()).isEqualByComparingTo("15");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("13.666667");
    }

    @Test
    void shouldRejectSaleAboveAvailableQuantity() {
        User user = User.builder().id(7L).name("Pessoa").email("pessoa@example.com").password("secret").build();
        InvestmentPosition position = InvestmentPosition.builder()
                .id(10L).user(user).assetType(InvestmentPosition.AssetType.ACAO).symbol("BBAS3")
                .externalId("BBAS3.SA").name("Banco do Brasil ON").market("BR").exchange("B3").currency("BRL")
                .quantity(new BigDecimal("3")).averagePrice(new BigDecimal("20")).purchaseDate(LocalDate.of(2026, 1, 2)).build();
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findFirstByUserAndAssetTypeAndMarketAndSymbolIgnoreCase(user, InvestmentPosition.AssetType.ACAO, "BR", "BBAS3"))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> service.recordTrade(trade(InvestmentMovement.MovementType.VENDA,
                new BigDecimal("4"), new BigDecimal("21"), BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("excede");
    }

    private TradeRequest trade(InvestmentMovement.MovementType type, BigDecimal quantity, BigDecimal price, BigDecimal fees) {
        return new TradeRequest(null, type, InvestmentPosition.AssetType.ACAO, "BBAS3", "BBAS3.SA",
                "Banco do Brasil ON", "BR", "B3", "BRL", quantity, price, fees, LocalDate.of(2026, 8, 28));
    }
}
