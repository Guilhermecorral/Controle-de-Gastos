import { DashboardResponse, TransactionResponse } from '../../../types';
import { categoryLabels, formatCurrency, formatIsoDate, formatMonthLabel } from '../../../lib/mockFinance';
import {
  EmptyState,
  FeatureChip,
  InfoStrip,
  LoadingCard,
  MetricCard,
  ProgressBar,
  SectionCard,
  SelectField,
  UnavailableCard,
} from '../../shared/ui';
import { monthOptions, yearOptions } from '../../workspace/constants';

type DashboardPageProps = {
  snapshot: DashboardResponse | null;
  annualTransactions: TransactionResponse[];
  wishlistSummary: {
    desiredCount: number;
    purchasedCount: number;
    desiredValue: number;
    purchasedValue: number;
  };
  hasError: boolean;
  isLoading: boolean;
  year: number;
  month: number;
  onReferenceYearChange: (value: number) => void;
  onReferenceMonthChange: (value: number) => void;
  onOpenWishlist: () => void;
  onboardingDismissed: boolean;
  sessionName: string;
  onDismissOnboarding: () => void;
  onOpenTransactions: () => void;
};

type MonthlyTotals = {
  monthIndex: number;
  label: string;
  receitas: number;
  despesas: number;
};

const annualMonthLabels = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

