package com.controledegastos.backend.investments;

import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.controledegastos.backend.investments.FixedIncomeTax.money;

@RestController @RequestMapping("/api/investments/tax") @RequiredArgsConstructor
public class InvestmentTaxController {
    private final AuthenticatedUserService auth;
    private final TaxOpeningBalanceRepository openings;
    private final TaxPaymentRepository payments;
    private final InvestmentMovementRepository movements;
    private final TransactionRepository transactions;
    private final jakarta.persistence.EntityManager entityManager;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public record OpeningRequest(@NotNull LocalDate startDate, @NotNull @DecimalMin("0") BigDecimal commonLoss,
            @NotNull @DecimalMin("0") BigDecimal dayTradeLoss, @NotNull @DecimalMin("0") BigDecimal fundLoss,
            @NotNull @DecimalMin("0") BigDecimal commonCredit, @NotNull @DecimalMin("0") BigDecimal dayTradeCredit,
            @NotNull @DecimalMin("0") @DecimalMax("9.99") BigDecimal pendingTax, @NotBlank @Size(max = 255) String source) {}
    public record PaymentRequest(@NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String period,
            @NotBlank @Pattern(regexp = "6015|4600") String revenueCode, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate paidAt, @NotNull LocalDate dueDate,
            @NotBlank @Size(max = 255) String accountLabel, @NotBlank @Size(max = 255) String note) {}
    public record PaymentResponse(Long id, String period, String revenueCode, BigDecimal amount, LocalDate paidAt) {}
    public record Month(String period, BigDecimal sales, MonthlyTaxCalculator.Bucket common,
            MonthlyTaxCalculator.Bucket funds, BigDecimal estimatedDue, BigDecimal carriedTax,
            boolean review, String note, PaymentResponse payment) {}
    public record Overview(OpeningRequest opening, List<Month> months, List<PaymentResponse> payments) {}

    @PutMapping("/opening") @Transactional
    public OpeningRequest opening(@Valid @RequestBody OpeningRequest request) {
        if (request.startDate().getDayOfMonth() != 1 || request.startDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Informe o primeiro dia de um mes ja iniciado");
        User user = auth.getAuthenticatedUser();
        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!payments.findAllByUser(user).isEmpty()) throw new IllegalArgumentException("Saldo inicial bloqueado apos registrar pagamento; preserve a apuracao conciliada");
        TaxOpeningBalance opening = openings.findByUser(user).orElseGet(TaxOpeningBalance::new);
        opening.setUser(user); opening.setStartDate(request.startDate()); opening.setCommonLoss(request.commonLoss());
        opening.setDayTradeLoss(request.dayTradeLoss()); opening.setFundLoss(request.fundLoss());
        opening.setCommonCredit(request.commonCredit()); opening.setDayTradeCredit(request.dayTradeCredit());
        opening.setPendingTax(request.pendingTax()); opening.setSource(request.source()); openings.save(opening);
        return request;
    }

