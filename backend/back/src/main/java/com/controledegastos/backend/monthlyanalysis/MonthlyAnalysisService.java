package com.controledegastos.backend.monthlyanalysis;

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO;
import com.controledegastos.backend.monthlyanalysis.dto.AnalysisTrend;
import com.controledegastos.backend.monthlyanalysis.dto.FinancialInsightDTO;
import com.controledegastos.backend.monthlyanalysis.dto.FinancialInsightSeverity;
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
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
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
        List<DashboardCategorySummaryDTO> receitasPorCategoria = findGroupedTransactionsByType(
                user,
                Transaction.TransactionType.RECEITA,
                requestedStartDate,
                requestedEndDate
        );
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

        BigDecimal ordinaryIncome = transactionRepository.sumOrdinaryByType(user, Transaction.TransactionType.RECEITA, requestedStartDate, requestedEndDate);
        BigDecimal ordinaryExpense = transactionRepository.sumOrdinaryByType(user, Transaction.TransactionType.DESPESA, requestedStartDate, requestedEndDate);
        List<FinancialInsightDTO> insights = buildInsights(
                ordinaryIncome,
                ordinaryExpense,
                saldo,
                gastosPorCategoria,
                transactionRepository.sumOrdinaryByType(user, Transaction.TransactionType.DESPESA, previousStartDate, previousEndDate)
        );

        return new MonthlyAnalysisResponseDTO(
                requestedPeriod.getYear(),
                requestedPeriod.getMonthValue(),
                totalReceitas,
                totalDespesas,
                saldo,
                maiorGasto,
                receitasPorCategoria,
                gastosPorCategoria,
                comparativoMesAnterior,
                comparativoMesmoMesAnoAnterior,
                acumuladoAnoAtual,
                comparativoAcumuladoAnoAnterior,
                insights
        );
    }

    private List<FinancialInsightDTO> buildInsights(
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal balance,
            List<DashboardCategorySummaryDTO> expensesByCategory,
            BigDecimal previousExpenses
    ) {
        List<FinancialInsightDTO> insights = new ArrayList<>();

        if (income.signum() == 0 && expenses.signum() == 0) {
            return List.of(new FinancialInsightDTO(
                    "NO_DATA",
                    FinancialInsightSeverity.NEUTRO,
                    "Registre o mês para receber uma leitura útil",
                    "Ainda não há movimentações suficientes para comparar seu comportamento financeiro.",
                    "Receitas e despesas do período estão zeradas.",
                    null
            ));
        }

        if (expenses.compareTo(income) > 0) {
            insights.add(new FinancialInsightDTO(
                    "NEGATIVE_BALANCE",
                    FinancialInsightSeverity.CRITICO,
                    "As despesas ultrapassaram as receitas",
                    "Revise primeiro gastos adiáveis e compromissos recorrentes antes de assumir uma nova parcela.",
                    "O mês fechou com saldo negativo de " + balance.abs().setScale(2, RoundingMode.HALF_UP) + ".",
                    balance.abs().setScale(2, RoundingMode.HALF_UP)
            ));
        } else if (income.signum() > 0) {
            BigDecimal expenseRatio = expenses.divide(income, 4, RoundingMode.HALF_UP);
            if (expenseRatio.compareTo(new BigDecimal("0.90")) >= 0) {
                insights.add(new FinancialInsightDTO(
                        "HIGH_COMMITMENT",
                        FinancialInsightSeverity.ATENCAO,
                        "Pouca margem livre neste mês",
                        "Uma margem pequena aumenta o impacto de imprevistos. Procure reduzir um gasto recorrente antes do próximo fechamento.",
                        "As despesas consumiram " + expenseRatio.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "% das receitas.",
                        null
                ));
            }
        }

        if (previousExpenses.signum() > 0 && expenses.compareTo(previousExpenses) > 0) {
            BigDecimal increase = expenses.subtract(previousExpenses)
                    .divide(previousExpenses, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (increase.compareTo(new BigDecimal("15")) >= 0) {
                insights.add(new FinancialInsightDTO(
                        "EXPENSE_GROWTH",
                        FinancialInsightSeverity.ATENCAO,
                        "Os gastos aceleraram em relação ao mês anterior",
                        "Compare as categorias que mais cresceram e confirme se o aumento foi pontual ou recorrente.",
                        "As despesas aumentaram " + increase.setScale(1, RoundingMode.HALF_UP) + "% sobre o mês anterior.",
                        expenses.subtract(previousExpenses).setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        if (expenses.signum() > 0 && !expensesByCategory.isEmpty()) {
            DashboardCategorySummaryDTO topCategory = expensesByCategory.getFirst();
            BigDecimal share = topCategory.totalAmount()
                    .divide(expenses, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (share.compareTo(new BigDecimal("35")) >= 0) {
                String category = topCategory.category().name().replace('_', ' ');
                insights.add(new FinancialInsightDTO(
                        "CATEGORY_CONCENTRATION",
                        FinancialInsightSeverity.ATENCAO,
                        "Uma categoria concentra boa parte das despesas",
                        "Abra essa categoria e procure cobranças repetidas, compras adiáveis ou valores fora do padrão.",
                        category + " representa " + share.setScale(1, RoundingMode.HALF_UP) + "% das despesas do mês.",
                        topCategory.totalAmount().setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        if (balance.signum() > 0 && income.signum() > 0) {
            BigDecimal suggestedReserve = income.multiply(new BigDecimal("0.10"))
                    .min(balance)
                    .setScale(2, RoundingMode.HALF_UP);
            insights.add(new FinancialInsightDTO(
                    "POSITIVE_BALANCE",
                    FinancialInsightSeverity.POSITIVO,
                    "Há espaço para fortalecer sua reserva",
                    "Se as contas essenciais e dívidas caras estiverem em dia, considere separar parte do saldo antes de aumentar o consumo.",
                    "Sugestão conservadora: até 10% da renda, limitada ao saldo disponível.",
                    suggestedReserve
            ));
        }

        return insights;
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
        return findGroupedTransactionsByType(user, Transaction.TransactionType.DESPESA, startDate, endDate);
    }

    private List<DashboardCategorySummaryDTO> findGroupedTransactionsByType(
            User user,
            Transaction.TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return transactionRepository.findSummaryByCategoryAndTypeAndTransactionDateBetween(user, type, startDate, endDate)
                .stream()
                .filter(item -> type != Transaction.TransactionType.DESPESA || item.getCategory() != Transaction.TransactionCategory.INVESTIMENTO)
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
                .highestOrdinaryExpense(user, startDate, endDate)
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
