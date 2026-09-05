package com.controledegastos.backend.investments;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.investments.InvestmentDtos.*;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final InvestmentPositionRepository repository;
    private final AuthenticatedUserService authenticatedUserService;
    private final MarketQuoteService marketQuoteService;
    private final AssetCatalogService assetCatalogService;
    private final InvestmentMovementRepository movementRepository;
    private final InvestmentPortfolioSnapshotRepository snapshotRepository;
    private final TransactionRepository transactionRepository;
    private final InvestmentIncomeScheduleRepository incomeScheduleRepository;
    private final InvestmentGoalRepository goalRepository;
    private final InvestmentGoalContributionRepository goalContributionRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Value("${app.investments.default-annual-rate:12.0}")
    private BigDecimal defaultAnnualRate;

    @Transactional
    public PortfolioResponse portfolio() {
        User user = authenticatedUserService.getAuthenticatedUser();
        List<InvestmentMovement> movements = movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user);
        Map<Long, BigDecimal> incomeByPosition = movements.stream()
                .filter(this::isIncome)
                .collect(Collectors.groupingBy(movement -> movement.getPosition().getId(),
                        Collectors.reducing(BigDecimal.ZERO, InvestmentMovement::getAmount, BigDecimal::add)));
        List<PositionResponse> positions = repository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .filter(position -> !position.isRedeemed())
                .filter(position -> position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA
                        || position.getQuantity() == null || position.getQuantity().signum() > 0)
                .map(position -> toResponse(position, incomeByPosition.getOrDefault(position.getId(), ZERO))).toList();
        BigDecimal invested = positions.stream().map(PositionResponse::investedAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal current = positions.stream().map(PositionResponse::currentValue).reduce(ZERO, BigDecimal::add);
        BigDecimal capitalGain = current.subtract(invested);
        BigDecimal income = positions.stream().map(PositionResponse::incomeAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalReturn = capitalGain.add(income);

        saveDailySnapshot(user, invested, current, income);
        List<PortfolioEvolutionPoint> evolution = snapshotRepository
                .findAllByUserAndSnapshotDateGreaterThanEqualOrderBySnapshotDate(user, LocalDate.now().minusMonths(12))
                .stream().map(snapshot -> new PortfolioEvolutionPoint(snapshot.getSnapshotDate(), snapshot.getInvestedAmount(),
                        snapshot.getCurrentValue(), snapshot.getIncomeAmount())).toList();
        return new PortfolioResponse(money(invested), money(current), money(capitalGain), money(income), money(totalReturn),
                percentage(totalReturn, invested), positions, evolution);
    }

    @Transactional
    public PositionResponse create(PositionRequest request) {
        validate(request);
        InvestmentPosition position = new InvestmentPosition();
        position.setUser(authenticatedUserService.getAuthenticatedUser());
        entityManager.lock(position.getUser(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        apply(position, request);
        if (position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA &&
                (!"BRL".equals(position.getCurrency()) || !"BR".equals(position.getMarket())))
            throw new IllegalArgumentException("Aplicacoes de renda fixa suportam apenas o mercado brasileiro em reais nesta versao");
        if (position.getAssetType() != InvestmentPosition.AssetType.RENDA_FIXA &&
                findPosition(position.getUser(), position.getAssetType(), position.getMarket(), position.getSymbol(), position.getExternalId()).isPresent())
            throw new IllegalArgumentException("Este ativo ja possui posicao. Registre as proximas operacoes pela compra ou venda");
        repository.save(position);
        if (request.openingDate() != null && request.openingDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("O saldo inicial nao pode estar no futuro");
        if (position.getAssetType() != InvestmentPosition.AssetType.RENDA_FIXA && request.openingDate() == null)
            throw new IllegalArgumentException("Use uma compra ou informe a data do saldo inicial");
        if (request.purchaseDate().isAfter(LocalDate.now()) || (request.openingDate() != null && request.purchaseDate().isAfter(request.openingDate())))
            throw new IllegalArgumentException("A data da aplicacao deve ser anterior ou igual ao saldo inicial e ao dia atual");
        BigDecimal amount = position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA ? position.getPrincipal()
                : position.getQuantity().multiply(position.getAveragePrice());
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder().user(position.getUser()).position(position)
                .movementType(request.openingDate() == null ? InvestmentMovement.MovementType.APORTE : InvestmentMovement.MovementType.SALDO_INICIAL)
                .amount(money(amount)).quantity(position.getQuantity()).unitPrice(position.getAveragePrice())
                .eventDate(request.openingDate() == null ? request.purchaseDate() : request.openingDate()).automatic(false).build());
        if (request.openingDate() == null) recordCashFlow(movement, BigDecimal.ONE);
        return toResponse(position, ZERO);
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        validate(request);
        InvestmentPosition position = owned(id);
        if (movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(position.getUser()).stream()
                .anyMatch(m -> m.getPosition().getId().equals(id)))
            throw new IllegalArgumentException("Uma posicao com movimentos nao pode ter seu saldo sobrescrito");
        apply(position, request);
        return toResponse(repository.save(position), ZERO);
    }

    @Transactional
    public void delete(Long id) {
        InvestmentPosition position = owned(id);
        if (movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(position.getUser()).stream()
                .anyMatch(m -> m.getPosition().getId().equals(id)))
            throw new IllegalArgumentException("Registre uma venda ou resgate para encerrar esta posicao");
        repository.delete(position);
    }

    public QuoteResponse quote(InvestmentPosition.AssetType type, String symbol, String externalId, String market) {
        return marketQuoteService.quote(type, symbol, externalId, market);
    }

    public List<AssetSearchResponse> searchAssets(String query, InvestmentPosition.AssetType type) {
        return assetCatalogService.search(query, type);
    }

    @Transactional
    public MovementResponse recordTrade(TradeRequest request) {
        if (request.movementType() != InvestmentMovement.MovementType.COMPRA
                && request.movementType() != InvestmentMovement.MovementType.VENDA) {
            throw new IllegalArgumentException("Selecione compra ou venda");
        }
        if (request.assetType() == InvestmentPosition.AssetType.RENDA_FIXA) {
            throw new IllegalArgumentException("Renda fixa continua no fluxo de aplicações e resgates");
        }

        User user = authenticatedUserService.getAuthenticatedUser();
        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        BigDecimal fees = request.costs() == null ? (request.fees() == null ? BigDecimal.ZERO : request.fees()) : request.costs().total();
        BigDecimal withheld = request.costs() == null ? BigDecimal.ZERO : request.costs().retention();
        BigDecimal fx = "BRL".equalsIgnoreCase(request.currency()) ? BigDecimal.ONE : request.exchangeRate();
        if (fx == null || fx.signum() <= 0) throw new IllegalArgumentException("Informe o cambio da operacao para registrar o fluxo em reais");
        if (request.eventDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("A operacao nao pode estar no futuro");
        if (request.movementType() == InvestmentMovement.MovementType.COMPRA && withheld.signum() > 0)
            throw new IllegalArgumentException("Informe IRRF antecipado na venda correspondente");
        String market = normalizeUpper(request.market(), "BR");
        String symbol = normalizeUpper(request.symbol(), null);
        String externalId = blank(request.externalId()) ? null : request.externalId().trim();
        validateTradeIdentity(request.assetType(), symbol, externalId, market);
        if (request.requestId() != null) {
            var previous = movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user).stream()
                    .filter(m -> ("trade:" + request.requestId()).equals(m.getExternalReference())).findFirst();
            if (previous.isPresent()) {
                InvestmentMovement saved = previous.get();
                InvestmentPosition asset = saved.getPosition();
                BigDecimal savedWithheld = saved.getCosts() == null ? ZERO : saved.getCosts().retention();
                if (saved.getMovementType() != request.movementType() || asset.getAssetType() != request.assetType()
                        || !java.util.Objects.equals(asset.getSymbol(), symbol)
                        || !java.util.Objects.equals(asset.getExternalId(), externalId)
                        || !asset.getMarket().equals(market) || !asset.getCurrency().equalsIgnoreCase(request.currency())
                        || (request.positionId() != null && !request.positionId().equals(asset.getId()))
                        || !saved.getEventDate().equals(request.eventDate())
                        || saved.getQuantity().compareTo(request.quantity()) != 0
                        || saved.getUnitPrice().compareTo(request.unitPrice()) != 0
                        || saved.getFees().compareTo(money(fees)) != 0
                        || savedWithheld.compareTo(money(withheld)) != 0
                        || saved.getExchangeRate().compareTo(fx) != 0)
                    throw new IllegalArgumentException("Esta solicitacao ja foi registrada com outros valores. Reabra o formulario para uma nova operacao");
                return toMovementResponse(saved);
            }
        }

        InvestmentPosition position = request.positionId() == null
                ? findPosition(user, request.assetType(), market, symbol, externalId).orElse(null)
                : repository.findByIdAndUser(request.positionId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

        if (position == null) {
            if (request.movementType() == InvestmentMovement.MovementType.VENDA) {
                throw new IllegalArgumentException("Não existe posição disponível para esta venda");
            }
            position = InvestmentPosition.builder()
                    .user(user)
                    .assetType(request.assetType())
                    .symbol(symbol)
                    .externalId(externalId)
                    .name(request.name().trim())
                    .market(market)
                    .exchange(blank(request.exchange()) ? null : request.exchange().trim())
                    .currency(normalizeUpper(request.currency(), market.equals("BR") ? "BRL" : "USD"))
                    .quantity(BigDecimal.ZERO)
                    .averagePrice(BigDecimal.ZERO)
                    .purchaseDate(request.eventDate())
                    .build();
        } else if (position.getAssetType() != request.assetType()) {
            throw new IllegalArgumentException("O ativo selecionado não corresponde à posição informada");
        }
        if (!position.getCurrency().equalsIgnoreCase(request.currency()) || !position.getMarket().equalsIgnoreCase(market))
            throw new IllegalArgumentException("Moeda e mercado devem corresponder a posicao");
        if (position.getSymbol() != null && !position.getSymbol().equalsIgnoreCase(symbol))
            throw new IllegalArgumentException("O ticker deve corresponder a posicao selecionada");
        if (position.getOpeningDate() != null && request.eventDate().isBefore(position.getOpeningDate()))
            throw new IllegalArgumentException("A operacao deve ocorrer apos o saldo inicial importado");
        Long existingId = position.getId();
        if (existingId != null && movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user).stream()
                .anyMatch(m -> m.getPosition().getId().equals(existingId) && m.getEventDate().isAfter(request.eventDate())))
            throw new IllegalArgumentException("Registre operacoes em ordem cronologica para preservar o custo medio");

        BigDecimal currentQuantity = position.getQuantity() == null ? BigDecimal.ZERO : position.getQuantity();
        BigDecimal currentAverage = position.getAveragePrice() == null ? BigDecimal.ZERO : position.getAveragePrice();
        BigDecimal gross = request.quantity().multiply(request.unitPrice());
        if (request.movementType() == InvestmentMovement.MovementType.COMPRA) {
            BigDecimal newQuantity = currentQuantity.add(request.quantity());
            BigDecimal newCost = currentQuantity.multiply(currentAverage).add(gross).add(fees);
            position.setQuantity(newQuantity);
            position.setAveragePrice(newCost.divide(newQuantity, 6, RoundingMode.HALF_UP));
            if (request.eventDate().isBefore(position.getPurchaseDate())) position.setPurchaseDate(request.eventDate());
        } else {
            if (request.quantity().compareTo(currentQuantity) > 0) {
                throw new IllegalArgumentException("A venda excede a quantidade disponível de " + currentQuantity.stripTrailingZeros().toPlainString());
            }
            if (fees.add(withheld).compareTo(gross) >= 0) throw new IllegalArgumentException("Os custos e retencoes devem ser menores que o valor bruto");
            BigDecimal remaining = currentQuantity.subtract(request.quantity());
            position.setQuantity(remaining);
            if (remaining.signum() == 0) position.setAveragePrice(BigDecimal.ZERO);
        }

        InvestmentPosition saved = repository.save(position);
        BigDecimal amount = request.movementType() == InvestmentMovement.MovementType.COMPRA
                ? gross.add(fees) : gross.subtract(fees).subtract(withheld);
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder()
                .user(user)
                .position(saved)
                .movementType(request.movementType())
                .amount(money(amount))
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .fees(money(fees))
                .costs(request.costs())
                .externalReference(request.requestId() == null ? null : "trade:" + request.requestId())
                .exchangeRate(fx)
                .costBasis(request.movementType() == InvestmentMovement.MovementType.VENDA ? currentAverage.multiply(request.quantity()) : null)
                .realizedGain(request.movementType() == InvestmentMovement.MovementType.VENDA ? gross.subtract(fees).subtract(currentAverage.multiply(request.quantity())) : null)
                .eventDate(request.eventDate())
                .automatic(false)
                .build());
        recordCashFlow(movement, fx);
        return toMovementResponse(movement);
    }

    @Transactional
    public MovementResponse recordIncome(Long positionId, IncomeRequest request) {
        if (request.movementType() != InvestmentMovement.MovementType.DIVIDENDO
                && request.movementType() != InvestmentMovement.MovementType.RENDIMENTO) {
            throw new IllegalArgumentException("Este fluxo aceita apenas dividendos ou rendimentos");
        }
        User user = authenticatedUserService.getAuthenticatedUser();
        InvestmentPosition position = repository.findByIdAndUser(positionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        return recordIncome(user, position, request.movementType(), request.amount(), request.eventDate(), null);
    }

    @Transactional(readOnly = true)
    public List<IncomeScheduleResponse> incomeSchedules() {
        User user = authenticatedUserService.getAuthenticatedUser();
        return incomeScheduleRepository.findAllByUserOrderByPaymentDateAscCreatedAtDesc(user).stream()
                .map(this::toIncomeScheduleResponse).toList();
    }

    @Transactional
    public IncomeScheduleResponse createIncomeSchedule(IncomeScheduleRequest request) {
        if (request.incomeType() != InvestmentMovement.MovementType.DIVIDENDO
                && request.incomeType() != InvestmentMovement.MovementType.RENDIMENTO) {
            throw new IllegalArgumentException("A agenda aceita apenas dividendos ou rendimentos");
        }
        if (request.exDate() != null && request.paymentDate().isBefore(request.exDate())) {
            throw new IllegalArgumentException("A data de pagamento deve ser igual ou posterior à Data Com");
        }
        User user = authenticatedUserService.getAuthenticatedUser();
        InvestmentPosition position = repository.findByIdAndUser(request.positionId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        InvestmentIncomeSchedule schedule = InvestmentIncomeSchedule.builder()
                .user(user).position(position).incomeType(request.incomeType()).amountPerUnit(request.amountPerUnit())
                .taxRate(request.taxRate() == null ? BigDecimal.ZERO : request.taxRate())
                .exDate(request.exDate()).paymentDate(request.paymentDate()).build();
        return toIncomeScheduleResponse(incomeScheduleRepository.save(schedule));
    }

    @Transactional
    public IncomeScheduleResponse receiveIncomeSchedule(Long id) {
        User user = authenticatedUserService.getAuthenticatedUser();
        entityManager.lock(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        InvestmentIncomeSchedule schedule = incomeScheduleRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Provento agendado não encontrado"));
        if (schedule.getStatus() == InvestmentIncomeSchedule.Status.RECEBIDO) {
            throw new IllegalArgumentException("Este provento já foi registrado como recebido");
        }
        IncomeScheduleResponse response = toIncomeScheduleResponse(schedule);
        recordIncome(user, schedule.getPosition(), schedule.getIncomeType(), response.netAmount(),
                schedule.getPaymentDate(), "income-schedule:" + schedule.getId());
        schedule.setStatus(InvestmentIncomeSchedule.Status.RECEBIDO);
        return toIncomeScheduleResponse(incomeScheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteIncomeSchedule(Long id) {
        InvestmentIncomeSchedule schedule = incomeScheduleRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Provento agendado não encontrado"));
        if (schedule.getStatus() == InvestmentIncomeSchedule.Status.RECEBIDO) {
            throw new IllegalArgumentException("Proventos recebidos não podem ser removidos da agenda");
        }
        incomeScheduleRepository.delete(schedule);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> goals() {
        User user = authenticatedUserService.getAuthenticatedUser();
        return goalRepository.findAllByUserAndActiveTrueOrderByCreatedAtDesc(user).stream()
                .map(this::toGoalResponse).toList();
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        InvestmentGoal goal = InvestmentGoal.builder().user(user).name(request.name().trim())
                .targetAmount(request.targetAmount())
                .initialAmount(request.initialAmount() == null ? BigDecimal.ZERO : request.initialAmount())
                .monthlyContribution(request.monthlyContribution() == null ? BigDecimal.ZERO : request.monthlyContribution())
                .annualGrowthRate(request.annualGrowthRate() == null ? BigDecimal.ZERO : request.annualGrowthRate()).build();
        goal = goalRepository.save(goal);
        return toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest request) {
        InvestmentGoal goal = goalRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setInitialAmount(request.initialAmount() == null ? BigDecimal.ZERO : request.initialAmount());
        goal.setMonthlyContribution(request.monthlyContribution() == null ? BigDecimal.ZERO : request.monthlyContribution());
        goal.setAnnualGrowthRate(request.annualGrowthRate() == null ? BigDecimal.ZERO : request.annualGrowthRate());
        return toGoalResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse contributeToGoal(Long id, GoalContributionRequest request) {
        InvestmentGoal goal = goalRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        goalContributionRepository.save(InvestmentGoalContribution.builder().goal(goal)
                .amount(money(request.amount())).eventDate(request.eventDate()).build());
        return toGoalResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalContributionResponse> goalContributions(Long id) {
        InvestmentGoal goal = goalRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        return goalContributionRepository.findAllByGoalOrderByEventDateDescCreatedAtDesc(goal).stream()
                .map(contribution -> new GoalContributionResponse(contribution.getId(), contribution.getAmount(), contribution.getEventDate()))
                .toList();
    }

    @Transactional
    public void deleteGoalContribution(Long goalId, Long contributionId) {
        InvestmentGoal goal = goalRepository.findByIdAndUser(goalId, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        InvestmentGoalContribution contribution = goalContributionRepository.findByIdAndGoal(contributionId, goal)
                .orElseThrow(() -> new ResourceNotFoundException("Aporte da meta não encontrado"));
        goalContributionRepository.delete(contribution);
    }

    @Transactional(readOnly = true)
    public TaxSummaryResponse taxSummary(Integer requestedYear) {
        int year = requestedYear == null ? LocalDate.now().getYear() : requestedYear;
        User user = authenticatedUserService.getAuthenticatedUser();
        List<TaxEventResponse> events = new ArrayList<>();
        Set<String> scheduledReferences = new HashSet<>();

        for (InvestmentIncomeSchedule schedule : incomeScheduleRepository.findAllByUserOrderByPaymentDateAscCreatedAtDesc(user)) {
            if (schedule.getStatus() != InvestmentIncomeSchedule.Status.RECEBIDO || schedule.getPaymentDate().getYear() != year) continue;
            IncomeScheduleResponse income = toIncomeScheduleResponse(schedule);
            scheduledReferences.add("income-schedule:" + schedule.getId());
            TaxStatus status = income.taxAmount().signum() > 0 ? TaxStatus.RETIDO : TaxStatus.SEM_RETENCAO;
            String note = status == TaxStatus.RETIDO ? "Imposto informado como retido no provento." : "Nenhum imposto retido foi informado neste provento.";
            events.add(new TaxEventResponse(schedule.getPaymentDate(), schedule.getPosition().getSymbol(), schedule.getPosition().getName(),
                    schedule.getIncomeType().name(), schedule.getPosition().getCurrency(), income.grossAmount(), income.taxAmount(), income.netAmount(), status, note));
        }

        for (InvestmentMovement movement : movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)) {
            if (movement.getEventDate().getYear() != year || scheduledReferences.contains(movement.getExternalReference())) continue;
            InvestmentPosition position = movement.getPosition();
            if (isIncome(movement)) {
                events.add(new TaxEventResponse(movement.getEventDate(), position.getSymbol(), position.getName(), movement.getMovementType().name(), position.getCurrency(),
                        movement.getAmount(), ZERO, movement.getAmount(), TaxStatus.REVISAR,
                        "Provento manual: informe a retenção na agenda ou confirme a tributação no comprovante."));
            } else if (movement.getMovementType() == InvestmentMovement.MovementType.VENDA) {
                BigDecimal retained = movement.getCosts() == null ? ZERO : movement.getCosts().retention();
                BigDecimal fx = "BRL".equals(position.getCurrency()) ? BigDecimal.ONE : movement.getExchangeRate();
                String eventCurrency = fx == null ? position.getCurrency() : "BRL";
                BigDecimal factor = fx == null ? BigDecimal.ONE : fx;
                events.add(new TaxEventResponse(movement.getEventDate(), position.getSymbol(), position.getName(), "VENDA", eventCurrency,
                        money(movement.getQuantity().multiply(movement.getUnitPrice()).multiply(factor)),
                        money(retained.multiply(factor)), money(movement.getAmount().multiply(factor)), TaxStatus.REVISAR,
                        "IRRF informado e uma antecipacao. Venda requer apuracao mensal; o liquido tambem desconta custos operacionais."));
            } else if (movement.getMovementType() == InvestmentMovement.MovementType.RESGATE) {
                BigDecimal retained = movement.getCosts() == null ? ZERO : movement.getCosts().retention();
                events.add(new TaxEventResponse(movement.getEventDate(), position.getSymbol(), position.getName(), "RESGATE", position.getCurrency(),
                        money(movement.getAmount().add(retained)), retained, movement.getAmount(),
                        retained.signum() > 0 ? TaxStatus.RETIDO : TaxStatus.SEM_RETENCAO,
                        "IR e IOF estimados no resgate registrado. Confira os valores retidos no comprovante da instituicao."));
            }
        }
        events.sort(Comparator.comparing(TaxEventResponse::date).reversed());
        BigDecimal totalWithheld = events.stream().filter(event -> "BRL".equals(event.currency()))
                .map(TaxEventResponse::withheldAmount).reduce(ZERO, BigDecimal::add);
        int reviewCount = (int) events.stream().filter(event -> event.status() == TaxStatus.REVISAR).count();
        return new TaxSummaryResponse(year, money(totalWithheld), reviewCount, events);
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse reconciliation(Integer requestedYear) {
        int year = requestedYear == null ? LocalDate.now().getYear() : requestedYear;
        User user = authenticatedUserService.getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findAllByUserOrderByTransactionDateDesc(user);
        Set<Long> usedTransactionIds = new HashSet<>();
        List<ReconciliationItemResponse> items = new ArrayList<>();

        for (InvestmentMovement movement : movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(user)) {
            if (movement.getEventDate().getYear() != year) continue;
            Transaction.TransactionType expectedType = expectedTransactionType(movement.getMovementType());
            if (expectedType == null) continue;
            InvestmentPosition position = movement.getPosition();
            Transaction linked = transactions.stream()
                    .filter(transaction -> java.util.Objects.equals(transaction.getInvestmentMovementId(), movement.getId()))
                    .findFirst().orElse(null);
            if (linked != null) {
                usedTransactionIds.add(linked.getId());
                items.add(new ReconciliationItemResponse(movement.getId(), movement.getEventDate(), position.getSymbol(), position.getName(),
                        movement.getMovementType(), linked.getAmount(), "BRL", ReconciliationStatus.GERADO_PELO_FAROL,
                        linked.getId(), linked.getTransactionDate(), linked.getAmount(),
                        "Lancamento vinculado a operacao pelo Farol. Ainda nao representa conferencia com extrato externo."));
                continue;
            }
            if (!"BRL".equalsIgnoreCase(position.getCurrency())) {
                items.add(new ReconciliationItemResponse(movement.getId(), movement.getEventDate(), position.getSymbol(), position.getName(),
                        movement.getMovementType(), movement.getAmount(), position.getCurrency(), ReconciliationStatus.REVISAR,
                        null, null, null, "Conciliação automática disponível apenas para movimentos em reais."));
                continue;
            }
            Transaction match = transactions.stream().filter(transaction -> !usedTransactionIds.contains(transaction.getId()))
                    .filter(transaction -> transaction.getInvestmentMovementId() == null && transaction.getManagedReference() == null)
                    .filter(transaction -> transaction.getType() == expectedType)
                    .filter(transaction -> Math.abs(ChronoUnit.DAYS.between(movement.getEventDate(), transaction.getTransactionDate())) <= 3)
                    .filter(transaction -> transaction.getAmount().subtract(movement.getAmount()).abs().compareTo(new BigDecimal("0.02")) <= 0)
                    .min(Comparator.comparing((Transaction transaction) -> transaction.getAmount().subtract(movement.getAmount()).abs())
                            .thenComparing(transaction -> Math.abs(ChronoUnit.DAYS.between(movement.getEventDate(), transaction.getTransactionDate()))))
                    .orElse(null);
            if (match == null) {
                items.add(new ReconciliationItemResponse(movement.getId(), movement.getEventDate(), position.getSymbol(), position.getName(),
                        movement.getMovementType(), movement.getAmount(), position.getCurrency(), ReconciliationStatus.PENDENTE,
                        null, null, null, "Nenhum lançamento compatível foi encontrado no extrato importado."));
                continue;
            }
            usedTransactionIds.add(match.getId());
            boolean generated = java.util.Objects.equals(match.getInvestmentMovementId(), movement.getId())
                    || match.getDescription().startsWith("Dividendo -") || match.getDescription().startsWith("Rendimento -");
            items.add(new ReconciliationItemResponse(movement.getId(), movement.getEventDate(), position.getSymbol(), position.getName(),
                    movement.getMovementType(), movement.getAmount(), position.getCurrency(),
                    generated ? ReconciliationStatus.GERADO_PELO_FAROL : ReconciliationStatus.CONCILIADO,
                    match.getId(), match.getTransactionDate(), match.getAmount(),
                    generated ? "Receita criada pelo próprio Farol ao registrar o provento." : "Valor e data conferem com o extrato importado."));
        }
        items.sort(Comparator.comparing(ReconciliationItemResponse::eventDate).reversed());
        int reconciled = (int) items.stream().filter(item -> item.status() == ReconciliationStatus.CONCILIADO
                || item.status() == ReconciliationStatus.GERADO_PELO_FAROL).count();
        int pending = items.size() - reconciled;
        return new ReconciliationResponse(year, reconciled, pending, items);
    }

    private Transaction.TransactionType expectedTransactionType(InvestmentMovement.MovementType movementType) {
        return switch (movementType) {
            case COMPRA, APORTE -> Transaction.TransactionType.DESPESA;
            case VENDA, RESGATE, DIVIDENDO, RENDIMENTO -> Transaction.TransactionType.RECEITA;
            case SALDO_INICIAL -> null;
        };
    }

    @Transactional
    public void deleteGoal(Long id) {
        InvestmentGoal goal = goalRepository.findByIdAndUser(id, authenticatedUserService.getAuthenticatedUser())
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada"));
        goalRepository.delete(goal);
    }

    private MovementResponse recordIncome(User user, InvestmentPosition position, InvestmentMovement.MovementType movementType,
                                           BigDecimal amount, LocalDate eventDate, String externalReference) {
        if (!"BRL".equalsIgnoreCase(position.getCurrency()))
            throw new IllegalArgumentException("Proventos em moeda estrangeira precisam de conversao e retencao revisadas; use o financeiro para registrar o liquido em reais");
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder()
                .user(user).position(position).movementType(movementType).amount(money(amount))
                .eventDate(eventDate).automatic(false).externalReference(externalReference).build());
        transactionRepository.save(Transaction.builder()
                .user(user).type(Transaction.TransactionType.RECEITA)
                .description((movementType == InvestmentMovement.MovementType.DIVIDENDO ? "Dividendo - " : "Rendimento - ") + position.getName())
                .category(Transaction.TransactionCategory.INVESTIMENTO).amount(money(amount))
                .investmentMovementId(movement.getId())
                .paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA).installments(1).transactionDate(eventDate).build());
        return toMovementResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> movements() {
        return movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(authenticatedUserService.getAuthenticatedUser())
                .stream().map(this::toMovementResponse).toList();
    }

    public ProjectionResponse projection(BigDecimal initialAmount, BigDecimal monthlyContribution, BigDecimal interestRate,
                                         RatePeriod ratePeriod, TimelinePeriod timelinePeriod,
                                         LocalDate startDate, LocalDate endDate) {
        return projection(initialAmount, monthlyContribution, interestRate, ratePeriod, timelinePeriod, startDate, endDate,
                FixedIncomeTax.Regime.REGRESSIVO, null, true);
    }

    public ProjectionResponse projection(BigDecimal initialAmount, BigDecimal monthlyContribution, BigDecimal interestRate,
                                         RatePeriod ratePeriod, TimelinePeriod timelinePeriod, LocalDate startDate, LocalDate endDate,
                                         FixedIncomeTax.Regime regime, BigDecimal manualRate, boolean iofApplicable) {
        BigDecimal initial = initialAmount == null ? BigDecimal.ZERO : initialAmount;
        BigDecimal contribution = monthlyContribution == null ? BigDecimal.ZERO : monthlyContribution;
        if (initial.signum() < 0 || contribution.signum() < 0 || initial.add(contribution).signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor inicial ou um aporte mensal maior que zero");
        }
        BigDecimal rate = interestRate == null ? defaultAnnualRate : interestRate;
        RatePeriod selectedRatePeriod = ratePeriod == null ? RatePeriod.ANNUAL : ratePeriod;
        TimelinePeriod selectedTimelinePeriod = timelinePeriod == null ? TimelinePeriod.MONTHLY : timelinePeriod;
        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("1000")) > 0) {
            throw new IllegalArgumentException("A taxa informada é inválida");
        }
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start.plusYears(1) : endDate;
        if (!end.isAfter(start)) throw new IllegalArgumentException("A data final deve ser posterior à data inicial");

        return FixedIncomeProjection.calculate(initial, contribution, rate, selectedRatePeriod, selectedTimelinePeriod,
                start, end, regime, manualRate, iofApplicable);
    }

    public ProjectionResponse projection(BigDecimal principal, BigDecimal annualRate, LocalDate startDate, LocalDate maturityDate) {
        return projection(principal, BigDecimal.ZERO, annualRate, RatePeriod.ANNUAL, TimelinePeriod.MONTHLY,
                startDate, maturityDate);
    }

    private PositionResponse toResponse(InvestmentPosition position, BigDecimal income) {
        QuoteResponse quote = marketQuoteService.quote(position.getAssetType(), position.getSymbol(), position.getExternalId(), position.getMarket());
        BigDecimal invested = investedAmount(position);
        BigDecimal current;
        if (position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA) {
            LocalDate end = LocalDate.now().isAfter(position.getMaturityDate()) ? position.getMaturityDate() : LocalDate.now();
            long elapsedDays = Math.max(0, ChronoUnit.DAYS.between(position.getPurchaseDate(), end));
            double growthFactor = Math.pow(1 + position.getAnnualRate().doubleValue() / 100.0, elapsedDays / 365.0);
            current = position.getPrincipal().multiply(BigDecimal.valueOf(growthFactor));
        } else {
            BigDecimal unitPrice = quote.available() && quote.price() != null ? quote.price() : position.getAveragePrice();
            String currentCurrency = quote.available() ? quote.currency() : position.getCurrency();
            current = toBrl(position.getQuantity().multiply(unitPrice), currentCurrency);
        }
        BigDecimal capitalGain = current.subtract(invested);
        BigDecimal totalReturn = capitalGain.add(income);
        return new PositionResponse(position.getId(), position.getAssetType(), position.getSymbol(), position.getExternalId(),
                position.getName(), position.getMarket(), position.getExchange(), position.getCurrency(),
                position.getQuantity(), position.getAveragePrice(), position.getPrincipal(), position.getAnnualRate(),
                position.getPurchaseDate(), position.getMaturityDate(), money(invested), money(current), money(capitalGain),
                percentage(capitalGain, invested), money(income), money(totalReturn), percentage(totalReturn, invested), quote,
                position.getTaxRegime(), position.getManualTaxRate(), position.isIofApplicable(), position.getOpeningDate());
    }

    private IncomeScheduleResponse toIncomeScheduleResponse(InvestmentIncomeSchedule schedule) {
        InvestmentPosition position = schedule.getPosition();
        BigDecimal quantity = eligibleQuantity(position, schedule.getExDate());
        BigDecimal gross = quantity.multiply(schedule.getAmountPerUnit());
        BigDecimal tax = gross.multiply(schedule.getTaxRate()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        return new IncomeScheduleResponse(schedule.getId(), position.getId(), position.getSymbol(), position.getName(),
                schedule.getIncomeType(), schedule.getAmountPerUnit(), quantity, money(gross), schedule.getTaxRate(),
                money(tax), money(gross.subtract(tax)), schedule.getExDate(), schedule.getPaymentDate(), schedule.getStatus());
    }

    private BigDecimal eligibleQuantity(InvestmentPosition position, LocalDate exDate) {
        if (exDate == null) return position.getQuantity() == null ? BigDecimal.ONE : position.getQuantity();
        List<InvestmentMovement> movements = movementRepository.findAllByUserOrderByEventDateDescCreatedAtDesc(position.getUser());
        BigDecimal quantity = movements.stream().filter(movement -> movement.getPosition().getId().equals(position.getId()))
                .filter(movement -> !movement.getEventDate().isAfter(exDate))
                .map(movement -> switch (movement.getMovementType()) {
                    case COMPRA, SALDO_INICIAL -> movement.getQuantity() == null ? BigDecimal.ZERO : movement.getQuantity();
                    case VENDA -> movement.getQuantity() == null ? BigDecimal.ZERO : movement.getQuantity().negate();
                    default -> BigDecimal.ZERO;
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (quantity.signum() == 0 && position.getPurchaseDate().isAfter(exDate)) return BigDecimal.ZERO;
        return quantity.signum() == 0 ? (position.getQuantity() == null ? BigDecimal.ONE : position.getQuantity()) : quantity;
    }

    private GoalResponse toGoalResponse(InvestmentGoal goal) {
        BigDecimal contributions = goalContributionRepository.findAllByGoalOrderByEventDateDescCreatedAtDesc(goal).stream()
                .map(InvestmentGoalContribution::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal initialAmount = goal.getInitialAmount() == null ? BigDecimal.ZERO : goal.getInitialAmount();
        BigDecimal currentAmount = initialAmount.add(contributions);
        BigDecimal remaining = goal.getTargetAmount().subtract(currentAmount).max(BigDecimal.ZERO);
        boolean achieved = remaining.signum() == 0;
        BigDecimal progress = goal.getTargetAmount().signum() == 0 ? BigDecimal.ZERO
                : currentAmount.multiply(BigDecimal.valueOf(100)).divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));
        return new GoalResponse(goal.getId(), goal.getName(), money(goal.getTargetAmount()), money(initialAmount),
                money(contributions), money(currentAmount), money(remaining), progress, money(goal.getMonthlyContribution()), goal.getAnnualGrowthRate(),
                achieved ? 0 : monthsToGoal(currentAmount, goal.getTargetAmount(), goal.getMonthlyContribution(), goal.getAnnualGrowthRate()), achieved);
    }

    private Integer monthsToGoal(BigDecimal current, BigDecimal target, BigDecimal contribution, BigDecimal annualRate) {
        double balance = Math.max(0, current.doubleValue());
        double targetValue = target.doubleValue();
        double monthlyRate = Math.pow(1 + annualRate.doubleValue() / 100D, 1D / 12D) - 1D;
        for (int month = 1; month <= 1200; month++) {
            balance = balance * (1 + monthlyRate) + contribution.doubleValue();
            if (balance >= targetValue) return month;
        }
        return null;
    }

    private void saveDailySnapshot(User user, BigDecimal invested, BigDecimal current, BigDecimal income) {
        LocalDate today = LocalDate.now();
        InvestmentPortfolioSnapshot snapshot = snapshotRepository.findByUserAndSnapshotDate(user, today)
                .orElseGet(() -> InvestmentPortfolioSnapshot.builder().user(user).snapshotDate(today).build());
        snapshot.setInvestedAmount(money(invested));
        snapshot.setCurrentValue(money(current));
        snapshot.setIncomeAmount(money(income));
        snapshotRepository.save(snapshot);
    }

    private boolean isIncome(InvestmentMovement movement) {
        return movement.getMovementType() == InvestmentMovement.MovementType.DIVIDENDO
                || movement.getMovementType() == InvestmentMovement.MovementType.RENDIMENTO;
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        return base.signum() == 0 ? ZERO : amount.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal investedAmount(InvestmentPosition position) {
        return position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA
                ? position.getPrincipal()
                : toBrl(position.getQuantity().multiply(position.getAveragePrice()), position.getCurrency());
    }

    private BigDecimal toBrl(BigDecimal value, String currency) {
        return value.multiply(marketQuoteService.exchangeRateToBrl(currency));
    }

    private void validate(PositionRequest request) {
        if (request.assetType() == InvestmentPosition.AssetType.RENDA_FIXA) {
            if (request.principal() == null || request.annualRate() == null || request.maturityDate() == null)
                throw new IllegalArgumentException("Renda fixa exige valor aplicado, taxa anual e vencimento");
            if (!request.maturityDate().isAfter(request.purchaseDate()))
                throw new IllegalArgumentException("O vencimento deve ser posterior à aplicação");
        } else {
            if (request.quantity() == null || request.averagePrice() == null)
                throw new IllegalArgumentException("Quantidade e preço médio são obrigatórios");
            if (request.assetType() == InvestmentPosition.AssetType.CRIPTO && blank(request.externalId()))
                throw new IllegalArgumentException("Informe o identificador CoinGecko, como bitcoin");
            if (request.assetType() != InvestmentPosition.AssetType.CRIPTO && blank(request.symbol()))
                throw new IllegalArgumentException("Informe o ticker do ativo");
        }
    }

    private void apply(InvestmentPosition target, PositionRequest request) {
        target.setAssetType(request.assetType());
        target.setSymbol(blank(request.symbol()) ? null : request.symbol().trim().toUpperCase(Locale.ROOT));
        target.setExternalId(blank(request.externalId()) ? null : request.externalId().trim().toLowerCase(Locale.ROOT));
        target.setName(request.name().trim());
        target.setMarket(normalizeUpper(request.market(), "BR"));
        target.setExchange(blank(request.exchange()) ? null : request.exchange().trim());
        target.setCurrency(normalizeUpper(request.currency(), "BRL"));
        target.setQuantity(request.quantity());
        target.setAveragePrice(request.averagePrice());
        target.setPrincipal(request.principal());
        target.setAnnualRate(request.annualRate());
        target.setPurchaseDate(request.purchaseDate());
        target.setMaturityDate(request.maturityDate());
        target.setTaxRegime(request.taxRegime());
        target.setManualTaxRate(request.manualTaxRate());
        target.setIofApplicable(Boolean.TRUE.equals(request.iofApplicable()));
        target.setOpeningDate(request.openingDate());
    }

    private InvestmentPosition owned(Long id) {
        User user = authenticatedUserService.getAuthenticatedUser();
        return repository.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
    }

    private MovementResponse toMovementResponse(InvestmentMovement movement) {
        return new MovementResponse(movement.getId(), movement.getPosition().getId(), movement.getPosition().getName(),
                movement.getMovementType(), movement.getAmount(), movement.getQuantity(), movement.getUnitPrice(), movement.getFees(),
                movement.getPosition().getCurrency(), movement.getEventDate(), movement.isAutomatic(),
                movement.getRealizedGain(), movement.getCosts(), movement.getExchangeRate());
    }

    private void recordCashFlow(InvestmentMovement movement, BigDecimal fx) {
        transactionRepository.save(Transaction.builder().user(movement.getUser()).investmentMovementId(movement.getId())
                .type(expectedTransactionType(movement.getMovementType())).category(Transaction.TransactionCategory.INVESTIMENTO)
                .description(movement.getMovementType().name() + " - " + movement.getPosition().getName())
                .amount(money(movement.getAmount().multiply(fx))).paymentMethod(Transaction.PaymentMethod.TRANSFERENCIA)
                .installments(1).transactionDate(movement.getEventDate()).build());
    }

    @Transactional
    public FixedIncomeTax.Result redeem(Long id, RedemptionRequest request, boolean confirm) {
        entityManager.lock(authenticatedUserService.getAuthenticatedUser(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        InvestmentPosition position = owned(id);
        if (position.getAssetType() != InvestmentPosition.AssetType.RENDA_FIXA || position.isRedeemed())
            throw new IllegalArgumentException("Selecione uma aplicacao de renda fixa ativa");
        var result = FixedIncomeTax.calculate(position.getPrincipal(), request.grossAmount(), position.getPurchaseDate(),
                request.eventDate(), request.taxRegime(), request.manualTaxRate(), request.iofApplicable());
        if (confirm) {
            if (request.eventDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("Confirme o resgate somente apos o recebimento");
            InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder().user(position.getUser()).position(position)
                    .movementType(InvestmentMovement.MovementType.RESGATE).amount(result.net()).costBasis(position.getPrincipal())
                    .realizedGain(request.grossAmount().subtract(position.getPrincipal()))
                    .costs(new OperationCosts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, result.incomeTax().add(result.iof())))
                    .eventDate(request.eventDate()).exchangeRate(BigDecimal.ONE).build());
            position.setRedeemed(true);
            repository.save(position);
            recordCashFlow(movement, BigDecimal.ONE);
        }
        return result;
    }

    private java.util.Optional<InvestmentPosition> findPosition(User user, InvestmentPosition.AssetType type,
                                                                 String market, String symbol, String externalId) {
        if (type == InvestmentPosition.AssetType.CRIPTO) {
            return repository.findFirstByUserAndAssetTypeAndExternalIdIgnoreCase(user, type, externalId);
        }
        return repository.findFirstByUserAndAssetTypeAndMarketAndSymbolIgnoreCase(user, type, market, symbol);
    }

    private void validateTradeIdentity(InvestmentPosition.AssetType type, String symbol, String externalId, String market) {
        if (!List.of("BR", "US", "GLOBAL").contains(market)) throw new IllegalArgumentException("Mercado não suportado nesta versão");
        if (type == InvestmentPosition.AssetType.CRIPTO && blank(externalId)) throw new IllegalArgumentException("Criptoativo sem identificador de catálogo");
        if (type != InvestmentPosition.AssetType.CRIPTO && blank(symbol)) throw new IllegalArgumentException("Ativo sem ticker de catálogo");
        if (!blank(symbol) && !symbol.matches("[-A-Z0-9.]{1,30}")) throw new IllegalArgumentException("Ticker do ativo é inválido");
    }

    private String normalizeUpper(String value, String fallback) {
        return blank(value) ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
