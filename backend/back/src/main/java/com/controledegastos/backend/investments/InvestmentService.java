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

@Service
@RequiredArgsConstructor
public class InvestmentService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final InvestmentPositionRepository repository;
    private final AuthenticatedUserService authenticatedUserService;
    private final MarketQuoteService marketQuoteService;
    private final InvestmentMovementRepository movementRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.investments.default-annual-rate:12.0}")
    private BigDecimal defaultAnnualRate;

    @Transactional(readOnly = true)
    public PortfolioResponse portfolio() {
        User user = authenticatedUserService.getAuthenticatedUser();
        List<PositionResponse> positions = repository.findAllByUserOrderByCreatedAtDesc(user).stream().map(this::toResponse).toList();
        BigDecimal invested = positions.stream().map(PositionResponse::investedAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal current = positions.stream().map(PositionResponse::currentValue).reduce(ZERO, BigDecimal::add);
        return new PortfolioResponse(money(invested), money(current), money(current.subtract(invested)), positions);
    }

    @Transactional
    public PositionResponse create(PositionRequest request) {
        validate(request);
        InvestmentPosition position = new InvestmentPosition();
        position.setUser(authenticatedUserService.getAuthenticatedUser());
        apply(position, request);
        return toResponse(repository.save(position));
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        validate(request);
        InvestmentPosition position = owned(id);
        apply(position, request);
        return toResponse(repository.save(position));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(owned(id));
    }

    public QuoteResponse quote(InvestmentPosition.AssetType type, String symbol, String externalId) {
        return marketQuoteService.quote(type, symbol, externalId);
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

    public ProjectionResponse projection(BigDecimal principal, BigDecimal annualRate, LocalDate startDate, LocalDate maturityDate) {
        if (principal == null || principal.signum() <= 0) throw new IllegalArgumentException("O valor inicial deve ser maior que zero");
        BigDecimal rate = annualRate == null ? defaultAnnualRate : annualRate;
        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("1000")) > 0) throw new IllegalArgumentException("A taxa anual informada é inválida");
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = maturityDate == null ? start.plusYears(1) : maturityDate;
        if (!end.isAfter(start)) throw new IllegalArgumentException("O vencimento deve ser posterior à data inicial");

        int months = Math.max(1, (int) ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)));
        double monthlyRate = Math.pow(1 + rate.doubleValue() / 100.0, 1.0 / 12.0) - 1;
        List<ProjectionPoint> timeline = new ArrayList<>();
        for (int month = 1; month <= months; month++) {
            BigDecimal balance = BigDecimal.valueOf(principal.doubleValue() * Math.pow(1 + monthlyRate, month));
            timeline.add(new ProjectionPoint(month, start.plusMonths(month), money(balance), money(balance.subtract(principal))));
        }
        BigDecimal projected = timeline.get(timeline.size() - 1).balance();
        return new ProjectionResponse(money(principal), rate.setScale(2, RoundingMode.HALF_UP), projected,
                money(projected.subtract(principal)), months, timeline,
                "Simulação educacional com juros compostos e taxa constante; não representa garantia de rentabilidade.");
    }

    private PositionResponse toResponse(InvestmentPosition position) {
        BigDecimal invested = investedAmount(position);
        QuoteResponse quote = marketQuoteService.quote(position.getAssetType(), position.getSymbol(), position.getExternalId());
        BigDecimal current;
        if (position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA) {
            LocalDate end = LocalDate.now().isAfter(position.getMaturityDate()) ? position.getMaturityDate() : LocalDate.now();
            current = projection(position.getPrincipal(), position.getAnnualRate(), position.getPurchaseDate(), end.isAfter(position.getPurchaseDate()) ? end : position.getPurchaseDate().plusDays(1)).projectedBalance();
        } else {
            BigDecimal unitPrice = quote.available() && quote.price() != null ? quote.price() : position.getAveragePrice();
            current = position.getQuantity().multiply(unitPrice);
        }
        return new PositionResponse(position.getId(), position.getAssetType(), position.getSymbol(), position.getExternalId(),
                position.getName(), position.getQuantity(), position.getAveragePrice(), position.getPrincipal(), position.getAnnualRate(),
                position.getPurchaseDate(), position.getMaturityDate(), money(invested), money(current), money(current.subtract(invested)), quote);
    }

    private BigDecimal investedAmount(InvestmentPosition position) {
        return position.getAssetType() == InvestmentPosition.AssetType.RENDA_FIXA
                ? position.getPrincipal()
                : position.getQuantity().multiply(position.getAveragePrice());
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
                movement.getMovementType(), movement.getAmount(), movement.getEventDate(), movement.isAutomatic());
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
