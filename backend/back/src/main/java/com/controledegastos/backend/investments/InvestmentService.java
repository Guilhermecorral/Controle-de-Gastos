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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        apply(position, request);
        return toResponse(repository.save(position), ZERO);
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        validate(request);
        InvestmentPosition position = owned(id);
        apply(position, request);
        return toResponse(repository.save(position), ZERO);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(owned(id));
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
        BigDecimal fees = request.fees() == null ? BigDecimal.ZERO : request.fees();
        String market = normalizeUpper(request.market(), "BR");
        String symbol = normalizeUpper(request.symbol(), null);
        String externalId = blank(request.externalId()) ? null : request.externalId().trim();
        validateTradeIdentity(request.assetType(), symbol, externalId, market);

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
            if (fees.compareTo(gross) >= 0) throw new IllegalArgumentException("Os custos da venda devem ser menores que o valor bruto");
            BigDecimal remaining = currentQuantity.subtract(request.quantity());
            position.setQuantity(remaining);
            if (remaining.signum() == 0) position.setAveragePrice(BigDecimal.ZERO);
        }

        InvestmentPosition saved = repository.save(position);
        BigDecimal amount = request.movementType() == InvestmentMovement.MovementType.COMPRA
                ? gross.add(fees) : gross.subtract(fees);
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder()
                .user(user)
                .position(saved)
                .movementType(request.movementType())
                .amount(money(amount))
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .fees(money(fees))
                .eventDate(request.eventDate())
                .automatic(false)
                .build());
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
        InvestmentMovement movement = movementRepository.save(InvestmentMovement.builder()
                .user(user).position(position).movementType(request.movementType()).amount(request.amount())
                .eventDate(request.eventDate()).automatic(false).build());
        transactionRepository.save(Transaction.builder()
                .user(user).type(Transaction.TransactionType.RECEITA)
                .description((request.movementType() == InvestmentMovement.MovementType.DIVIDENDO ? "Dividendo - " : "Rendimento - ") + position.getName())
                .category(Transaction.TransactionCategory.OUTROS).amount(request.amount())
                .paymentMethod(Transaction.PaymentMethod.PIX).installments(1).transactionDate(request.eventDate()).build());
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

        int months = Math.max(1, (int) ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)));
        if (months > 1200) throw new IllegalArgumentException("O período máximo da simulação é de 100 anos");
        double monthlyRateValue = selectedRatePeriod == RatePeriod.MONTHLY
                ? rate.doubleValue() / 100.0
                : Math.pow(1 + rate.doubleValue() / 100.0, 1.0 / 12.0) - 1;
        BigDecimal monthlyRate = BigDecimal.valueOf(monthlyRateValue);
        List<ProjectionPoint> timeline = new ArrayList<>();
        BigDecimal balance = initial;
        BigDecimal totalInvested = initial;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal periodInterest = BigDecimal.ZERO;
        for (int month = 1; month <= months; month++) {
            BigDecimal monthInterest = balance.multiply(monthlyRate);
            totalInterest = totalInterest.add(monthInterest);
            periodInterest = periodInterest.add(monthInterest);
            balance = balance.add(monthInterest).add(contribution);
            totalInvested = totalInvested.add(contribution);
            if (selectedTimelinePeriod == TimelinePeriod.MONTHLY || month % 12 == 0 || month == months) {
                timeline.add(new ProjectionPoint(month, start.plusMonths(month), money(contribution), money(periodInterest),
                        money(totalInvested), money(totalInterest), money(balance)));
                periodInterest = BigDecimal.ZERO;
            }
        }
        return new ProjectionResponse(money(initial), money(contribution), rate.setScale(4, RoundingMode.HALF_UP),
                selectedRatePeriod, selectedTimelinePeriod,
                monthlyRate.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP), money(totalInvested),
                money(balance), money(totalInterest), months, timeline,
                "Aportes são considerados no fim de cada mês. Simulação educacional com taxa constante, sem impostos ou inflação.");
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
                percentage(capitalGain, invested), money(income), money(totalReturn), percentage(totalReturn, invested), quote);
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
    }

    private InvestmentPosition owned(Long id) {
        User user = authenticatedUserService.getAuthenticatedUser();
        return repository.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
    }

    private MovementResponse toMovementResponse(InvestmentMovement movement) {
        return new MovementResponse(movement.getId(), movement.getPosition().getId(), movement.getPosition().getName(),
                movement.getMovementType(), movement.getAmount(), movement.getQuantity(), movement.getUnitPrice(), movement.getFees(),
                movement.getPosition().getCurrency(), movement.getEventDate(), movement.isAutomatic());
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
