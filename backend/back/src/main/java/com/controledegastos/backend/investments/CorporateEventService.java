package com.controledegastos.backend.investments;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.InvestmentDtos.WalletEarningAdjustmentRequest;
import com.controledegastos.backend.investments.InvestmentDtos.WalletEarningResponse;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CorporateEventService {
    private static final int PILOT_LOOKBACK_DAYS = 90;
    private static final int PILOT_LOOKAHEAD_DAYS = 180;
    private final CorporateEventRepository corporateEventRepository;
    private final WalletEarningRepository walletEarningRepository;
    private final InvestmentPositionRepository positionRepository;
    private final InvestmentMovementRepository movementRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MarketDataProvider marketDataProvider;
    private final CorporateEventPilotAccess pilotAccess;
    private final com.controledegastos.backend.security.AuthenticatedUserService authenticatedUserService;

    @Value("${app.investments.corporate-events.automatic-sync-enabled:false}")
    private boolean automaticSyncEnabled;

    @Transactional
    public List<WalletEarningResponse> synchronizeCurrentUser() {
        User user = authenticatedUserService.getAuthenticatedUser();
        synchronize(user);
        return responses(user);
    }

    @Transactional(readOnly = true)
    public InvestmentDtos.CorporateEventPilotAccessResponse pilotAccess() {
        return new InvestmentDtos.CorporateEventPilotAccessResponse(pilotAccess.canUse(authenticatedUserService.getAuthenticatedUser()));
    }

    @Transactional(readOnly = true)
    public List<InvestmentDtos.CorporateEventPreviewResponse> previewCurrentUser() {
        User user = authenticatedUserService.getAuthenticatedUser();
        pilotAccess.require(user);
        return pilotCandidates(user).stream().map(PilotCandidate::response).toList();
    }

    @Transactional
    public List<WalletEarningResponse> publishCurrentUser(InvestmentDtos.CorporateEventPublishRequest request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        pilotAccess.require(user);
        Set<String> selected = Set.copyOf(request.sourceReferences());
        List<PilotCandidate> accepted = pilotCandidates(user).stream()
                .filter(candidate -> candidate.isPublishable() && selected.contains(candidate.data().sourceReference()))
                .toList();
        if (accepted.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma previsão válida da prévia atual");
        }
        for (PilotCandidate candidate : accepted) {
            CorporateEvent event = upsert(candidate.data());
            createEarningIfNeeded(user, candidate.position(), event);
        }
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
        if (!automaticSyncEnabled) return;
        List<InvestmentPosition> allPositions = positionRepository.findAll();
        List<CorporateEvent> events = marketDataProvider.corporateEvents(LocalDate.now(), symbolsOf(allPositions)).stream()
                .filter(data -> "VALIDO".equals(data.resolutionStatus()) && data.symbol() != null)
                .map(this::upsert).toList();
        for (User user : userRepository.findAll()) {
            List<InvestmentPosition> positions = allPositions.stream().filter(position -> position.getUser().getId().equals(user.getId())).toList();
            synchronize(user, positions, events);
        }
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
                .user(user).position(position).movementType(event.getEventType() == CorporateEvent.EventType.RENDIMENTO
                        ? InvestmentMovement.MovementType.RENDIMENTO : InvestmentMovement.MovementType.DIVIDENDO)
                .amount(earning.getNetAmount()).eventDate(event.getPaymentDate()).automatic(true)
                .externalReference(reference).build());
        transactionRepository.save(Transaction.builder().user(user).type(Transaction.TransactionType.RECEITA)
                .description(eventLabel(event) + " - " + position.getName())
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
        List<InvestmentPosition> positions = positionRepository.findAllByUserOrderByCreatedAtDesc(user);
        List<CorporateEvent> events = marketDataProvider.corporateEvents(LocalDate.now(), symbolsOf(positions)).stream().map(this::upsert).toList();
        synchronize(user, positions, events);
    }

    private void synchronize(User user, List<InvestmentPosition> positions, List<CorporateEvent> events) {
        for (CorporateEvent event : events) {
            if (event.getExDate().isAfter(LocalDate.now())) continue;
            positions.stream().filter(position -> isEligibleAsset(position, event)).forEach(position -> createEarningIfNeeded(user, position, event));
        }
        refreshStatuses(user);
    }

    private Set<String> symbolsOf(List<InvestmentPosition> positions) {
        return positions.stream()
                .filter(position -> position.getAssetType() != InvestmentPosition.AssetType.RENDA_FIXA)
                .filter(position -> position.getSymbol() != null && !position.getSymbol().isBlank())
                .map(InvestmentPosition::getSymbol)
                .collect(java.util.stream.Collectors.toSet());
    }

    private CorporateEvent upsert(MarketDataProvider.CorporateEventData data) {
        if (data.symbol() == null || data.symbol().isBlank()) {
            throw new IllegalArgumentException("O evento B3 não possui um ticker resolvido com segurança");
        }
        return corporateEventRepository.findBySourceReference(data.sourceReference()).orElseGet(() -> corporateEventRepository.save(CorporateEvent.builder()
                .sourceReference(data.sourceReference()).symbol(data.symbol().trim().toUpperCase())
                .isinCode(data.isinCode()).eventType(data.eventType()).payerCnpj(data.payerCnpj()).amountPerUnit(data.amountPerUnit())
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

    private List<PilotCandidate> pilotCandidates(User user) {
        List<InvestmentPosition> positions = positionRepository.findAllByUserOrderByCreatedAtDesc(user);
        LocalDate today = LocalDate.now();
        return marketDataProvider.corporateEvents(today, symbolsOf(positions)).stream()
                .map(data -> pilotCandidate(data, positions, today))
                .sorted(Comparator.comparing(candidate -> candidate.data().paymentDate()))
                .toList();
    }

    private PilotCandidate pilotCandidate(MarketDataProvider.CorporateEventData data, List<InvestmentPosition> positions, LocalDate today) {
        if (!"VALIDO".equals(data.resolutionStatus()) || data.symbol() == null || data.symbol().isBlank()) {
            return PilotCandidate.rejected(data, "TICKER_AMBIGUO", "A B3 retornou uma classe de ação que não pôde ser vinculada ao ticker da carteira.");
        }
        InvestmentPosition position = positions.stream().filter(item -> isEligibleAsset(item, asEvent(data))).findFirst().orElse(null);
        if (position == null) return PilotCandidate.rejected(data, "ATIVO_FORA_DA_CARTEIRA", "O ativo do evento não está disponível na carteira atual.");
        if (data.paymentDate().isBefore(today.minusDays(PILOT_LOOKBACK_DAYS)) || data.paymentDate().isAfter(today.plusDays(PILOT_LOOKAHEAD_DAYS))) {
            return PilotCandidate.rejected(data, "FORA_DA_JANELA", "O piloto mostra pagamentos dos últimos 90 e dos próximos 180 dias.");
        }
        BigDecimal quantity = eligibleQuantity(position, data.exDate());
        if (quantity.signum() <= 0) return PilotCandidate.rejected(data, position, "SEM_COTAS_ELEGIVEIS", "Não havia cotas elegíveis na Data Com.");
        BigDecimal gross = money(quantity.multiply(data.amountPerUnit()));
        BigDecimal withheld = money(gross.multiply(data.taxRate()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return PilotCandidate.accepted(data, position, quantity, gross, withheld, money(gross.subtract(withheld)));
    }

    private CorporateEvent asEvent(MarketDataProvider.CorporateEventData data) {
        return CorporateEvent.builder().symbol(data.symbol()).eventType(data.eventType()).exDate(data.exDate())
                .paymentDate(data.paymentDate()).build();
    }

    private String eventLabel(CorporateEvent event) {
        return switch (event.getEventType()) {
            case JCP -> "JCP";
            case RENDIMENTO -> "Rendimento";
            case DIVIDENDO -> "Dividendo";
        };
    }

    private record PilotCandidate(MarketDataProvider.CorporateEventData data, InvestmentPosition position,
                                  BigDecimal quantity, BigDecimal gross, BigDecimal withheld, BigDecimal net,
                                  String status, String reason) {
        static PilotCandidate accepted(MarketDataProvider.CorporateEventData data, InvestmentPosition position,
                                       BigDecimal quantity, BigDecimal gross, BigDecimal withheld, BigDecimal net) {
            return new PilotCandidate(data, position, quantity, gross, withheld, net, "VALIDO", "Pronto para publicar na Agenda.");
        }
        static PilotCandidate rejected(MarketDataProvider.CorporateEventData data, String status, String reason) {
            return rejected(data, null, status, reason);
        }
        static PilotCandidate rejected(MarketDataProvider.CorporateEventData data, InvestmentPosition position, String status, String reason) {
            return new PilotCandidate(data, position, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, status, reason);
        }
        boolean isPublishable() { return "VALIDO".equals(status) && position != null; }
        InvestmentDtos.CorporateEventPreviewResponse response() {
            return new InvestmentDtos.CorporateEventPreviewResponse(data.sourceReference(), position == null ? null : position.getId(), data.symbol(),
                    position == null ? "Ativo não identificado" : position.getName(), data.isinCode(), data.eventType(), data.amountPerUnit(), quantity,
                    gross, withheld, net, data.taxRate(), data.exDate(), data.paymentDate(), data.source(), status, reason);
        }
    }
}
