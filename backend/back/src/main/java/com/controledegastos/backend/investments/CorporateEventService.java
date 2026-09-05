package com.controledegastos.backend.investments;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.InvestmentDtos.WalletEarningAdjustmentRequest;
import com.controledegastos.backend.investments.InvestmentDtos.WalletEarningResponse;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorporateEventService {
    private final CorporateEventRepository corporateEventRepository;
    private final WalletEarningRepository walletEarningRepository;
    private final InvestmentPositionRepository positionRepository;
    private final InvestmentMovementRepository movementRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MarketDataProvider marketDataProvider;
    private final com.controledegastos.backend.security.AuthenticatedUserService authenticatedUserService;

    @Transactional
    public List<WalletEarningResponse> synchronizeCurrentUser() {
        User user = authenticatedUserService.getAuthenticatedUser();
        synchronize(user);
        return responses(user);
    }

    @Transactional
    public List<WalletEarningResponse> walletEarnings() {
        User user = authenticatedUserService.getAuthenticatedUser();
        refreshStatuses(user);
        return responses(user);
    }

    // A fonte mock nunca deve preencher agendas reais sem uma ação explícita do usuário.
    @Scheduled(cron = "${app.investments.corporate-events.sync-cron:-}")
    @Transactional
    public void synchronizeAllWallets() {
        List<CorporateEvent> events = marketDataProvider.corporateEvents(LocalDate.now()).stream().map(this::upsert).toList();
        for (User user : userRepository.findAll()) synchronize(user, events);
    }

    @Transactional
    public WalletEarningResponse confirm(Long id) {
        User user = authenticatedUserService.getAuthenticatedUser();
        WalletEarning earning = walletEarningRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Provento automático não encontrado"));
        refreshStatus(earning);
        if (earning.getStatus() == WalletEarning.Status.EFETIVADO)
            throw new IllegalArgumentException("Este provento já foi confirmado");
        if (earning.getStatus() != WalletEarning.Status.PENDENTE_CONCILIACAO)
            throw new IllegalArgumentException("Aguarde a data de pagamento antes de confirmar o recebimento");
        InvestmentPosition position = earning.getPosition();
        if (!"BRL".equalsIgnoreCase(position.getCurrency()))
            throw new IllegalArgumentException("A confirmação automática está disponível apenas para proventos em reais");

        CorporateEvent event = earning.getCorporateEvent();
        String reference = "wallet-earning:" + earning.getId();
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder()
                .user(user).position(position).movementType(InvestmentMovement.MovementType.DIVIDENDO)
                .amount(earning.getNetAmount()).eventDate(event.getPaymentDate()).automatic(true)
                .externalReference(reference).build());
        transactionRepository.save(Transaction.builder().user(user).type(Transaction.TransactionType.RECEITA)
                .description((event.getEventType() == CorporateEvent.EventType.JCP ? "JCP - " : "Dividendo - ") + position.getName())
                .category(Transaction.TransactionCategory.INVESTIMENTO).amount(earning.getNetAmount())
                .investmentMovementId(movement.getId()).paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA)
                .installments(1).transactionDate(event.getPaymentDate()).build());
        earning.setInvestmentMovementId(movement.getId());
        earning.setStatus(WalletEarning.Status.EFETIVADO);
        return toResponse(walletEarningRepository.save(earning));
    }

    @Transactional
    public WalletEarningResponse adjust(Long id, WalletEarningAdjustmentRequest request) {
        WalletEarning earning = walletEarningRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Provento automático não encontrado"));
        if (earning.getStatus() == WalletEarning.Status.EFETIVADO)
            throw new IllegalArgumentException("Um provento já confirmado não pode ser alterado por esta tela");
        if (Boolean.TRUE.equals(request.cancelled())) {
            earning.setStatus(WalletEarning.Status.CANCELADO);
            return toResponse(walletEarningRepository.save(earning));
        }
        if (earning.getStatus() == WalletEarning.Status.CANCELADO)
            throw new IllegalArgumentException("Um provento cancelado não pode ser reaberto por esta tela");
        BigDecimal gross = request.grossAmount() == null ? earning.getGrossAmount() : money(request.grossAmount());
        BigDecimal withheld = request.withheldAmount() == null ? earning.getWithheldAmount() : money(request.withheldAmount());
        if (gross.signum() <= 0 || withheld.signum() < 0 || withheld.compareTo(gross) > 0)
            throw new IllegalArgumentException("Informe valores válidos para o bruto e o imposto retido");
        earning.setGrossAmount(gross);
        earning.setWithheldAmount(withheld);
        earning.setNetAmount(money(gross.subtract(withheld)));
        return toResponse(walletEarningRepository.save(earning));
    }

    private void synchronize(User user) {
        List<CorporateEvent> events = marketDataProvider.corporateEvents(LocalDate.now()).stream().map(this::upsert).toList();
        synchronize(user, events);
    }

    private void synchronize(User user, List<CorporateEvent> events) {
        List<InvestmentPosition> positions = positionRepository.findAllByUserOrderByCreatedAtDesc(user);
        for (CorporateEvent event : events) {
            if (event.getExDate().isAfter(LocalDate.now())) continue;
            positions.stream().filter(position -> isEligibleAsset(position, event)).forEach(position -> createEarningIfNeeded(user, position, event));
        }
        refreshStatuses(user);
    }

    private CorporateEvent upsert(MarketDataProvider.CorporateEventData data) {
        return corporateEventRepository.findBySourceReference(data.sourceReference()).orElseGet(() -> corporateEventRepository.save(CorporateEvent.builder()
                .sourceReference(data.sourceReference()).symbol(data.symbol().trim().toUpperCase())
                .eventType(data.eventType()).payerCnpj(data.payerCnpj()).amountPerUnit(data.amountPerUnit())
                .taxRate(data.taxRate()).exDate(data.exDate()).paymentDate(data.paymentDate()).source(data.source()).build()));
    }

    private boolean isEligibleAsset(InvestmentPosition position, CorporateEvent event) {
        return position.getAssetType() != InvestmentPosition.AssetType.RENDA_FIXA
                && position.getSymbol() != null && position.getSymbol().equalsIgnoreCase(event.getSymbol())
                && !position.isRedeemed();
    }

    private void createEarningIfNeeded(User user, InvestmentPosition position, CorporateEvent event) {
        if (walletEarningRepository.existsByUserAndCorporateEvent(user, event)) return;
        BigDecimal quantity = eligibleQuantity(position, event.getExDate());
        if (quantity.signum() <= 0) return;
        BigDecimal gross = money(quantity.multiply(event.getAmountPerUnit()));
        BigDecimal withheld = money(gross.multiply(event.getTaxRate()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        walletEarningRepository.save(WalletEarning.builder().user(user).position(position).corporateEvent(event)
                .quantityEligible(quantity).grossAmount(gross).withheldAmount(withheld).netAmount(money(gross.subtract(withheld)))
                .status(statusFor(event)).build());
    }

    private BigDecimal eligibleQuantity(InvestmentPosition position, LocalDate exDate) {
        return movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(position.getUser()).stream()
                .filter(movement -> movement.getPosition().getId().equals(position.getId()))
                .filter(movement -> !movement.getEventDate().isAfter(exDate))
                .map(movement -> switch (movement.getMovementType()) {
                    case COMPRA, SALDO_INICIAL -> movement.getQuantity() == null ? BigDecimal.ZERO : movement.getQuantity();
                    case VENDA -> movement.getQuantity() == null ? BigDecimal.ZERO : movement.getQuantity().negate();
                    default -> BigDecimal.ZERO;
                }).reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
    }

    private void refreshStatuses(User user) {
        walletEarningRepository.findAllByUserOrderByCorporateEventPaymentDateAscCreatedAtDesc(user).forEach(this::refreshStatus);
    }

    private void refreshStatus(WalletEarning earning) {
        if (earning.getStatus() == WalletEarning.Status.PROVISIONADO
                && !earning.getCorporateEvent().getPaymentDate().isAfter(LocalDate.now())) {
            earning.setStatus(WalletEarning.Status.PENDENTE_CONCILIACAO);
            walletEarningRepository.save(earning);
        }
    }

    private List<WalletEarningResponse> responses(User user) {
        return walletEarningRepository.findAllByUserOrderByCorporateEventPaymentDateAscCreatedAtDesc(user).stream()
                .sorted(Comparator.comparing(earning -> earning.getCorporateEvent().getPaymentDate()))
                .map(this::toResponse).toList();
    }

    private WalletEarning.Status statusFor(CorporateEvent event) {
        return event.getPaymentDate().isAfter(LocalDate.now()) ? WalletEarning.Status.PROVISIONADO : WalletEarning.Status.PENDENTE_CONCILIACAO;
    }

    private WalletEarningResponse toResponse(WalletEarning earning) {
        CorporateEvent event = earning.getCorporateEvent();
        InvestmentPosition position = earning.getPosition();
        return new WalletEarningResponse(earning.getId(), position.getId(), position.getSymbol(), position.getName(), event.getEventType(),
                event.getPayerCnpj(), event.getSource(), event.getAmountPerUnit(), earning.getQuantityEligible(), earning.getGrossAmount(),
                earning.getWithheldAmount(), earning.getNetAmount(), event.getTaxRate(), event.getExDate(), event.getPaymentDate(), earning.getStatus());
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
