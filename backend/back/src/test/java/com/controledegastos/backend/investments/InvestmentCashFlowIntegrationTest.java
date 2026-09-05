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
import org.springframework.mock.web.MockMultipartFile;
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
    @Autowired CorporateEventService corporateEvents;
    @Autowired InvestmentImportService investmentImports;
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

    @Test void dailyLiquidityFixedIncomeDoesNotRequireMaturityDate() {
        var position = investments.create(new PositionRequest(InvestmentPosition.AssetType.RENDA_FIXA, null, null, "CDB liquidez diária", null, null, n("1000"), n("12"),
                LocalDate.of(2026, 1, 2), null, "BR", "B3", "BRL", FixedIncomeTax.Regime.REGRESSIVO, null, true, null,
                InvestmentPosition.FixedIncomeYieldType.POS_FIXADO, "CDI", true));
        assertThat(position.dailyLiquidity()).isTrue();
        assertThat(position.maturityDate()).isNull();
        assertThat(position.fixedIncomeIndexer()).isEqualTo("CDI");
    }

    @Test void retroactiveTradeCanBeCorrectedAndKeepsLinkedCashFlowInSync() {
        var buy = investments.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, "19.50", LocalDate.of(2026, 1, 2)));
        investments.updateMovement(buy.id(), new MovementUpdateRequest(n("3"), n("20.00"), n("0"), LocalDate.of(2026, 1, 1),
                new OperationCosts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), null));
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(entry -> {
            assertThat(entry.getAmount()).isEqualByComparingTo("60.00");
            assertThat(entry.getTransactionDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        });
        assertThat(investments.portfolio().positions()).singleElement().satisfies(position -> {
            assertThat(position.quantity()).isEqualByComparingTo("3");
            assertThat(position.averagePrice()).isEqualByComparingTo("20");
        });
    }

    @Test void retroactivePurchaseRecalculatesAverageCostAndLaterSale() {
        investments.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, "20.00", LocalDate.of(2026, 1, 3)));
        investments.recordTrade(new TradeRequest(null, InvestmentMovement.MovementType.VENDA, InvestmentPosition.AssetType.ACAO,
                "BBAS3", "BBAS3.SA", "Banco do Brasil", "BR", "B3", "BRL", n("1"), n("25.00"), BigDecimal.ZERO,
                LocalDate.of(2026, 1, 4), new OperationCosts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), null));

        investments.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, "10.00", LocalDate.of(2026, 1, 2)));

        assertThat(investments.portfolio().positions()).singleElement().satisfies(position -> {
            assertThat(position.quantity()).isEqualByComparingTo("3");
            assertThat(position.averagePrice()).isEqualByComparingTo("15.060000");
        });
        assertThat(investments.movements()).filteredOn(movement -> movement.movementType() == InvestmentMovement.MovementType.VENDA)
                .singleElement().satisfies(sale -> {
                    assertThat(sale.realizedGain()).isEqualByComparingTo("9.940000");
                    assertThat(sale.amount()).isEqualByComparingTo("25.00");
                });
    }

    @Test void automaticJcpFreezesExDateQuantityAndCreatesCashOnlyAfterConfirmation() {
        LocalDate today = LocalDate.now();
        investments.recordTrade(trade(InvestmentMovement.MovementType.COMPRA, "20.00", today.minusDays(3)));
        investments.recordTrade(trade(InvestmentMovement.MovementType.VENDA, "20.00", today.minusDays(1)));

        var earnings = corporateEvents.synchronizeCurrentUser();
        var jcp = earnings.stream().filter(earning -> earning.eventType() == CorporateEvent.EventType.JCP).findFirst().orElseThrow();
        assertThat(jcp.quantityEligible()).isEqualByComparingTo("2");
        assertThat(jcp.grossAmount()).isEqualByComparingTo("0.40");
        assertThat(jcp.withheldAmount()).isEqualByComparingTo("0.06");
        assertThat(jcp.netAmount()).isEqualByComparingTo("0.34");
        assertThat(jcp.status()).isEqualTo(WalletEarning.Status.PENDENTE_CONCILIACAO);
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).hasSize(2);

        corporateEvents.confirm(jcp.id());

        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).filteredOn(transaction -> transaction.getDescription().startsWith("JCP -"))
                .singleElement().satisfies(transaction -> {
                    assertThat(transaction.getType()).isEqualTo(Transaction.TransactionType.RECEITA);
                    assertThat(transaction.getCategory()).isEqualTo(Transaction.TransactionCategory.INVESTIMENTO);
                    assertThat(transaction.getAmount()).isEqualByComparingTo("0.34");
                });
        assertThat(investments.taxSummary(today.getYear()).events()).filteredOn(event -> event.eventType().equals("JCP"))
                .singleElement().satisfies(event -> {
                    assertThat(event.status()).isEqualTo(TaxStatus.RETIDO_INTEGRAL);
                    assertThat(event.withheldAmount()).isEqualByComparingTo("0.06");
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

    @Test void investmentFileStaysInReviewUntilTheUserConfirmsIt() {
        MockMultipartFile file = new MockMultipartFile("file", "carteira.csv", "text/csv", ("Data;Ticker;Operação;Quantidade;Preço;Corretagem;IRRF\n"
                + "02/01/2026;PETR4;COMPRA;2;30,50;0,10;0\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var preview = investmentImports.preview(file);
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.symbol()).isEqualTo("PETR4");
            assertThat(item.movementType()).isEqualTo(InvestmentMovement.MovementType.COMPRA);
        });
        assertThat(investments.movements()).isEmpty();

        var item = preview.items().getFirst();
        var response = investmentImports.confirm(preview.batchId(), new InvestmentImportDtos.ConfirmRequest(List.of(
                new InvestmentImportDtos.ConfirmItemRequest(item.id(), true, item.movementType(), item.assetType(), item.symbol(), item.name(),
                        item.market(), item.exchange(), item.currency(), item.quantity(), item.unitPrice(), item.costs(), item.exchangeRate(), item.eventDate())
        )));
        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(investments.movements()).singleElement().satisfies(movement -> {
            assertThat(movement.assetName()).isEqualTo("PETR4");
            assertThat(movement.quantity()).isEqualByComparingTo("2");
        });
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(transaction ->
                assertThat(transaction.getCategory()).isEqualTo(Transaction.TransactionCategory.INVESTIMENTO));
    }
}
