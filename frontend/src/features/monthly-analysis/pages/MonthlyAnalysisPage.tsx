import { Category, MonthlyAnalysisResponse } from '../../../types';
import { categoryLabels, formatCurrency, formatMonthLabel } from '../../../lib/mockFinance';
import {
  ComparisonCard,
  EmptyState,
  ExpenseHighlight,
  InfoStrip,
  LoadingCard,
  MetricCard,
  SectionCard,
  SelectField,
  TrendRow,
  UnavailableCard,
} from '../../shared/ui';

type MonthlyAnalysisPageProps = {
  snapshot: MonthlyAnalysisResponse | null;
  hasError: boolean;
  isLoading: boolean;
  year: number;
  month: number;
  yearOptions: number[];
  monthOptions: Array<{ value: number; label: string }>;
  onYearChange: (value: number) => void;
  onMonthChange: (value: number) => void;
};

type PieSlice = {
  category: Category;
  totalAmount: number;
  percentage: number;
  color: string;
};

const pieColors = ['#0f766e', '#10b981', '#14b8a6', '#f59e0b', '#f97316', '#ef4444', '#6366f1', '#64748b'];

export default function MonthlyAnalysisPage({
  snapshot,
  hasError,
  isLoading,
  year,
  month,
  yearOptions,
  monthOptions,
  onYearChange,
  onMonthChange,
}: MonthlyAnalysisPageProps) {
  if (isLoading) {
    return <LoadingCard label="Carregando análise mensal real..." />;
  }

  if (hasError || !snapshot) {
    return <UnavailableCard label="Não foi possível carregar a análise mensal agora." />;
  }

  const menorGasto =
    snapshot.gastosPorCategoria.length === 0
      ? null
      : [...snapshot.gastosPorCategoria].sort((a, b) => a.totalAmount - b.totalAmount)[0];
  const incomePieData = buildCategoryPieData(snapshot.receitasPorCategoria);
  const categoryPieData = buildCategoryPieData(snapshot.gastosPorCategoria);

  return (
    <>
      <SectionCard>
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Leitura do mês</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">{formatMonthLabel(year, month)}</h3>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              Esta área deixa o período bem visível e cria o contexto certo antes das comparações.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <SelectField
              label="Ano"
              options={yearOptions.map((option) => ({ value: String(option), label: String(option) }))}
              value={String(year)}
              onChange={(value) => onYearChange(Number(value))}
            />
            <SelectField
              label="Mês"
              options={monthOptions.map((option) => ({ value: String(option.value), label: option.label }))}
              value={String(month)}
              onChange={(value) => onMonthChange(Number(value))}
            />
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          <MetricCard label="Receitas do mês" tone="positive" value={formatCurrency(snapshot.totalReceitas)} />
          <MetricCard label="Despesas do mês" tone="negative" value={formatCurrency(snapshot.totalDespesas)} />
          <MetricCard label="Saldo do mês" tone={snapshot.saldo >= 0 ? 'neutral' : 'warning'} value={formatCurrency(snapshot.saldo)} />
        </div>
      </SectionCard>

      <section className="grid gap-5 lg:grid-cols-2">
        <SectionCard title="Maior gasto">
          {snapshot.maiorGasto ? <ExpenseHighlight transaction={snapshot.maiorGasto} /> : <EmptyState label="Sem despesas registradas no período." />}
        </SectionCard>

        <SectionCard title="Menor gasto">
          {menorGasto ? (
            <div className="rounded-[22px] bg-slate-50 p-5">
              <p className="text-lg font-semibold text-slate-900">{categoryLabels[menorGasto.category as Category]}</p>
              <p className="mt-2 text-sm text-slate-500">Categoria com menor peso entre os gastos do mês</p>
              <p className="mt-4 text-2xl font-semibold text-emerald-600">{formatCurrency(menorGasto.totalAmount)}</p>
            </div>
          ) : (
            <EmptyState label="Sem despesas registradas no período." />
          )}
        </SectionCard>
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <ComparisonCard
          title="Comparação com o mês anterior"
          subtitle={`Referência: ${formatMonthLabel(snapshot.comparativoMesAnterior.year, snapshot.comparativoMesAnterior.month)}`}
          summary={snapshot.comparativoMesAnterior}
        />
        <ComparisonCard
          title="Mesmo mês do ano passado"
          subtitle={`Referência: ${formatMonthLabel(
            snapshot.comparativoMesmoMesAnoAnterior.year,
            snapshot.comparativoMesmoMesAnoAnterior.month,
          )}`}
          summary={snapshot.comparativoMesmoMesAnoAnterior}
        />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard title="Acumulado do ano">
          <div className="grid gap-4 sm:grid-cols-3">
            <InfoStrip helper="Receitas até o mês selecionado." label="Receitas" value={formatCurrency(snapshot.acumuladoAnoAtual.totalReceitas)} />
            <InfoStrip helper="Despesas até o mês selecionado." label="Despesas" value={formatCurrency(snapshot.acumuladoAnoAtual.totalDespesas)} />
            <InfoStrip helper="Saldo do ano até aqui." label="Saldo" value={formatCurrency(snapshot.acumuladoAnoAtual.saldo)} />
          </div>
        </SectionCard>

        <SectionCard title="Ano atual x ano passado">
          <div className="grid gap-4">
            <TrendRow label="Receitas" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaReceitas} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaReceitas} />
            <TrendRow label="Despesas" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaDespesas} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaDespesas} inverse />
            <TrendRow label="Saldo" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaSaldo} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaSaldo} />
          </div>
          <div className="mt-5 rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
            Tendência geral do período: <span className="font-semibold text-slate-900">{snapshot.comparativoAcumuladoAnoAnterior.tendenciaGeral.toLowerCase()}</span>.
          </div>
        </SectionCard>
      </section>

      <SectionCard title="Peso das categorias no mês">
        <div className="grid gap-6 xl:items-start xl:grid-cols-2">
          <div className="grid gap-4 xl:grid-rows-[340px_auto]">
            <CategoryPieChart
              data={incomePieData}
              emptyLabel="Sem receitas categorizadas neste mês."
              title="Receitas por categoria"
              centerLabel="Receitas"
              helper="Aqui você enxerga de onde o dinheiro entrou neste período."
            />
            <div className="grid gap-4 xl:max-h-[280px] xl:overflow-y-auto xl:pr-1">
              <CategoryLegend
                data={incomePieData}
                emptyLabel="Sem receitas categorizadas neste mês."
                percentageLabel="das receitas do mês"
              />
            </div>
          </div>

          <div className="grid gap-4 xl:grid-rows-[340px_auto]">
            <CategoryPieChart
              data={categoryPieData}
              emptyLabel="Sem despesas categorizadas neste mês."
              title="Despesas por categoria"
              centerLabel="Despesas"
              helper="Aqui fica mais claro para onde o dinheiro mais saiu no mês."
            />
            <div className="grid gap-4 xl:max-h-[280px] xl:overflow-y-auto xl:pr-1">
              <CategoryLegend
                data={categoryPieData}
                emptyLabel="Sem despesas categorizadas neste mês."
                percentageLabel="das despesas do mês"
              />
            </div>
          </div>
        </div>
      </SectionCard>
    </>
  );
}

