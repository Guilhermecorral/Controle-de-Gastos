package com.controledegastos.backend.monthlyanalysis;

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO;
import com.controledegastos.backend.monthlyanalysis.dto.AnalysisTrend;
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyAnalysisResponseDTO;
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyComparisonDTO;
import com.controledegastos.backend.monthlyanalysis.dto.MonthlyHighestExpenseDTO;
import com.controledegastos.backend.monthlyanalysis.dto.YearToDateComparisonDTO;
import com.controledegastos.backend.monthlyanalysis.dto.YearToDateSummaryDTO;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.transactions.Repository.TransactionCategorySummaryProjection;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
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
public class MonthlyAnalysisService {

    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional(readOnly = true)
    public MonthlyAnalysisResponseDTO getMonthlyAnalysis(int year, int month) {
        YearMonth requestedPeriod = buildYearMonth(year, month);
        YearMonth previousPeriod = requestedPeriod.minusMonths(1);
        YearMonth sameMonthLastYearPeriod = requestedPeriod.minusYears(1);

        LocalDate requestedStartDate = requestedPeriod.atDay(1);
        LocalDate requestedEndDate = requestedPeriod.atEndOfMonth();
        LocalDate previousStartDate = previousPeriod.atDay(1);
        LocalDate previousEndDate = previousPeriod.atEndOfMonth();
        LocalDate sameMonthLastYearStartDate = sameMonthLastYearPeriod.atDay(1);
        LocalDate sameMonthLastYearEndDate = sameMonthLastYearPeriod.atEndOfMonth();
        LocalDate currentYearStartDate = requestedPeriod.atDay(1).withDayOfYear(1);
        LocalDate previousYearStartDate = sameMonthLastYearPeriod.atDay(1).withDayOfYear(1);

        User user = authenticatedUserService.getAuthenticatedUser();

        BigDecimal totalReceitas = sumByType(user, Transaction.TransactionType.RECEITA, requestedStartDate, requestedEndDate);
        BigDecimal totalDespesas = sumByType(user, Transaction.TransactionType.DESPESA, requestedStartDate, requestedEndDate);
        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        MonthlyHighestExpenseDTO maiorGasto = findHighestExpense(user, requestedStartDate, requestedEndDate);
        List<DashboardCategorySummaryDTO> gastosPorCategoria = findGroupedExpenses(user, requestedStartDate, requestedEndDate);

        MonthlyComparisonDTO comparativoMesAnterior = buildMonthlyComparison(
                user,
                previousPeriod,
                previousStartDate,
                previousEndDate,
                totalReceitas,
                totalDespesas,
                saldo
        );

        MonthlyComparisonDTO comparativoMesmoMesAnoAnterior = buildMonthlyComparison(
                user,
                sameMonthLastYearPeriod,
                sameMonthLastYearStartDate,
                sameMonthLastYearEndDate,
                totalReceitas,
                totalDespesas,
                saldo
        );

        YearToDateSummaryDTO acumuladoAnoAtual = buildYearToDateSummary(
                user,
                requestedPeriod.getYear(),
                requestedPeriod.getMonthValue(),
                currentYearStartDate,
                requestedEndDate
        );

        YearToDateComparisonDTO comparativoAcumuladoAnoAnterior = buildYearToDateComparison(
                user,
                acumuladoAnoAtual,
                sameMonthLastYearPeriod.getYear(),
                requestedPeriod.getMonthValue(),
                previousYearStartDate,
                sameMonthLastYearEndDate
        );

        return new MonthlyAnalysisResponseDTO(
                requestedPeriod.getYear(),
                requestedPeriod.getMonthValue(),
                totalReceitas,
                totalDespesas,
                saldo,
                maiorGasto,
                gastosPorCategoria,
                comparativoMesAnterior,
                comparativoMesmoMesAnoAnterior,
                acumuladoAnoAtual,
                comparativoAcumuladoAnoAnterior
        );
    }