export default function DashboardPage({
  snapshot,
  annualTransactions,
  wishlistSummary,
  hasError,
  isLoading,
  year,
  month,
  onReferenceYearChange,
  onReferenceMonthChange,
  onOpenWishlist,
  onboardingDismissed,
  sessionName,
  onDismissOnboarding,
  onOpenTransactions,
}: DashboardPageProps) {
  if (isLoading) {
    return <LoadingCard label="Carregando painel real..." />;
  }

  if (hasError || !snapshot) {
    return <UnavailableCard label="Não foi possível carregar o painel agora." />;
  }

  const annualFlow = buildAnnualFlowData(annualTransactions, year);

  return (
    <>
      {!onboardingDismissed && (
        <WelcomePanel
          onDismiss={onDismissOnboarding}
          onOpenTransactions={onOpenTransactions}
          onOpenWishlist={onOpenWishlist}
          sessionName={sessionName}
        />
      )}

      <SectionCard>
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Resumo do período</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">{formatMonthLabel(year, month)}</h3>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              O painel mostra o mês escolhido com clareza e preserva o acumulado até esse ponto para contar a história financeira certa.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <SelectField
              label="Ano"
              options={yearOptions.map((option) => ({ value: String(option), label: String(option) }))}
              value={String(year)}
              onChange={(value) => onReferenceYearChange(Number(value))}
            />
            <SelectField
              label="Mês"
              options={monthOptions.map((option) => ({ value: String(option.value), label: option.label }))}
              value={String(month)}
              onChange={(value) => onReferenceMonthChange(Number(value))}
            />
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Receitas do mês" tone="positive" value={formatCurrency(snapshot.receitasMesAtual)} />
          <MetricCard label="Despesas do mês" tone="negative" value={formatCurrency(snapshot.despesasMesAtual)} />
          <MetricCard
            label="Resultado do mês"
            tone={snapshot.resultadoMesAtual >= 0 ? 'neutral' : 'warning'}
            value={formatCurrency(snapshot.resultadoMesAtual)}
          />
          <MetricCard label="Saldo acumulado" tone="neutral" value={formatCurrency(snapshot.saldoAcumulado)} />
        </div>
      </SectionCard>

      <SectionCard title="Fluxo do período">
        <div className="grid gap-4 xl:grid-cols-3">
          <InfoStrip helper="Tudo que entrou até o mês de referência." label="Receitas do ano" value={formatCurrency(snapshot.receitasAnoReferencia)} />
          <InfoStrip helper="Tudo que saiu até o mês de referência." label="Despesas do ano" value={formatCurrency(snapshot.despesasAnoReferencia)} />
          <InfoStrip helper="Resultado acumulado do ano até aqui." label="Resultado do ano" value={formatCurrency(snapshot.resultadoAnoReferencia)} />
        </div>

        <div className="mt-6 grid gap-6 xl:grid-cols-[280px_minmax(0,1fr)]">
          <div className="rounded-[24px] bg-slate-50 p-5">
            <div className="flex items-center justify-between text-sm text-slate-500">
              <span>Entrada x saída do mês</span>
              <span>{snapshot.resultadoMesAtual >= 0 ? 'Mês positivo' : 'Mês apertado'}</span>
            </div>
            <div className="mt-4 grid gap-4">
              <ProgressBar
                label="Receitas"
                tone="positive"
                total={Math.max(snapshot.receitasMesAtual, snapshot.despesasMesAtual, 1)}
                value={snapshot.receitasMesAtual}
              />
              <ProgressBar
                label="Despesas"
                tone="negative"
                total={Math.max(snapshot.receitasMesAtual, snapshot.despesasMesAtual, 1)}
                value={snapshot.despesasMesAtual}
              />
            </div>
          </div>

          <AnnualBarChart data={annualFlow} year={year} />
        </div>

        <div className="mt-6 rounded-[24px] border border-slate-100 bg-slate-50 p-5">
          <div className="grid gap-5 xl:grid-cols-[0.95fr_1.05fr] xl:items-start">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Lista de desejos em destaque</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">Desejos que ainda pesam no planejamento</h4>
              <p className="mt-3 max-w-md text-sm leading-7 text-slate-600">
                Esse bloco mostra o peso dos desejos sem roubar a atenção da parte financeira principal.
              </p>
              <button
                className="mt-5 rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                onClick={onOpenWishlist}
                type="button"
              >
                Abrir lista de desejos
              </button>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <CompactWishlistCard
                helper="Itens aguardando decisão."
                label="Pendentes"
                value={`${wishlistSummary.desiredCount} item(ns)`}
              />
              <CompactWishlistCard
                helper="Total ainda desejado."
                label="Valor desejado"
                value={formatCurrency(wishlistSummary.desiredValue)}
              />
              <CompactWishlistCard
                helper="Compra concluída já enviada ao financeiro."
                label="Comprados"
                value={`${wishlistSummary.purchasedCount} item(ns)`}
              />
              <CompactWishlistCard
                helper="Total que já virou compra."
                label="Valor comprado"
                value={formatCurrency(wishlistSummary.purchasedValue)}
              />
            </div>
          </div>
        </div>
      </SectionCard>

      <section className="grid gap-5 lg:grid-cols-2">
        <SectionCard title="Últimas 5 transações">
          <div className="grid max-h-[320px] gap-3 overflow-y-auto pr-1">
            {snapshot.ultimasTransacoes.map((transaction) => (
              <div key={transaction.id} className="rounded-[20px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-slate-900">{transaction.description}</p>
                    <p className="mt-1 text-sm text-slate-500">
                      {categoryLabels[transaction.category]} · {formatIsoDate(transaction.transactionDate)}
                    </p>
                  </div>
                  <span className={`text-sm font-semibold ${transaction.type === 'RECEITA' ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {transaction.type === 'RECEITA' ? '+' : '-'} {formatCurrency(transaction.amount)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="Gastos por categoria">
          <div className="grid max-h-[320px] gap-4 overflow-y-auto pr-1">
            {snapshot.gastosPorCategoria.length === 0 && <EmptyState label="Sem despesas categorizadas no período." />}
            {snapshot.gastosPorCategoria.map((entry) => (
              <div key={entry.category}>
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-semibold text-slate-700">{categoryLabels[entry.category]}</span>
                  <span className="text-slate-500">{formatCurrency(entry.totalAmount)}</span>
                </div>
                <div className="h-3 rounded-full bg-slate-100">
                  <div
                    className="h-3 rounded-full bg-gradient-to-r from-emerald-400 to-emerald-500"
                    style={{
                      width: `${Math.max((entry.totalAmount / Math.max(...snapshot.gastosPorCategoria.map((item) => item.totalAmount), 1)) * 100, 8)}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>
    </>
  );
}

function AnnualBarChart({ data, year }: { data: MonthlyTotals[]; year: number }) {
  const maxValue = Math.max(...data.flatMap((entry) => [entry.receitas, entry.despesas]), 1);
  const hasAnyData = data.some((entry) => entry.receitas > 0 || entry.despesas > 0);

  return (
    <div className="rounded-[24px] border border-slate-100 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-600">Visão anual</p>
          <h4 className="mt-2 text-lg font-semibold text-slate-900">Receitas x despesas em {year}</h4>
        </div>
        <div className="flex flex-wrap gap-3 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
          <span className="inline-flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-emerald-500" />
            Receitas
          </span>
          <span className="inline-flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-rose-500" />
            Despesas
          </span>
        </div>
      </div>

      {!hasAnyData ? (
        <div className="mt-5">
          <EmptyState label="Ainda não há movimentações suficientes neste ano para desenhar o gráfico." />
        </div>
      ) : (
        <div className="mt-5">
          <div className="grid h-[260px] grid-cols-12 gap-3">
            {data.map((entry) => (
              <div key={entry.monthIndex} className="group relative flex h-full flex-col justify-end">
                <div className="pointer-events-none absolute left-1/2 top-1 z-10 w-32 -translate-x-1/2 rounded-2xl bg-slate-900 px-3 py-2 text-[11px] text-white opacity-0 shadow-xl transition group-hover:opacity-100">
                  <p className="font-semibold uppercase tracking-[0.12em] text-emerald-200">{entry.label}</p>
                  <p className="mt-2">Receita: {formatCurrency(entry.receitas)}</p>
                  <p>Despesa: {formatCurrency(entry.despesas)}</p>
                </div>

                <div className="flex h-[215px] items-end justify-center gap-1.5 rounded-[18px] bg-slate-50 px-2 py-3 transition group-hover:bg-slate-100">
                  <div className="flex h-full items-end">
                    <div
                      className="w-3 rounded-full bg-emerald-500 shadow-[0_10px_20px_rgba(16,185,129,0.18)] sm:w-3.5"
                      style={{ height: `${Math.max((entry.receitas / maxValue) * 100, 4)}%` }}
                      title={`Receita em ${entry.label}: ${formatCurrency(entry.receitas)}`}
                    />
                  </div>
                  <div className="flex h-full items-end">
                    <div
                      className="w-3 rounded-full bg-rose-500 shadow-[0_10px_20px_rgba(244,63,94,0.18)] sm:w-3.5"
                      style={{ height: `${Math.max((entry.despesas / maxValue) * 100, 4)}%` }}
                      title={`Despesa em ${entry.label}: ${formatCurrency(entry.despesas)}`}
                    />
                  </div>
                </div>
                <p className="mt-2 text-center text-[10px] font-semibold uppercase tracking-[0.08em] text-slate-500">
                  {entry.label}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function buildAnnualFlowData(transactions: TransactionResponse[], referenceYear: number): MonthlyTotals[] {
  return Array.from({ length: 12 }, (_, monthIndex) => {
    const monthTransactions = transactions.filter((transaction) => {
      const date = new Date(`${transaction.transactionDate}T12:00:00`);
      return date.getFullYear() === referenceYear && date.getMonth() === monthIndex;
    });

    const receitas = monthTransactions
      .filter((transaction) => transaction.type === 'RECEITA')
      .reduce((sum, transaction) => sum + Number(transaction.amount), 0);
    const despesas = monthTransactions
      .filter((transaction) => transaction.type === 'DESPESA')
      .reduce((sum, transaction) => sum + Number(transaction.amount), 0);

    return {
      monthIndex,
      label: annualMonthLabels[monthIndex],
      receitas,
      despesas,
    };
  });
}

function CompactWishlistCard({
  label,
  value,
  helper,
}: {
  label: string;
  value: string;
  helper: string;
}) {
  return (
    <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-600">{label}</p>
      <p className="mt-3 text-2xl font-semibold text-slate-900">{value}</p>
      <p className="mt-2 text-sm leading-6 text-slate-500">{helper}</p>
    </div>
  );
}

function WelcomePanel({
  sessionName,
  onDismiss,
  onOpenTransactions,
  onOpenWishlist,
}: {
  sessionName: string;
  onDismiss: () => void;
  onOpenTransactions: () => void;
  onOpenWishlist: () => void;
}) {
  return (
    <section className="rounded-[32px] border border-emerald-200 bg-gradient-to-r from-emerald-500 to-emerald-400 px-6 py-6 text-white shadow-[0_24px_70px_rgba(16,185,129,0.18)] lg:px-8">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-100">Primeiros passos</p>
          <h3 className="mt-2 text-3xl font-semibold">Bom te ver por aqui, {sessionName.split(' ')[0]}.</h3>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-emerald-50">
            Este bloco deixa a entrada mais humana. Primeiro você entende o ambiente, depois registra algo e por fim testa a lista de desejos sem se perder.
          </p>
        </div>

        <button
          className="rounded-full border border-white/30 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
          onClick={onDismiss}
          type="button"
        >
          Dispensar
        </button>
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-3">
        <FeatureChip dark helper="Veja o mês, o acumulado e a lista de desejos em destaque." label="1. Ler o painel" />
        <FeatureChip dark helper="Crie uma receita ou despesa e sinta o fluxo do modal." label="2. Fazer um lançamento" />
        <FeatureChip dark helper="Marque um item como comprado e observe o impacto no financeiro." label="3. Testar a lista de desejos" />
      </div>

      <div className="mt-6 flex flex-wrap gap-3">
        <button
          className="rounded-full bg-white px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-50"
          onClick={onOpenTransactions}
          type="button"
        >
          Ir para transações
        </button>
        <button
          className="rounded-full border border-white/30 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
          onClick={onOpenWishlist}
          type="button"
        >
          Abrir lista de desejos
        </button>
      </div>
    </section>
  );
}
