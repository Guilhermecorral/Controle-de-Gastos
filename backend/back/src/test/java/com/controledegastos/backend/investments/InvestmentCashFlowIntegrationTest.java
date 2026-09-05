package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.TransactionService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.monthlyanalysis.MonthlyAnalysisService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.controledegastos.backend.investments.InvestmentDtos.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class InvestmentCashFlowIntegrationTest {
    @Autowired InvestmentService investments;
    @Autowired InvestmentTaxController tax;
    @Autowired UserRepository users;
    @Autowired TransactionRepository transactions;
    @Autowired TransactionService financial;
    @Autowired MonthlyAnalysisService monthly;
    @MockitoBean MarketQuoteService quotes;
    private User user;
    private BigDecimal n(String value) { return new BigDecimal(value); }
    @BeforeEach void setup() {
        user = users.save(User.builder().name("Investment test").email("invest-test@example.com").password("unused").role(User.Role.USER).build());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(quotes.exchangeRateToBrl(any())).thenReturn(BigDecimal.ONE);
        when(quotes.quote(any(), any(), any(), any())).thenReturn(new QuoteResponse("BBAS3", n("20"), null, null, "BRL", "TEST", java.time.Instant.now(), true));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private TradeRequest trade(InvestmentMovement.MovementType type, String price, LocalDate date) {
        return new TradeRequest(null, type, InvestmentPosition.AssetType.ACAO, "BBAS3", "BBAS3.SA", "Banco do Brasil", "BR", "B3", "BRL",
                n("2"), n(price), BigDecimal.ZERO, date, new OperationCosts(n("0.10"), n("0.02"), BigDecimal.ZERO, type == InvestmentMovement.MovementType.VENDA ? n("0.01") : BigDecimal.ZERO), null);
    }
    @Test void buyAndSellCreateLinkedCashFlowAndKeepActualGainSeparate() {
        investments.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, "19.50", LocalDate.of(2026, 1, 2)));
        var sale = investments.recordTrade(trade(InvestmentMovement.MovementType.VENDA, "19.70", LocalDate.of(2026, 1, 3)));
        var entries = transactions.findAllByUserOrderByTransactionDateDesc(user);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getAmount()).isEqualByComparingTo("39.27");
        assertThat(entries.get(1).getAmount()).isEqualByComparingTo("39.12");
        assertThat(sale.realizedGain()).isEqualByComparingTo("0.16");
        assertThat(entries).allMatch(t -> t.getCategory() == Transaction.TransactionCategory.INVESTIMENTO && t.getInvestmentMovementId() != null);
        assertThatThrownBy(() -> financial.delete(entries.get(0).getId())).isInstanceOf(IllegalArgumentException.class);
        var analysis = monthly.getMonthlyAnalysis(2026, 1);
        assertThat(analysis.maiorGasto()).isNull();
        assertThat(analysis.gastosPorCategoria()).isEmpty();
        assertThat(investments.reconciliation(2026).items()).hasSize(2).allSatisfy(item ->
                assertThat(item.status()).isEqualTo(ReconciliationStatus.GERADO_PELO_FAROL));
        assertThat(investments.taxSummary(2026).totalWithheld()).isEqualByComparingTo("0.01");
    }
    @Test void openingBalanceDoesNotSpendCashAndRedemptionDoes() {
        var position = investments.create(new PositionRequest(InvestmentPosition.AssetType.RENDA_FIXA, null, null, "CDB antigo", null, null, n("1000"), n("12"),
                LocalDate.of(2025,1,1), LocalDate.of(2027,1,1), "BR", "B3", "BRL", FixedIncomeTax.Regime.REGRESSIVO, null, true, LocalDate.of(2026,1,1)));
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).isEmpty();
        var result = investments.redeem(position.id(), new RedemptionRequest(LocalDate.of(2026,1,2), n("1127"), FixedIncomeTax.Regime.REGRESSIVO, null, true), true);
        assertThat(result.incomeTax()).isEqualByComparingTo("22.23");
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(t -> assertThat(t.getAmount()).isEqualByComparingTo("1104.77"));
        assertThat(investments.taxSummary(2026).totalWithheld()).isEqualByComparingTo("22.23");
        assertThatThrownBy(() -> investments.redeem(position.id(), new RedemptionRequest(LocalDate.of(2026,1,2), n("1127"), FixedIncomeTax.Regime.REGRESSIVO, null, true), true)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void taxEstimateDoesNotSpendCashUntilPaymentAndPreventsDuplicatePayment() {
        tax.opening(new InvestmentTaxController.OpeningRequest(LocalDate.of(2026,1,1), n("500"), n("0"), n("0"), n("0"), n("0"), n("0"), "Apuracao anterior"));
        tax.overview();
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).isEmpty();
        var request = new InvestmentTaxController.PaymentRequest("2026-01", "6015", n("45"), LocalDate.of(2026,2,20), LocalDate.of(2026,2,27), "Conta teste", "Comprovante confirmado");
        tax.pay(request);
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(t -> {
            assertThat(t.getCategory()).isEqualTo(Transaction.TransactionCategory.IMPOSTOS);
            assertThat(t.getAmount()).isEqualByComparingTo("45");
        });
        assertThatThrownBy(() -> tax.pay(request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tax.overview().months()).filteredOn(m -> m.period().equals("2026-01")).singleElement().satisfies(m -> {
            assertThat(m.review()).isTrue();
            assertThat(m.estimatedDue()).isNull();
        });
    }

    @Test void repeatedTradeCreatesOnlyOneCashEntryAndRejectsChangedPayload() {
        String key = java.util.UUID.randomUUID().toString();
        var request = new TradeRequest(null, InvestmentMovement.MovementType.COMPRA, InvestmentPosition.AssetType.ACAO,
                "BBAS3", "BBAS3.SA", "Banco do Brasil", "BR", "B3", "BRL", n("2"), n("19.50"), n("0"),
                LocalDate.of(2026, 1, 2), null, null, key);
        var first = investments.recordTrade(request);
        assertThat(investments.recordTrade(request).id()).isEqualTo(first.id());
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).hasSize(1);
        var changed = new TradeRequest(null, request.movementType(), request.assetType(), request.symbol(), request.externalId(),
                request.name(), request.market(), request.exchange(), request.currency(), n("3"), request.unitPrice(),
                request.fees(), request.eventDate(), null, null, key);
        assertThatThrownBy(() -> investments.recordTrade(changed)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void newFixedIncomeApplicationCreatesExpense() {
        investments.create(new PositionRequest(InvestmentPosition.AssetType.RENDA_FIXA, null, null, "CDB novo", null, null, n("1000"), n("12"),
                LocalDate.of(2026,1,2), LocalDate.of(2027,1,1), "BR", "B3", "BRL", FixedIncomeTax.Regime.REGRESSIVO, null, true, null));
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(t -> {
            assertThat(t.getAmount()).isEqualByComparingTo("1000");
            assertThat(t.getType()).isEqualTo(Transaction.TransactionType.DESPESA);
            assertThat(t.getInvestmentMovementId()).isNotNull();
        });
    }

    @Test void foreignTradeUsesHistoricalExchangeRateInCashAndReconciliation() {
        investments.recordTrade(new TradeRequest(null, InvestmentMovement.MovementType.COMPRA, InvestmentPosition.AssetType.ACAO,
                "AAPL", "AAPL", "Apple", "US", "NASDAQ", "USD", n("2"), n("100"), n("0"),
                LocalDate.of(2026,1,2), null, n("5.20")));
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(t ->
                assertThat(t.getAmount()).isEqualByComparingTo("1040"));
        assertThat(investments.reconciliation(2026).items()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo(ReconciliationStatus.GERADO_PELO_FAROL);
            assertThat(item.currency()).isEqualTo("BRL");
        });
        investments.recordTrade(new TradeRequest(null, InvestmentMovement.MovementType.VENDA, InvestmentPosition.AssetType.ACAO,
                "AAPL", "AAPL", "Apple", "US", "NASDAQ", "USD", n("1"), n("101"), n("0"),
                LocalDate.of(2026,1,3), new OperationCosts(n("0"), n("0"), n("0"), n("0.10")), n("5.20")));
        var summary = investments.taxSummary(2026);
        assertThat(summary.totalWithheld()).isEqualByComparingTo("0.52");
        assertThat(summary.events()).singleElement().satisfies(event -> {
            assertThat(event.currency()).isEqualTo("BRL");
            assertThat(event.grossAmount()).isEqualByComparingTo("525.20");
        });
    }
}
