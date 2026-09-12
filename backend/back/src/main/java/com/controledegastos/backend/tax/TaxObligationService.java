package com.controledegastos.backend.tax;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.TaxPayment;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.TaxProfileType;
import com.controledegastos.backend.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxObligationService {
    private final AuthenticatedUserService auth;
    private final TaxObligationRepository obligations;
    private final TransactionRepository transactions;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public TaxProfileType getTaxProfile() { return auth.getAuthenticatedUser().getTaxProfileType(); }

    @Transactional
    public TaxProfileType saveTaxProfile(TaxProfileType type) {
        if (type == null) throw new IllegalArgumentException("Escolha Pessoa Física ou Pessoa Jurídica");
        User user = auth.getAuthenticatedUser();
        user.setTaxProfileType(type);
        return type;
    }

    @Transactional(readOnly = true)
    public List<TaxObligationResponse> listObligations() {
        User user = requirePf();
        return obligations.findByUser_IdOrderByDueDateAsc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaxObligationResponse createObligation(TaxObligationRequest request) {
        User user = requirePf();
        validateEditableRequest(request);
        TaxObligation obligation = new TaxObligation();
        obligation.setUser(user);
        copyEditableFields(obligation, request);
        obligation.setOrigin(TaxObligation.Origin.MANUAL);
        return toResponse(obligations.save(obligation));
    }

    @Transactional
    public TaxObligationResponse updateObligation(UUID id, TaxObligationRequest request) {
        User user = requirePf();
        validateEditableRequest(request);
        TaxObligation obligation = findOwned(id, user);
        requireUnpaidManual(obligation);
        copyEditableFields(obligation, request);
        return toResponse(obligations.save(obligation));
    }

    @Transactional
    public void deleteObligation(UUID id) {
        User user = requirePf();
        TaxObligation obligation = findOwned(id, user);
        requireUnpaidManual(obligation);
        obligations.delete(obligation);
    }

    @Transactional
    public TaxObligationResponse confirmPayment(UUID id, PaymentConfirmationRequest request) {
        User user = requirePf();
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
        TaxObligation obligation = findOwned(id, user);
        if (obligation.getPaidDate() != null || obligation.getLinkedTransaction() != null || obligation.getLinkedDarf() != null)
            throw new IllegalArgumentException("Esta obrigação já possui pagamento registrado");
        if (obligation.getStatus() == TaxObligation.Status.ISENTA)
            throw new IllegalArgumentException("Obrigação isenta não pode receber pagamento");
        if (request.paidDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Registre somente pagamentos realizados");
        if (request.paidAmount().signum() <= 0)
            throw new IllegalArgumentException("Informe um valor pago maior que zero");

        Transaction transaction = transactions.save(Transaction.builder()
                .user(user).type(Transaction.TransactionType.DESPESA)
                .category(Transaction.TransactionCategory.IMPOSTOS)
                .paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA).installments(1)
                .description(obligation.getName()).amount(request.paidAmount())
                .transactionDate(request.paidDate()).managedReference("tax-obligation:" + obligation.getId())
                .build());
        obligation.setPaidAmount(request.paidAmount());
        obligation.setPaidDate(request.paidDate());
        obligation.setStatus(TaxObligation.Status.PAGA);
        obligation.setLinkedTransaction(transaction);
        obligation.setPaymentAccountDescription(request.accountDescription().trim());
        obligation.setReceiptReference(blankToNull(request.receiptReference()));
        return toResponse(obligations.save(obligation));
    }

    @Transactional
    public void recordPaidInvestmentDarf(User user, TaxPayment payment, Transaction transaction) {
        if (obligations.existsByUser_IdAndLinkedDarf_Id(user.getId(), payment.getId())) return;
        TaxObligation obligation = new TaxObligation();
        obligation.setUser(user);
        obligation.setName("DARF " + payment.getRevenueCode() + " - " + payment.getPeriod());
        obligation.setIssuingAuthority("Receita Federal");
        obligation.setCategory(TaxObligation.Category.DARF);
        obligation.setDueDate(payment.getDueDate());
        obligation.setEstimatedAmount(payment.getAmount());
        obligation.setPaidAmount(payment.getAmount());
        obligation.setPaidDate(payment.getPaidAt());
        obligation.setStatus(TaxObligation.Status.PAGA);
        obligation.setDocumentStage(TaxObligation.DocumentStage.GUIA_EMITIDA);
        obligation.setRecurrence(TaxObligation.Recurrence.UNICA);
        obligation.setCompetenceYear(Integer.parseInt(payment.getPeriod().substring(0, 4)));
        obligation.setCompetenceMonth(Integer.parseInt(payment.getPeriod().substring(5, 7)));
        obligation.setNotes(payment.getNote());
        obligation.setOrigin(TaxObligation.Origin.INVESTIMENTO);
        obligation.setPaymentAccountDescription(payment.getAccountLabel());
        obligation.setLinkedTransaction(transaction);
        obligation.setLinkedDarf(payment);
        obligations.save(obligation);
    }

    private User requirePf() {
        User user = auth.getAuthenticatedUser();
        if (user.getTaxProfileType() != TaxProfileType.PF)
            throw new AccessDeniedException("Selecione o perfil Pessoa Física para acessar a Central de Tributos");
        return user;
    }

    private TaxObligation findOwned(UUID id, User user) {
        return obligations.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Obrigação não encontrada"));
    }

    private void requireUnpaidManual(TaxObligation obligation) {
        if (obligation.getPaidDate() != null || obligation.getLinkedTransaction() != null || obligation.getLinkedDarf() != null)
            throw new IllegalArgumentException("Pagamento confirmado: esta obrigação não pode ser editada ou excluída");
        if (obligation.getOrigin() != TaxObligation.Origin.MANUAL)
            throw new IllegalArgumentException("Obrigações de investimentos devem ser ajustadas na área de Investimentos");
    }

    private void validateEditableRequest(TaxObligationRequest request) {
        if (request.origin() != null && request.origin() != TaxObligation.Origin.MANUAL)
            throw new IllegalArgumentException("A origem de uma obrigação manual deve ser MANUAL");
        if (request.status() == TaxObligation.Status.PAGA || request.status() == TaxObligation.Status.ATRASADA)
            throw new IllegalArgumentException("Pagamento e atraso são estados definidos pelo sistema");
        if (request.recurrence() == TaxObligation.Recurrence.MENSAL && request.competenceMonth() == null)
            throw new IllegalArgumentException("Informe o mês de competência da obrigação mensal");
    }

    private void copyEditableFields(TaxObligation obligation, TaxObligationRequest request) {
        obligation.setName(request.name().trim());
        obligation.setIssuingAuthority(blankToNull(request.issuingAuthority()));
        obligation.setCategory(request.category());
        obligation.setDueDate(request.dueDate());
        obligation.setEstimatedAmount(request.estimatedAmount());
        obligation.setRecurrence(request.recurrence());
        obligation.setCompetenceYear(request.competenceYear());
        obligation.setCompetenceMonth(request.competenceMonth());
        obligation.setNotes(blankToNull(request.notes()));
        obligation.setStatus(request.status() == null ? TaxObligation.Status.A_PAGAR : request.status());
        obligation.setDocumentStage(request.documentStage() == null
                ? TaxObligation.DocumentStage.ESTIMATIVA : request.documentStage());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private TaxObligationResponse toResponse(TaxObligation obligation) {
        TaxObligation.Status status = obligation.getPaidDate() != null ? TaxObligation.Status.PAGA
                : obligation.getStatus() == TaxObligation.Status.ISENTA ? TaxObligation.Status.ISENTA
                : obligation.getDueDate().isBefore(LocalDate.now()) ? TaxObligation.Status.ATRASADA
                : obligation.getStatus();
        String display = switch (status) {
            case A_PAGAR -> "A pagar";
            case PAGA -> "Paga";
            case ATRASADA -> "Atrasada";
            case ISENTA -> "Isenta";
            case EM_REVISAO -> "Em revisão";
        };
        return new TaxObligationResponse(obligation.getId(), obligation.getName(), obligation.getIssuingAuthority(),
                obligation.getCategory(), obligation.getDueDate(), obligation.getEstimatedAmount(), obligation.getPaidAmount(),
                obligation.getPaidDate(), status, display, obligation.getDocumentStage(), obligation.getRecurrence(),
                obligation.getCompetenceYear(), obligation.getCompetenceMonth(), obligation.getNotes(), obligation.getOrigin(),
                obligation.getPaymentAccountDescription(), obligation.getReceiptReference(),
                obligation.getLinkedTransaction() == null ? null : obligation.getLinkedTransaction().getId(),
                obligation.getLinkedDarf() == null ? null : obligation.getLinkedDarf().getId(),
                obligation.getCreatedAt(), obligation.getUpdatedAt());
    }
}