    private YearMonth buildYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid year or month informed for monthly analysis", exception);
        }
    }

    private BigDecimal sumByType(
            User user,
            Transaction.TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return transactionRepository.sumAmountByUserAndTypeAndTransactionDateBetween(user, type, startDate, endDate);
    }

    private List<DashboardCategorySummaryDTO> findGroupedExpenses(
            User user,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return transactionRepository.findExpenseSummaryByCategoryAndTransactionDateBetween(user, startDate, endDate)
                .stream()
                .map(this::toCategorySummaryDTO)
                .toList();
    }

    private DashboardCategorySummaryDTO toCategorySummaryDTO(TransactionCategorySummaryProjection projection) {
        return new DashboardCategorySummaryDTO(projection.getCategory(), projection.getTotalAmount());
    }

    private MonthlyHighestExpenseDTO findHighestExpense(
            User user,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return transactionRepository
                .findTopByUserAndTypeAndTransactionDateBetweenOrderByAmountDescTransactionDateDescCreatedAtDesc(
                        user,
                        Transaction.TransactionType.DESPESA,
                        startDate,
                        endDate
                )
                .map(this::toHighestExpenseDTO)
                .orElse(null);
    }

    private MonthlyHighestExpenseDTO toHighestExpenseDTO(Transaction transaction) {
        return new MonthlyHighestExpenseDTO(
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getTransactionDate()
        );
    }

    private MonthlyComparisonDTO buildMonthlyComparison(
            User user,
            YearMonth comparedPeriod,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal selectedReceitas,
            BigDecimal selectedDespesas,
            BigDecimal selectedSaldo
    ) {
        BigDecimal comparedReceitas = sumByType(user, Transaction.TransactionType.RECEITA, startDate, endDate);
        BigDecimal comparedDespesas = sumByType(user, Transaction.TransactionType.DESPESA, startDate, endDate);
        BigDecimal comparedSaldo = comparedReceitas.subtract(comparedDespesas);

        BigDecimal diferencaReceitas = selectedReceitas.subtract(comparedReceitas);
        BigDecimal diferencaDespesas = selectedDespesas.subtract(comparedDespesas);
        BigDecimal diferencaSaldo = selectedSaldo.subtract(comparedSaldo);

        AnalysisTrend tendenciaReceitas = evaluateIncomeTrend(diferencaReceitas);
        AnalysisTrend tendenciaDespesas = evaluateExpenseTrend(diferencaDespesas);
        AnalysisTrend tendenciaSaldo = evaluateBalanceTrend(diferencaSaldo);

        return new MonthlyComparisonDTO(
                comparedPeriod.getYear(),
                comparedPeriod.getMonthValue(),
                comparedReceitas,
                comparedDespesas,
                comparedSaldo,
                diferencaReceitas,
                diferencaDespesas,
                diferencaSaldo,
                tendenciaReceitas,
                tendenciaDespesas,
                tendenciaSaldo,
                tendenciaSaldo
        );
    }

    private YearToDateSummaryDTO buildYearToDateSummary(
            User user,
            int year,
            int monthLimit,
            LocalDate startDate,
            LocalDate endDate
    ) {
        BigDecimal totalReceitas = sumByType(user, Transaction.TransactionType.RECEITA, startDate, endDate);
        BigDecimal totalDespesas = sumByType(user, Transaction.TransactionType.DESPESA, startDate, endDate);
        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        return new YearToDateSummaryDTO(year, monthLimit, totalReceitas, totalDespesas, saldo);
    }

    private YearToDateComparisonDTO buildYearToDateComparison(
            User user,
            YearToDateSummaryDTO currentYearSummary,
            int previousYear,
            int monthLimit,
            LocalDate previousYearStartDate,
            LocalDate previousYearEndDate
    ) {
        YearToDateSummaryDTO previousYearSummary = buildYearToDateSummary(
                user,
                previousYear,
                monthLimit,
                previousYearStartDate,
                previousYearEndDate
        );

        BigDecimal diferencaReceitas = currentYearSummary.totalReceitas().subtract(previousYearSummary.totalReceitas());
        BigDecimal diferencaDespesas = currentYearSummary.totalDespesas().subtract(previousYearSummary.totalDespesas());
        BigDecimal diferencaSaldo = currentYearSummary.saldo().subtract(previousYearSummary.saldo());

        AnalysisTrend tendenciaReceitas = evaluateIncomeTrend(diferencaReceitas);
        AnalysisTrend tendenciaDespesas = evaluateExpenseTrend(diferencaDespesas);
        AnalysisTrend tendenciaSaldo = evaluateBalanceTrend(diferencaSaldo);

        return new YearToDateComparisonDTO(
                currentYearSummary,
                previousYearSummary,
                diferencaReceitas,
                diferencaDespesas,
                diferencaSaldo,
                tendenciaReceitas,
                tendenciaDespesas,
                tendenciaSaldo,
                tendenciaSaldo
        );
    }

    private AnalysisTrend evaluateIncomeTrend(BigDecimal difference) {
        int comparison = difference.compareTo(BigDecimal.ZERO);

        if (comparison > 0) {
            return AnalysisTrend.MELHOR;
        }

        if (comparison < 0) {
            return AnalysisTrend.PIOR;
        }

        return AnalysisTrend.IGUAL;
    }

    private AnalysisTrend evaluateExpenseTrend(BigDecimal difference) {
        int comparison = difference.compareTo(BigDecimal.ZERO);

        if (comparison < 0) {
            return AnalysisTrend.MELHOR;
        }

        if (comparison > 0) {
            return AnalysisTrend.PIOR;
        }

        return AnalysisTrend.IGUAL;
    }

    private AnalysisTrend evaluateBalanceTrend(BigDecimal difference) {
        int comparison = difference.compareTo(BigDecimal.ZERO);

        if (comparison > 0) {
            return AnalysisTrend.MELHOR;
        }

        if (comparison < 0) {
            return AnalysisTrend.PIOR;
        }

        return AnalysisTrend.IGUAL;
    }
}