function CategoryPieChart({
  data,
  title,
  centerLabel,
  helper,
  emptyLabel,
}: {
  data: PieSlice[];
  title: string;
  centerLabel: string;
  helper: string;
  emptyLabel: string;
}) {
  if (data.length === 0) {
    return <EmptyState label={emptyLabel} />;
  }

  const gradient = buildPieGradient(data);

  return (
    <div className="flex h-[340px] flex-col items-center rounded-[24px] border border-slate-100 bg-white px-6 py-8">
      <p className="text-center text-sm font-semibold uppercase tracking-[0.14em] text-emerald-600">{title}</p>
      <div
        className="relative mt-6 h-44 w-44 rounded-full shadow-[0_18px_50px_rgba(15,23,42,0.08)]"
        style={{ background: gradient }}
        title={title}
      >
        <div className="absolute inset-[22%] rounded-full bg-white shadow-inner" />
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="text-center">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{centerLabel}</p>
            <p className="mt-2 text-xl font-semibold text-slate-900">100%</p>
          </div>
        </div>
      </div>
      <p className="mt-auto text-center text-sm leading-7 text-slate-600">
        {helper}
      </p>
    </div>
  );
}

function CategoryLegend({
  data,
  emptyLabel,
  percentageLabel,
}: {
  data: PieSlice[];
  emptyLabel: string;
  percentageLabel: string;
}) {
  if (data.length === 0) {
    return <EmptyState label={emptyLabel} />;
  }

  return (
    <>
      {data.map((entry) => (
        <div key={entry.category} className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="h-3.5 w-3.5 rounded-full" style={{ backgroundColor: entry.color }} />
              <div>
                <p className="font-semibold text-slate-900">{categoryLabels[entry.category]}</p>
                <p className="text-sm text-slate-500">{entry.percentage.toFixed(1)}% {percentageLabel}</p>
              </div>
            </div>
            <span className="text-sm font-semibold text-slate-700">{formatCurrency(entry.totalAmount)}</span>
          </div>
        </div>
      ))}
    </>
  );
}

function buildCategoryPieData(
  entries: MonthlyAnalysisResponse['gastosPorCategoria'],
): PieSlice[] {
  const total = entries.reduce((sum, entry) => sum + entry.totalAmount, 0);

  return entries.map((entry, index) => ({
    category: entry.category,
    totalAmount: entry.totalAmount,
    percentage: total === 0 ? 0 : (entry.totalAmount / total) * 100,
    color: pieColors[index % pieColors.length],
  }));
}

function buildPieGradient(data: PieSlice[]): string {
  let currentStop = 0;

  const segments = data.map((entry) => {
    const nextStop = currentStop + entry.percentage;
    const segment = `${entry.color} ${currentStop}% ${nextStop}%`;
    currentStop = nextStop;
    return segment;
  });

  return `conic-gradient(${segments.join(', ')})`;
}
