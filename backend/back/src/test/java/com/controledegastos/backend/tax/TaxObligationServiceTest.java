package com.controledegastos.backend.tax;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.TaxPayment;
import com.controledegastos.backend.investments.TaxPaymentRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.TaxProfileType;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class TaxObligationServiceTest {
    @Autowired TaxObligationService service;
    @Autowired TaxObligationRepository obligations;
    @Autowired TaxPaymentRepository payments;
    @Autowired TransactionRepository transactions;
    @Autowired UserRepository users;
    private User user;

    @BeforeEach void setUp() {
        user = users.save(User.builder().name("Tax test").email("tax-test@example.com")
                .password("unused").role(User.Role.USER).build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        service.saveTaxProfile(TaxProfileType.PF);
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private TaxObligationRequest request(LocalDate dueDate) {
        return new TaxObligationRequest("IPVA", "DETRAN", TaxObligation.Category.IPVA,
                dueDate, new BigDecimal("200.00"), TaxObligation.Recurrence.ANUAL,
                dueDate.getYear(), null, null, TaxObligation.Origin.MANUAL, null, null);
    }

    @Test void creationIsOnlyAnEstimateUntilExplicitPayment() {
        var created = service.createObligation(request(LocalDate.now().plusDays(5)));
        assertThat(created.status()).isEqualTo(TaxObligation.Status.A_PAGAR);
        assertThat(created.documentStage()).isEqualTo(TaxObligation.DocumentStage.ESTIMATIVA);
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).isEmpty();
    }

    @Test void overdueUnpaidObligationIsReportedAsLate() {
        service.createObligation(request(LocalDate.now().minusDays(1)));
        assertThat(service.listObligations()).singleElement()
                .extracting(TaxObligationResponse::status).isEqualTo(TaxObligation.Status.ATRASADA);
    }

    @Test void paymentCreatesOneLinkedExpenseAndCannotBeRepeatedOrDeleted() {
        var created = service.createObligation(request(LocalDate.now().plusDays(2)));
        var paid = service.confirmPayment(created.id(), new PaymentConfirmationRequest(
                new BigDecimal("210.00"), LocalDate.now(), "Conta principal", "guia-123"));
        assertThat(paid.status()).isEqualTo(TaxObligation.Status.PAGA);
        assertThat(paid.linkedTransactionId()).isNotNull();
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).singleElement().satisfies(entry -> {
            assertThat(entry.getType()).isEqualTo(Transaction.TransactionType.DESPESA);
            assertThat(entry.getCategory()).isEqualTo(Transaction.TransactionCategory.IMPOSTOS);
            assertThat(entry.getAmount()).isEqualByComparingTo("210.00");
            assertThat(entry.getManagedReference()).isEqualTo("tax-obligation:" + created.id());
        });
        assertThatThrownBy(() -> service.confirmPayment(created.id(), new PaymentConfirmationRequest(
                BigDecimal.ONE, LocalDate.now(), "Conta principal", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteObligation(created.id())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void cannotReadAnotherUsersObligation() {
        var created = service.createObligation(request(LocalDate.now().plusDays(1)));
        User other = users.save(User.builder().name("Other").email("tax-other@example.com")
                .password("unused").role(User.Role.USER).taxProfileType(TaxProfileType.PF).build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(other.getEmail(), null, List.of()));
        assertThat(service.listObligations()).isEmpty();
        assertThatThrownBy(() -> service.deleteObligation(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void pjProfileCannotUsePfObligations() {
        service.saveTaxProfile(TaxProfileType.PJ);
        assertThatThrownBy(service::listObligations).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.createObligation(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void paidInvestmentDarfIsLinkedOnceWithoutAnotherExpense() {
        TaxPayment payment = new TaxPayment();
        payment.setUser(user); payment.setPeriod("2026-08"); payment.setRevenueCode("6015");
        payment.setAmount(new BigDecimal("50.00")); payment.setPaidAt(LocalDate.now());
        payment.setDueDate(LocalDate.now()); payment.setAccountLabel("Conta principal");
        payment.setNote("Pago no Sicalc"); payments.save(payment);
        Transaction transaction = transactions.save(Transaction.builder().user(user)
                .type(Transaction.TransactionType.DESPESA).category(Transaction.TransactionCategory.IMPOSTOS)
                .paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA).installments(1)
                .description("DARF").amount(payment.getAmount()).transactionDate(LocalDate.now())
                .managedReference("tax-payment:" + payment.getId()).build());

        service.recordPaidInvestmentDarf(user, payment, transaction);
        service.recordPaidInvestmentDarf(user, payment, transaction);

        assertThat(obligations.findByUser_IdOrderByDueDateAsc(user.getId())).singleElement().satisfies(obligation -> {
            assertThat(obligation.getLinkedDarf().getId()).isEqualTo(payment.getId());
            assertThat(obligation.getLinkedTransaction().getId()).isEqualTo(transaction.getId());
        });
        assertThat(transactions.findAllByUserOrderByTransactionDateDesc(user)).hasSize(1);
    }
}
