package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.ProjectionResponse;
import com.controledegastos.backend.investments.InvestmentDtos.RatePeriod;
import com.controledegastos.backend.investments.InvestmentDtos.TimelinePeriod;
import com.controledegastos.backend.investments.InvestmentDtos.TradeRequest;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestmentServiceTest {

    private final InvestmentPositionRepository repository = mock(InvestmentPositionRepository.class);
    private final AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
    private final InvestmentMovementRepository movementRepository = mock(InvestmentMovementRepository.class);
    private final MarketQuoteService marketQuoteService = mock(MarketQuoteService.class);
    private final InvestmentPortfolioSnapshotRepository snapshotRepository = mock(InvestmentPortfolioSnapshotRepository.class);
    private final InvestmentIncomeScheduleRepository incomeScheduleRepository = mock(InvestmentIncomeScheduleRepository.class);
    private final InvestmentService service = new InvestmentService(repository, authenticatedUserService,
            marketQuoteService, mock(AssetCatalogService.class), movementRepository, snapshotRepository,
            mock(TransactionRepository.class), incomeScheduleRepository, mock(InvestmentGoalRepository.class));

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
    void shouldProjectMonthlyContributionsAtTheEndOfEachMonth() {
        ProjectionResponse result = service.projection(
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                new BigDecimal("12"),
                RatePeriod.ANNUAL,
                TimelinePeriod.MONTHLY,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        assertThat(result.totalInvested()).isEqualByComparingTo("12000.00");
        assertThat(result.projectedBalance()).isEqualByComparingTo("12646.50");
        assertThat(result.projectedEarnings()).isEqualByComparingTo("646.50");
        assertThat(result.timeline().get(0).interest()).isEqualByComparingTo("0.00");
        assertThat(result.timeline().get(1).interest()).isEqualByComparingTo("9.49");
    }

    @Test
    void shouldCondenseLongProjectionIntoAnnualRows() {
        ProjectionResponse result = service.projection(
                new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("1"), RatePeriod.MONTHLY,
                TimelinePeriod.YEARLY, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 7, 1)
        );

        assertThat(result.months()).isEqualTo(30);
        assertThat(result.timeline()).extracting(point -> point.month()).containsExactly(12, 24, 30);
        assertThat(result.timeline()).extracting(point -> point.contribution())
                .containsExactly(new BigDecimal("1200.00"), new BigDecimal("1200.00"), new BigDecimal("600.00"));
    }

    @Test
    void shouldSeparateCapitalGainIncomeAndTotalReturn() {
        User user = User.builder().id(7L).name("Pessoa").email("pessoa@example.com").password("secret").build();
        InvestmentPosition position = InvestmentPosition.builder()
                .id(10L).user(user).assetType(InvestmentPosition.AssetType.ACAO).symbol("BBAS3")
                .externalId("BBAS3.SA").name("Banco do Brasil ON").market("BR").exchange("B3").currency("BRL")
                .quantity(new BigDecimal("5")).averagePrice(new BigDecimal("20"))
                .purchaseDate(LocalDate.of(2026, 1, 2)).build();
        InvestmentMovement income = InvestmentMovement.builder().id(1L).user(user).position(position)
                .movementType(InvestmentMovement.MovementType.DIVIDENDO).amount(new BigDecimal("5"))
                .eventDate(LocalDate.of(2026, 8, 1)).build();
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findAllByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(income));
        when(marketQuoteService.exchangeRateToBrl("BRL")).thenReturn(BigDecimal.ONE);
        when(marketQuoteService.quote(InvestmentPosition.AssetType.ACAO, "BBAS3", "BBAS3.SA", "BR"))
                .thenReturn(new InvestmentDtos.QuoteResponse("BBAS3", new BigDecimal("22"), BigDecimal.ZERO,
                        null, "BRL", "TEST", Instant.now(), true));
        when(snapshotRepository.findByUserAndSnapshotDate(user, LocalDate.now())).thenReturn(Optional.empty());
        when(snapshotRepository.findAllByUserAndSnapshotDateGreaterThanEqualOrderBySnapshotDate(user, LocalDate.now().minusMonths(12)))
                .thenReturn(List.of());

        InvestmentDtos.PortfolioResponse result = service.portfolio();

        assertThat(result.totalCapitalGain()).isEqualByComparingTo("10.00");
        assertThat(result.totalIncome()).isEqualByComparingTo("5.00");
        assertThat(result.totalReturn()).isEqualByComparingTo("15.00");
        assertThat(result.totalReturnPercent()).isEqualByComparingTo("15.00");
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

    @Test
    void shouldEstimateScheduledIncomeUsingQuantityOwnedOnExDate() {
        User user = User.builder().id(7L).name("Pessoa").email("pessoa@example.com").password("secret").build();
        InvestmentPosition position = InvestmentPosition.builder()
                .id(10L).user(user).assetType(InvestmentPosition.AssetType.ACAO).symbol("BBAS3")
                .name("Banco do Brasil ON").market("BR").currency("BRL").quantity(new BigDecimal("1"))
                .averagePrice(new BigDecimal("20")).purchaseDate(LocalDate.of(2026, 1, 2)).build();
        InvestmentMovement purchase = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.COMPRA).quantity(new BigDecimal("5"))
                .eventDate(LocalDate.of(2026, 7, 1)).build();
        InvestmentMovement sale = InvestmentMovement.builder().position(position)
                .movementType(InvestmentMovement.MovementType.VENDA).quantity(new BigDecimal("2"))
                .eventDate(LocalDate.of(2026, 8, 1)).build();
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(position));
        when(movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)).thenReturn(List.of(purchase, sale));
        when(incomeScheduleRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentDtos.IncomeScheduleResponse result = service.createIncomeSchedule(new InvestmentDtos.IncomeScheduleRequest(
                10L, InvestmentMovement.MovementType.DIVIDENDO, new BigDecimal("2.00"), new BigDecimal("10"),
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 30)
        ));

        assertThat(result.quantityEligible()).isEqualByComparingTo("3");
        assertThat(result.grossAmount()).isEqualByComparingTo("6.00");
        assertThat(result.taxAmount()).isEqualByComparingTo("0.60");
        assertThat(result.netAmount()).isEqualByComparingTo("5.40");
    }

    private TradeRequest trade(InvestmentMovement.MovementType type, BigDecimal quantity, BigDecimal price, BigDecimal fees) {
        return new TradeRequest(null, type, InvestmentPosition.AssetType.ACAO, "BBAS3", "BBAS3.SA",
                "Banco do Brasil ON", "BR", "B3", "BRL", quantity, price, fees, LocalDate.of(2026, 8, 28));
    }
}
