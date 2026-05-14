package com.controledegastos.backend.dashboard;

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO;
import com.controledegastos.backend.dashboard.dto.DashboardResponseDTO;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.TransactionCategorySummaryProjection;
import com.controledegastos.backend.transactions.TransactionRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    private User getAuthenticatedUser() {
        return authenticatedUserService.getAuthenticatedUser();
    }

    private TransactionResponseDTO toTransactionResponseDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }

    private DashboardCategorySummaryDTO toCategorySummaryDTO(TransactionCategorySummaryProjection projection) {
        return new DashboardCategorySummaryDTO(
                projection.getCategory(),
                projection.getTotalAmount()
        );
    }

    private YearMonth resolveReferencePeriod(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.now();
        }

        if (year == null || month == null) {
            throw new IllegalArgumentException(
                    "Both year and month must be informed together for the dashboard reference period"
            );
        }

        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid year or month informed for dashboard reference period", exception);
        }
    }

    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboard(Integer year, Integer month) {
        User user = getAuthenticatedUser();
        YearMonth referencePeriod = resolveReferencePeriod(year, month);

        LocalDate monthStart = referencePeriod.atDay(1);
        LocalDate monthEnd = referencePeriod.atEndOfMonth();
        LocalDate yearStart = monthStart.withDayOfYear(1);

        BigDecimal totalReceitas = transactionRepository.sumAmountByUserAndTypeUpToDate(
                user,
                Transaction.TransactionType.RECEITA,
                monthEnd
        );
        BigDecimal totalDespesas = transactionRepository.sumAmountByUserAndTypeUpToDate(
                user,
                Transaction.TransactionType.DESPESA,
                monthEnd
        );
        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        BigDecimal receitasAnoReferencia = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween(
                user,
                Transaction.TransactionType.RECEITA,
                yearStart,
                monthEnd
        );
        BigDecimal despesasAnoReferencia = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween(
                user,
                Transaction.TransactionType.DESPESA,
                yearStart,
                monthEnd
        );
        BigDecimal resultadoAnoReferencia = receitasAnoReferencia.subtract(despesasAnoReferencia);

        BigDecimal receitasMesAtual = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween(
                user,
                Transaction.TransactionType.RECEITA,
                monthStart,
                monthEnd
        );
        BigDecimal despesasMesAtual = transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween(
                user,
                Transaction.TransactionType.DESPESA,
                monthStart,
                monthEnd
        );
        BigDecimal resultadoMesAtual = receitasMesAtual.subtract(despesasMesAtual);

        List<TransactionResponseDTO> ultimasTransacoes = transactionRepository
                .findTop5ByUserAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(user, monthStart, monthEnd)
                .stream()
                .map(this::toTransactionResponseDTO)
                .toList();

        List<DashboardCategorySummaryDTO> gastosPorCategoria = transactionRepository
                .findExpenseSummaryByCategoryAndTransactionDateBetween(user, monthStart, monthEnd)
                .stream()
                .map(this::toCategorySummaryDTO)
                .toList();

        return new DashboardResponseDTO(
                totalReceitas,
                totalDespesas,
                saldo,
                totalReceitas,
                totalDespesas,
                saldo,
                receitasAnoReferencia,
                despesasAnoReferencia,
                resultadoAnoReferencia,
                receitasMesAtual,
                despesasMesAtual,
                resultadoMesAtual,
                referencePeriod.getYear(),
                referencePeriod.getMonthValue(),
                ultimasTransacoes,
                gastosPorCategoria
        );
    }
}