    @GetMapping @Transactional(readOnly = true)
    public Overview overview() {
        User user = auth.getAuthenticatedUser();
        TaxOpeningBalance opening = openings.findByUser(user).orElse(null);
        List<PaymentResponse> paid = payments.findAllByUser(user).stream().map(this::response).toList();
        if (opening == null) return new Overview(null, List.of(), paid);
        List<InvestmentMovement> all = movements.findAllByUserOrderByEventDateDescCreatedAtDesc(user);
        List<Month> months = new ArrayList<>();
        BigDecimal commonLoss = opening.getCommonLoss(), fundLoss = opening.getFundLoss(), credit = opening.getCommonCredit();
        BigDecimal carry = opening.getPendingTax();
        boolean historyReview = false;
        YearMonth current = YearMonth.now();
        YearMonth first = YearMonth.from(opening.getStartDate());
        if (first.isBefore(current.minusYears(100))) throw new IllegalArgumentException("Periodo inicial muito antigo");
        for (YearMonth period = first; !period.isAfter(current); period = period.plusMonths(1)) {
            if (period.getMonthValue() == 1 && !period.equals(first)) credit = ZERO;
            YearMonth selected = period;
            List<InvestmentMovement> sales = all.stream().filter(m -> m.getMovementType() == InvestmentMovement.MovementType.VENDA)
                    .filter(m -> YearMonth.from(m.getEventDate()).equals(selected)).toList();
            BigDecimal volume = ZERO, commonResult = ZERO, fundResult = ZERO, retained = ZERO, fundRetained = ZERO;
            boolean review = historyReview;
            for (InvestmentMovement sale : sales) {
                var p = sale.getPosition();
                // Same-day matching needs broker identity and complete executions, absent in legacy movements.
                boolean sameDay = all.stream().anyMatch(m -> m.getMovementType() == InvestmentMovement.MovementType.COMPRA
                        && m.getPosition().getId().equals(p.getId()) && m.getEventDate().equals(sale.getEventDate()));
                if (sameDay || sale.getRealizedGain() == null || !"BR".equals(p.getMarket()) || !"BRL".equals(p.getCurrency())
                        || (p.getAssetType() != InvestmentPosition.AssetType.ACAO && p.getAssetType() != InvestmentPosition.AssetType.FII)) {
                    review = true; continue;
                }
                BigDecimal withheld = sale.getCosts() == null ? ZERO : sale.getCosts().retention();
                if (p.getAssetType() == InvestmentPosition.AssetType.FII) {
                    fundResult = fundResult.add(sale.getRealizedGain()); fundRetained = fundRetained.add(withheld);
                } else {
                    volume = volume.add(sale.getQuantity().multiply(sale.getUnitPrice()));
                    commonResult = commonResult.add(sale.getRealizedGain()); retained = retained.add(withheld);
                }
            }
            var common = MonthlyTaxCalculator.calculate(commonResult, volume, commonLoss, credit.add(retained), new BigDecimal("15"), new BigDecimal("20000"));
            var funds = MonthlyTaxCalculator.calculate(fundResult, ZERO, fundLoss, fundRetained, new BigDecimal("20"), null);
            BigDecimal due = ZERO;
            if (!review) {
                commonLoss = common.remainingLoss(); fundLoss = funds.remainingLoss(); credit = common.remainingCredit();
                if (funds.remainingCredit().signum() > 0) review = true;
                BigDecimal total = common.tax().add(funds.tax()).add(carry);
                if (total.compareTo(BigDecimal.TEN) < 0) carry = total;
                else { due = total; carry = ZERO; }
            }
            PaymentResponse payment = paid.stream().filter(p -> p.period().equals(selected.toString()) && p.revenueCode().equals("6015")).findFirst().orElse(null);
            boolean paymentMismatch = !review && payment != null && payment.amount().compareTo(money(due)) != 0;
            review = review || paymentMismatch;
            historyReview = review;
            months.add(new Month(period.toString(), money(volume), common, funds, review ? null : money(due), money(carry), review,
                    paymentMismatch ? "Pagamento difere da estimativa atual. Confira ajustes, multas, juros e alteracoes no historico antes de considerar a competencia conciliada."
                            : review ? "Revisar historico: operacao nao suportada, credito residual ou divergencia de pagamento. Meses seguintes dependem dessa revisao."
                            : period.equals(current) ? "Previa do mes em andamento" : "Estimativa 6015. Confirme operacoes de todas as corretoras e vencimento no Sicalc.", payment));
        }
        Collections.reverse(months);
        return new Overview(new OpeningRequest(opening.getStartDate(), opening.getCommonLoss(), opening.getDayTradeLoss(), opening.getFundLoss(),
                opening.getCommonCredit(), opening.getDayTradeCredit(), opening.getPendingTax(), opening.getSource()), months, paid);
    }

    @PostMapping("/payments") @Transactional
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        User user = auth.getAuthenticatedUser();
        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (request.paidAt().isAfter(LocalDate.now())) throw new IllegalArgumentException("Registre somente pagamentos realizados");
        if (payments.existsByUserAndPeriodAndRevenueCode(user, request.period(), request.revenueCode()))
            throw new IllegalArgumentException("Pagamento ja registrado para esta competencia e codigo");
        TaxPayment payment = new TaxPayment();
        payment.setUser(user); payment.setPeriod(request.period()); payment.setRevenueCode(request.revenueCode());
        payment.setAmount(money(request.amount())); payment.setPaidAt(request.paidAt()); payment.setDueDate(request.dueDate());
        payment.setAccountLabel(request.accountLabel()); payment.setNote(request.note()); payments.save(payment);
        transactions.save(Transaction.builder().user(user).type(Transaction.TransactionType.DESPESA)
                .category(Transaction.TransactionCategory.IMPOSTOS).paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA).installments(1)
                .description("DARF " + request.revenueCode() + " - " + request.period()).amount(payment.getAmount()).transactionDate(request.paidAt())
                .managedReference("tax-payment:" + payment.getId()).build());
        return response(payment);
    }
    private PaymentResponse response(TaxPayment payment) { return new PaymentResponse(payment.getId(), payment.getPeriod(), payment.getRevenueCode(), payment.getAmount(), payment.getPaidAt()); }
}
