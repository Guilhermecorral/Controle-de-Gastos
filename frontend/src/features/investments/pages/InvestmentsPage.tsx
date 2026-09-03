import { FormEvent, ReactNode, useDeferredValue, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, BarChart3, CalendarDays, Clock3, Plus, Search, X } from 'lucide-react';
import {
  InvestmentAssetSearchResponse,
  InvestmentAssetType,
  InvestmentGoalResponse,
  InvestmentIncomeScheduleResponse,
  InvestmentMovementResponse,
  InvestmentPositionRequest,
  InvestmentPositionResponse,
  InvestmentProjectionRequest,
  InvestmentProjectionResponse,
  InvestmentTradeRequest,
} from '../../../types';
import {
  useCreateInvestmentMutation,
  useContributeToInvestmentGoalMutation,
  useCreateInvestmentGoalMutation,
  useCreateInvestmentIncomeScheduleMutation,
  useDeleteInvestmentMutation,
  useDeleteInvestmentGoalMutation,
  useDeleteInvestmentGoalContributionMutation,
  useDeleteInvestmentIncomeScheduleMutation,
  useInvestmentAssetSearchQuery,
  useInvestmentGoalsQuery,
  useInvestmentGoalContributionsQuery,
  useInvestmentIncomeSchedulesQuery,
  useInvestmentMovementsQuery,
  useInvestmentPortfolioQuery,
  useInvestmentProjectionMutation,
  useInvestmentReconciliationQuery,
  useInvestmentTaxSummaryQuery,
  useRecordInvestmentIncomeMutation,
  useReceiveInvestmentIncomeScheduleMutation,
  useRecordInvestmentTradeMutation,
  useUpdateInvestmentGoalMutation,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { Field, LoadingCard, MetricCard, SectionCard, UnavailableCard } from '../../shared/ui';
import OFXUploader from '../../ofx-upload/components/OFXUploader';

const today = new Date().toISOString().slice(0, 10);
const nextYear = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().slice(0, 10);
type TradableType = Exclude<InvestmentAssetType, 'RENDA_FIXA'>;

export default function InvestmentsPage() {
  const portfolioQuery = useInvestmentPortfolioQuery();
  const movementsQuery = useInvestmentMovementsQuery();
  const schedulesQuery = useInvestmentIncomeSchedulesQuery();
  const goalsQuery = useInvestmentGoalsQuery();
  const createMutation = useCreateInvestmentMutation();
  const deleteMutation = useDeleteInvestmentMutation();
  const projectionMutation = useInvestmentProjectionMutation();
  const [tradeOpen, setTradeOpen] = useState(false);
  const [incomePosition, setIncomePosition] = useState<InvestmentPositionResponse | null>(null);
  const [selectedPosition, setSelectedPosition] = useState<InvestmentPositionResponse | null>(null);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [goalOpen, setGoalOpen] = useState(false);
  const [editingGoal, setEditingGoal] = useState<InvestmentGoalResponse | null>(null);
  const [contributionGoal, setContributionGoal] = useState<InvestmentGoalResponse | null>(null);
  const [fixedForm, setFixedForm] = useState<InvestmentPositionRequest>({
    assetType: 'RENDA_FIXA', symbol: null, externalId: null, name: '', quantity: null, averagePrice: null,
    principal: 0, annualRate: 12, purchaseDate: today, maturityDate: nextYear, market: 'BR', currency: 'BRL', exchange: null,
  });
  const [projection, setProjection] = useState<InvestmentProjectionRequest>({
    initialAmount: 1000,
    monthlyContribution: 500,
    interestRate: 12,
    ratePeriod: 'ANNUAL',
    timelinePeriod: 'MONTHLY',
    startDate: today,
    endDate: nextYear,
  });
  const [feedback, setFeedback] = useState('');

  if (portfolioQuery.isLoading) return <LoadingCard label="Buscando sua carteira e atualizando as cotações." />;
  if (portfolioQuery.isError) return <UnavailableCard label={getApiErrorMessage(portfolioQuery.error, 'Não foi possível carregar os investimentos.')} />;
  const portfolio = portfolioQuery.data;
  const distribution = (portfolio?.positions ?? []).reduce<Record<InvestmentAssetType, number>>((totals, position) => {
    totals[position.assetType] += position.currentValue;
    return totals;
  }, { ACAO: 0, FII: 0, CRIPTO: 0, RENDA_FIXA: 0 });

  const createFixedIncome = (event: FormEvent) => {
    event.preventDefault();
    setFeedback('');
    createMutation.mutate(fixedForm, {
      onSuccess: () => {
        setFeedback('Aplicação de renda fixa adicionada.');
        setFixedForm((current) => ({ ...current, name: '', principal: 0 }));
      },
      onError: (error) => setFeedback(getApiErrorMessage(error, 'Não foi possível salvar a aplicação.')),
    });
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[.2em] text-emerald-600">Carteira inteligente</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-950">Seus investimentos, sem cadastro no escuro</h2>
          <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">Selecione ativos verificados no catálogo e registre cada compra ou venda. A posição é calculada pelo Farol.</p>
        </div>
        <button className="button-pop button-glow flex items-center gap-2 rounded-full bg-slate-950 px-5 py-3 font-semibold text-white" type="button" onClick={() => setTradeOpen(true)}>
          <Plus size={18} /> Nova movimentação
        </button>
      </header>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Total investido" value={currency(portfolio?.totalInvested ?? 0)} tone="neutral" />
        <MetricCard label="Valor atual" value={currency(portfolio?.currentValue ?? 0)} tone="positive" />
        <MetricCard label="Ganho de capital" value={signedCurrency(portfolio?.totalCapitalGain ?? 0)} tone={(portfolio?.totalCapitalGain ?? 0) >= 0 ? 'positive' : 'negative'} />
        <MetricCard label="Retorno total" value={`${signedCurrency(portfolio?.totalReturn ?? 0)} · ${signedPercent(portfolio?.totalReturnPercent ?? 0)}`} tone={(portfolio?.totalReturn ?? 0) >= 0 ? 'positive' : 'negative'} />
      </div>

      <PortfolioEvolution points={portfolio?.evolution ?? []} />

      <div className="grid gap-6 xl:grid-cols-[1.25fr_.75fr]">
        <IncomeCalendar schedules={schedulesQuery.data ?? []} loading={schedulesQuery.isLoading} onAdd={() => setScheduleOpen(true)} />
        <GoalsPanel goals={goalsQuery.data ?? []} loading={goalsQuery.isLoading} onAdd={() => { setEditingGoal(null); setGoalOpen(true); }} onContribute={setContributionGoal} onEdit={(goal) => { setEditingGoal(goal); setGoalOpen(true); }} />
      </div>

      <TaxAndReconciliationPanel />

      {(portfolio?.currentValue ?? 0) > 0 && (
        <SectionCard title="Distribuição da carteira">
          <div className="grid gap-4 md:grid-cols-4">
            {(Object.entries(distribution) as Array<[InvestmentAssetType, number]>).map(([type, value]) => {
              const percent = ((value / (portfolio?.currentValue ?? 1)) * 100);
              return <div key={type} className="rounded-[20px] bg-slate-50 p-4"><div className="flex justify-between gap-3 text-sm"><span className="font-semibold text-slate-800">{assetLabel(type)}</span><span className="text-slate-500">{percent.toFixed(1)}%</span></div><div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${percent}%` }} /></div></div>;
            })}
          </div>
        </SectionCard>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.35fr_.65fr]">
        <SectionCard title="Minha carteira">
          <p className="mb-5 text-sm leading-7 text-slate-500">Ações, FIIs e criptos são consolidados pelas compras e vendas. A fonte e o horário da cotação permanecem visíveis.</p>
          <div className="space-y-3">
            {(portfolio?.positions ?? []).length === 0 && <EmptyPortfolio onAdd={() => setTradeOpen(true)} />}
            {(portfolio?.positions ?? []).map((position) => (
              <article key={position.id} className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white font-bold text-emerald-700 shadow-sm">{(position.symbol || position.name).slice(0, 2)}</div>
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[.16em] text-emerald-600">{assetLabel(position.assetType)} · {position.market ?? 'BR'}</p>
                      <h4 className="mt-1 text-lg font-semibold text-slate-900">{position.symbol || position.name}</h4>
                      <p className="text-sm text-slate-500">{position.name}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-slate-900">{currency(position.currentValue)}</p>
                    <p className={`mt-1 text-sm font-semibold ${position.totalReturnAmount >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{signedCurrency(position.totalReturnAmount)} · {signedPercent(position.totalReturnPercent)}</p>
                  </div>
                </div>
                <div className="mt-4 grid gap-3 border-t border-slate-200 pt-4 text-sm sm:grid-cols-3">
                  <PositionDatum label="Quantidade" value={formatQuantity(position.quantity)} />
                  <PositionDatum label={`Preço médio (${position.currency ?? 'BRL'})`} value={numberCurrency(position.averagePrice ?? 0, position.currency ?? 'BRL')} />
                  <PositionDatum label="Cotação" value={position.quote.available ? numberCurrency(position.quote.price ?? 0, position.quote.currency) : 'Indisponível'} />
                </div>
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
                  <span className="flex items-center gap-1"><Clock3 size={13} /> {position.quote.available ? `${position.quote.source} · atualizada ${formatTimestamp(position.quote.updatedAt)}` : 'Cotação indisponível · usando preço médio'}</span>
                  <div className="flex gap-3">
                    <button className="font-semibold text-slate-700 hover:text-emerald-700" type="button" onClick={() => setSelectedPosition(position)}>Ver análise</button>
                    {position.assetType !== 'RENDA_FIXA' && <button className="font-semibold text-slate-700 hover:text-emerald-700" type="button" onClick={() => setTradeOpen(true)}>Comprar ou vender</button>}
                    <button className="font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={() => setIncomePosition(position)}>Registrar provento</button>
                    {position.assetType === 'RENDA_FIXA' && <button className="font-semibold text-rose-600 hover:text-rose-800" type="button" onClick={() => window.confirm(`Excluir ${position.name} da carteira?`) && deleteMutation.mutate(position.id)}>Excluir</button>}
                  </div>
                </div>
              </article>
            ))}
          </div>
        </SectionCard>

        <div className="space-y-6">
          <SectionCard title="Últimas movimentações">
            <div className="space-y-3">
              {(movementsQuery.data ?? []).slice(0, 6).map((movement) => (
                <div key={movement.id} className="flex items-center justify-between gap-3 rounded-[18px] bg-slate-50 p-3">
                  <div className="flex items-center gap-3">
                    <span className={`flex h-9 w-9 items-center justify-center rounded-xl ${movement.movementType === 'VENDA' ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-700'}`}>{movement.movementType === 'VENDA' ? <ArrowUpRight size={17} /> : <ArrowDownLeft size={17} />}</span>
                    <div><p className="text-sm font-semibold text-slate-900">{movement.assetName}</p><p className="text-xs text-slate-500">{movementLabel(movement.movementType)} · {formatDate(movement.eventDate)}</p></div>
                  </div>
                  <span className="text-sm font-semibold text-slate-700">{numberCurrency(movement.amount, movement.currency)}</span>
                </div>
              ))}
              {(movementsQuery.data ?? []).length === 0 && <p className="rounded-[20px] border border-dashed border-slate-200 p-5 text-center text-sm text-slate-500">As compras e vendas aparecerão aqui.</p>}
            </div>
          </SectionCard>

          <SectionCard title="Nova renda fixa">
            <form className="space-y-4" onSubmit={createFixedIncome}>
              <Field label="Nome da aplicação"><input className={inputClass} required value={fixedForm.name} onChange={(e) => setFixedForm({ ...fixedForm, name: e.target.value })} placeholder="CDB, Tesouro ou LCI" /></Field>
              <NumberField label="Valor aplicado" value={fixedForm.principal ?? 0} onChange={(principal) => setFixedForm({ ...fixedForm, principal })} />
              <NumberField label="Taxa anual (%)" value={fixedForm.annualRate ?? 12} onChange={(annualRate) => setFixedForm({ ...fixedForm, annualRate })} />
              <div className="grid gap-3 sm:grid-cols-2"><DateField label="Aplicação" value={fixedForm.purchaseDate} onChange={(purchaseDate) => setFixedForm({ ...fixedForm, purchaseDate })} /><DateField label="Vencimento" value={fixedForm.maturityDate ?? nextYear} onChange={(maturityDate) => setFixedForm({ ...fixedForm, maturityDate })} /></div>
              {feedback && <p className="text-sm text-emerald-700">{feedback}</p>}
              <button className="w-full rounded-full bg-slate-900 px-5 py-3 font-semibold text-white disabled:bg-slate-400" disabled={createMutation.isPending} type="submit">Adicionar aplicação</button>
            </form>
          </SectionCard>
        </div>
      </div>

      <SectionCard title="Simulador de renda fixa">
        <p className="mb-5 max-w-3xl text-sm leading-7 text-slate-500">Simule juros compostos com aporte único ou mensal. Escolha como a taxa foi informada e como deseja acompanhar a evolução.</p>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <NumberField label="Valor inicial" value={projection.initialAmount} onChange={(initialAmount) => setProjection({ ...projection, initialAmount })} />
          <NumberField label="Aporte mensal" value={projection.monthlyContribution} onChange={(monthlyContribution) => setProjection({ ...projection, monthlyContribution })} />
          <NumberField label="Taxa de juros (%)" value={projection.interestRate} onChange={(interestRate) => setProjection({ ...projection, interestRate })} step="0.0001" />
          <Field label="Período da taxa"><select className={inputClass} value={projection.ratePeriod} onChange={(event) => setProjection({ ...projection, ratePeriod: event.target.value as 'MONTHLY' | 'ANNUAL' })}><option value="MONTHLY">Mensal</option><option value="ANNUAL">Anual</option></select></Field>
          <DateField label="Data inicial" value={projection.startDate} onChange={(startDate) => setProjection({ ...projection, startDate })} />
          <DateField label="Data final" value={projection.endDate} onChange={(endDate) => setProjection({ ...projection, endDate })} />
          <Field label="Exibir evolução"><select className={inputClass} value={projection.timelinePeriod} onChange={(event) => setProjection({ ...projection, timelinePeriod: event.target.value as 'MONTHLY' | 'YEARLY' })}><option value="MONTHLY">Por mês</option><option value="YEARLY">Por ano</option></select></Field>
          <div className="flex items-end"><button className="h-12 w-full rounded-2xl bg-emerald-500 px-5 font-semibold text-white hover:bg-emerald-600 disabled:bg-slate-300" disabled={projectionMutation.isPending || projection.initialAmount + projection.monthlyContribution <= 0} type="button" onClick={() => projectionMutation.mutate(projection)}>{projectionMutation.isPending ? 'Calculando...' : 'Calcular evolução'}</button></div>
        </div>
        {projectionMutation.isError && <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{getApiErrorMessage(projectionMutation.error, 'Não foi possível calcular esta simulação.')}</p>}
        {projectionMutation.data && <ProjectionResults result={projectionMutation.data} />}
      </SectionCard>

      <TradeDialog open={tradeOpen} positions={portfolio?.positions ?? []} onClose={() => setTradeOpen(false)} />
      <IncomeDialog position={incomePosition} onClose={() => setIncomePosition(null)} />
      <IncomeScheduleDialog open={scheduleOpen} positions={portfolio?.positions ?? []} onClose={() => setScheduleOpen(false)} />
      <GoalDialog key={editingGoal?.id ?? 'new'} goal={editingGoal} open={goalOpen} onClose={() => { setGoalOpen(false); setEditingGoal(null); }} />
      <GoalContributionDialog goal={contributionGoal} onClose={() => setContributionGoal(null)} />
      <AssetAnalysisDialog position={selectedPosition} movements={(movementsQuery.data ?? []).filter((movement) => movement.positionId === selectedPosition?.id)} onClose={() => setSelectedPosition(null)} />
    </div>
  );
}

function TradeDialog({ open, positions, onClose }: { open: boolean; positions: InvestmentPositionResponse[]; onClose: () => void }) {
  const mutation = useRecordInvestmentTradeMutation();
  const [movementType, setMovementType] = useState<'COMPRA' | 'VENDA'>('COMPRA');
  const [assetType, setAssetType] = useState<TradableType>('ACAO');
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  const [selected, setSelected] = useState<InvestmentAssetSearchResponse | null>(null);
  const [positionId, setPositionId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(0);
  const [fees, setFees] = useState(0);
  const [eventDate, setEventDate] = useState(today);
  const [error, setError] = useState('');
  const search = useInvestmentAssetSearchQuery(deferredQuery, assetType, open && movementType === 'COMPRA' && !selected);
  if (!open) return null;

  const chooseAsset = (asset: InvestmentAssetSearchResponse, id: number | null = null, price?: number | null) => {
    setSelected(asset); setPositionId(id); setUnitPrice(price ?? asset.currentPrice ?? 0); setError('');
  };
  const choosePosition = (position: InvestmentPositionResponse) => chooseAsset({
    assetType: position.assetType as TradableType, symbol: position.symbol ?? '', externalId: position.externalId ?? '',
    name: position.name, market: (position.market ?? 'BR') as 'BR' | 'US' | 'GLOBAL', exchange: position.exchange ?? '',
    currency: position.currency ?? position.quote.currency, currentPrice: position.quote.price, source: position.quote.source,
  }, position.id, position.quote.price);
  const resetSelection = () => { setSelected(null); setPositionId(null); setQuery(''); setUnitPrice(0); setError(''); };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!selected) return setError('Selecione um ativo verificado para continuar.');
    const payload: InvestmentTradeRequest = { positionId, movementType, assetType: selected.assetType as TradableType, symbol: selected.symbol,
      externalId: selected.externalId, name: selected.name, market: selected.market, exchange: selected.exchange,
      currency: selected.currency, quantity, unitPrice, fees, eventDate };
    mutation.mutate(payload, { onSuccess: () => { onClose(); resetSelection(); }, onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível registrar a movimentação.')) });
  };

  const sellable = positions.filter((position) => position.assetType !== 'RENDA_FIXA' && (position.quantity ?? 0) > 0);
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/55 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="dialog" aria-modal="true" aria-label="Nova movimentação">
      <div className="max-h-[92vh] w-full overflow-y-auto rounded-t-[30px] bg-white shadow-2xl sm:max-w-2xl sm:rounded-[30px]">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white/95 px-6 py-5 backdrop-blur">
          <div><p className="text-xs font-semibold uppercase tracking-[.18em] text-emerald-600">Movimentação</p><h3 className="mt-1 text-xl font-semibold text-slate-950">Registrar compra ou venda</h3></div>
          <button className="rounded-full bg-slate-100 p-2 text-slate-500 hover:text-slate-900" type="button" onClick={onClose}><X size={20} /></button>
        </div>
        <form className="space-y-5 p-6" onSubmit={submit}>
          <div className="grid grid-cols-2 rounded-2xl bg-slate-100 p-1">
            {(['COMPRA', 'VENDA'] as const).map((type) => <button key={type} className={`rounded-xl px-4 py-3 text-sm font-semibold transition ${movementType === type ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} type="button" onClick={() => { setMovementType(type); resetSelection(); }}>{type === 'COMPRA' ? 'Compra' : 'Venda'}</button>)}
          </div>

          {!selected && movementType === 'COMPRA' && <>
            <div className="flex flex-wrap gap-2">{(['ACAO', 'FII', 'CRIPTO'] as TradableType[]).map((type) => <button key={type} className={`rounded-full border px-4 py-2 text-sm font-semibold ${assetType === type ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 text-slate-600'}`} type="button" onClick={() => { setAssetType(type); setQuery(''); }}>{assetLabel(type)}</button>)}</div>
            <Field label="Busque pelo ticker ou nome do ativo"><div className="relative"><Search className="absolute left-4 top-3.5 text-slate-400" size={19} /><input autoFocus className={`${inputClass} pl-11`} value={query} onChange={(e) => setQuery(e.target.value)} placeholder={assetType === 'CRIPTO' ? 'Bitcoin, Ethereum...' : 'ITUB4, Itaú, Apple...'} /></div></Field>
            <div className="space-y-2">
              {search.isFetching && <p className="py-4 text-center text-sm text-slate-500">Consultando o catálogo...</p>}
              {!search.isFetching && deferredQuery.length >= 2 && search.data?.length === 0 && <p className="rounded-2xl border border-dashed border-slate-200 p-5 text-center text-sm text-slate-500">Nenhum ativo verificado foi encontrado.</p>}
              {search.data?.map((asset) => <button key={`${asset.market}-${asset.externalId}`} className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-emerald-200 hover:bg-emerald-50/40" type="button" onClick={() => chooseAsset(asset)}><div><p className="font-semibold text-slate-950">{asset.symbol} <span className="ml-2 text-sm font-normal text-slate-500">{asset.name}</span></p><p className="mt-1 text-xs text-slate-500">{asset.market} · {asset.exchange} · {asset.currency}</p></div>{asset.currentPrice != null && <span className="text-sm font-semibold text-slate-700">{numberCurrency(asset.currentPrice, asset.currency)}</span>}</button>)}
            </div>
          </>}

          {!selected && movementType === 'VENDA' && <div className="space-y-2"><p className="text-sm font-semibold text-slate-700">Selecione uma posição disponível</p>{sellable.map((position) => <button key={position.id} className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-rose-200" type="button" onClick={() => choosePosition(position)}><div><p className="font-semibold text-slate-950">{position.symbol || position.name}</p><p className="mt-1 text-xs text-slate-500">{position.name} · disponível {formatQuantity(position.quantity)}</p></div><span className="text-sm font-semibold text-slate-700">{numberCurrency(position.quote.price ?? position.averagePrice ?? 0, position.currency ?? 'BRL')}</span></button>)}</div>}

          {selected && <>
            <div className="flex items-center justify-between rounded-[22px] border border-emerald-100 bg-emerald-50/60 p-4"><div><p className="text-xs font-semibold uppercase tracking-[.16em] text-emerald-700">Ativo verificado</p><p className="mt-1 text-lg font-semibold text-slate-950">{selected.symbol} · {selected.name}</p><p className="mt-1 text-xs text-slate-500">{selected.market} · {selected.exchange} · preço em {selected.currency}</p></div><button className="text-sm font-semibold text-slate-600" type="button" onClick={resetSelection}>Trocar</button></div>
            <div className="grid gap-4 sm:grid-cols-2"><NumberField label="Quantidade" value={quantity} onChange={setQuantity} step="0.00000001" /><NumberField label={`Preço unitário (${selected.currency})`} value={unitPrice} onChange={setUnitPrice} step="0.000001" /><NumberField label={`Custos (${selected.currency})`} value={fees} onChange={setFees} /><DateField label="Data da operação" value={eventDate} onChange={setEventDate} /></div>
            <div className="flex items-center justify-between rounded-2xl bg-slate-100 px-4 py-3"><span className="text-sm font-semibold text-slate-600">Valor {movementType === 'COMPRA' ? 'investido' : 'líquido'}</span><strong className="text-slate-950">{numberCurrency(Math.max(0, quantity * unitPrice + (movementType === 'COMPRA' ? fees : -fees)), selected.currency)}</strong></div>
          </>}
          {error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
          <button className="w-full rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={!selected || mutation.isPending || quantity <= 0 || unitPrice <= 0} type="submit">{mutation.isPending ? 'Registrando...' : `Registrar ${movementType === 'COMPRA' ? 'compra' : 'venda'}`}</button>
        </form>
      </div>
    </div>
  );
}

function PortfolioEvolution({ points }: { points: Array<{ date: string; investedAmount: number; currentValue: number; incomeAmount: number }> }) {
  const history = points.filter((point) => Number.isFinite(point.investedAmount) && Number.isFinite(point.currentValue));
  const first = history[0];
  const latest = history[history.length - 1];
  const periodChange = first && latest ? latest.currentValue - first.currentValue : 0;
  const periodChangePercent = first?.currentValue ? (periodChange / first.currentValue) * 100 : 0;

  const width = 960;
  const height = 300;
  const chart = { top: 24, right: 28, bottom: 38, left: 88 };
  const plotWidth = width - chart.left - chart.right;
  const plotHeight = height - chart.top - chart.bottom;
  const values = history.flatMap((point) => [point.investedAmount, point.currentValue]);
  const rawMinimum = Math.min(...values);
  const rawMaximum = Math.max(...values);
  const rangePadding = Math.max((rawMaximum - rawMinimum) * 0.16, rawMaximum * 0.025, 1);
  const minimum = Math.max(0, rawMinimum - rangePadding);
  const maximum = rawMaximum + rangePadding;
  const domain = Math.max(1, maximum - minimum);
  const xAt = (index: number) => chart.left + (index / Math.max(1, history.length - 1)) * plotWidth;
  const yAt = (value: number) => chart.top + ((maximum - value) / domain) * plotHeight;
  const pathFor = (key: 'investedAmount' | 'currentValue') => history
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${xAt(index)} ${yAt(point[key])}`)
    .join(' ');
  const currentPath = pathFor('currentValue');
  const areaPath = currentPath
    ? `${currentPath} L ${xAt(history.length - 1)} ${chart.top + plotHeight} L ${xAt(0)} ${chart.top + plotHeight} Z`
    : '';
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    y: chart.top + ratio * plotHeight,
    value: maximum - ratio * domain,
  }));
  const labelIndexes = Array.from(new Set([0, Math.floor((history.length - 1) / 2), history.length - 1]));

  return (
    <SectionCard title="Evolução patrimonial">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-slate-500">Acompanhe o valor da carteira comparado ao capital efetivamente investido.</p>
          <p className="mt-1 text-xs text-slate-400">Retratos diários reais, sem estimar cotações anteriores ao início do acompanhamento.</p>
        </div>
        <div className="flex gap-4 text-xs font-semibold text-slate-600">
          <span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-slate-400" />Capital investido</span>
          <span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-emerald-500" />Valor da carteira</span>
        </div>
      </div>
      {history.length > 1 && first && latest ? (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <EvolutionMetric label="Capital investido" value={currency(latest.investedAmount)} />
            <EvolutionMetric label="Patrimônio atual" value={currency(latest.currentValue)} />
            <EvolutionMetric
              label="Variação no período"
              value={`${signedCurrency(periodChange)} · ${signedPercent(periodChangePercent)}`}
              tone={periodChange >= 0 ? 'positive' : 'negative'}
            />
          </div>
          <div className="overflow-x-auto rounded-[24px] border border-slate-100 bg-slate-50/80 p-3 sm:p-5">
            <svg aria-label="Gráfico da evolução patrimonial" className="h-[280px] min-w-[720px] w-full" role="img" viewBox={`0 0 ${width} ${height}`}>
              <defs>
                <linearGradient id="portfolioArea" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity="0.2" />
                  <stop offset="100%" stopColor="#10b981" stopOpacity="0.01" />
                </linearGradient>
              </defs>
              {yTicks.map((tick) => (
                <g key={tick.y}>
                  <line stroke="#e2e8f0" strokeWidth="1" x1={chart.left} x2={width - chart.right} y1={tick.y} y2={tick.y} />
                  <text fill="#94a3b8" fontSize="12" textAnchor="end" x={chart.left - 12} y={tick.y + 4}>{compactCurrency(tick.value)}</text>
                </g>
              ))}
              <path d={areaPath} fill="url(#portfolioArea)" />
              <path d={pathFor('investedAmount')} fill="none" stroke="#94a3b8" strokeDasharray="7 7" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
              <path d={currentPath} fill="none" stroke="#10b981" strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" />
              {history.map((point, index) => (
                <g key={point.date}>
                  <circle cx={xAt(index)} cy={yAt(point.currentValue)} fill="white" r="5" stroke="#10b981" strokeWidth="3">
                    <title>{`${formatDate(point.date)} · Carteira ${currency(point.currentValue)} · Investido ${currency(point.investedAmount)}`}</title>
                  </circle>
                </g>
              ))}
              {labelIndexes.map((index) => (
                <text key={history[index].date} fill="#94a3b8" fontSize="12" textAnchor={index === 0 ? 'start' : index === history.length - 1 ? 'end' : 'middle'} x={xAt(index)} y={height - 8}>{shortDate(history[index].date)}</text>
              ))}
            </svg>
            <p className="mt-1 text-right text-[11px] text-slate-400">Escala ajustada ao intervalo exibido</p>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 rounded-[22px] border border-dashed border-slate-200 bg-slate-50 p-5 sm:grid-cols-[1fr_auto] sm:items-center">
          <div><p className="font-semibold text-slate-800">O histórico começa com o primeiro retrato</p><p className="mt-1 text-sm text-slate-500">A partir do próximo dia acompanhado, o Farol mostrará a variação real da carteira.</p></div>
          {first && <div className="grid grid-cols-2 gap-3 text-right sm:min-w-[300px]"><EvolutionMetric label="Investido" value={currency(first.investedAmount)} /><EvolutionMetric label="Carteira" value={currency(first.currentValue)} /></div>}
        </div>
      )}
    </SectionCard>
  );
}

function EvolutionMetric({ label, value, tone = 'neutral' }: { label: string; value: string; tone?: 'neutral' | 'positive' | 'negative' }) {
  const valueTone = tone === 'positive' ? 'text-emerald-700' : tone === 'negative' ? 'text-rose-700' : 'text-slate-950';
  return <div className="rounded-[18px] border border-slate-100 bg-white px-4 py-3"><p className="text-xs font-semibold uppercase tracking-[.1em] text-slate-400">{label}</p><p className={`mt-1 text-base font-semibold ${valueTone}`}>{value}</p></div>;
}

function ProjectionResults({ result }: { result: InvestmentProjectionResponse }) {
  const investedShare = result.projectedBalance > 0 ? Math.min(100, (result.totalInvested / result.projectedBalance) * 100) : 0;
  const periodRate = (index: number) => {
    const previousMonth = index === 0 ? 0 : result.timeline[index - 1].month;
    const elapsedMonths = result.timeline[index].month - previousMonth;
    return (Math.pow(1 + result.effectiveMonthlyRate / 100, elapsedMonths) - 1) * 100;
  };
  return (
    <div className="mt-6 overflow-hidden rounded-[26px] bg-slate-950 text-white">
      <div className="grid gap-5 p-6 md:grid-cols-3">
        <ProjectionMetric label="Saldo final" value={currency(result.projectedBalance)} />
        <ProjectionMetric label="Total investido" value={currency(result.totalInvested)} />
        <ProjectionMetric label="Juros ganhos" value={currency(result.projectedEarnings)} />
      </div>
      <div className="border-y border-white/10 px-6 py-5">
        <div className="mb-3 flex justify-between text-xs text-slate-300"><span>Composição do saldo</span><span>{result.months} meses · {result.effectiveMonthlyRate.toFixed(4).replace('.', ',')}% a.m.</span></div>
        <div className="flex h-4 overflow-hidden rounded-full bg-emerald-400"><div className="bg-slate-400" style={{ width: `${investedShare}%` }} /></div>
        <div className="mt-3 flex flex-wrap gap-5 text-xs"><span className="flex items-center gap-2 text-slate-300"><i className="h-2.5 w-2.5 rounded-full bg-slate-400" />Investido {currency(result.totalInvested)}</span><span className="flex items-center gap-2 text-emerald-300"><i className="h-2.5 w-2.5 rounded-full bg-emerald-400" />Juros {currency(result.projectedEarnings)}</span></div>
      </div>
      <div className="overflow-x-auto bg-white text-slate-800">
        <table className="w-full min-w-[980px] text-left text-sm">
          <thead className="bg-slate-100 text-xs uppercase tracking-[.1em] text-slate-500"><tr><th className="px-5 py-4">Período</th><th className="px-5 py-4">Data</th><th className="px-5 py-4">Taxa no período</th><th className="px-5 py-4">Aportes no período</th><th className="px-5 py-4">Juros do período</th><th className="px-5 py-4">Total investido</th><th className="px-5 py-4">Total em juros</th><th className="px-5 py-4 text-right">Saldo</th></tr></thead>
          <tbody>{result.timeline.map((point, index) => <tr key={point.month} className="border-t border-slate-100"><td className="px-5 py-4 font-semibold">{result.timelinePeriod === 'YEARLY' ? `${Math.ceil(point.month / 12)}º ano` : `${point.month}º mês`}</td><td className="px-5 py-4 text-slate-500">{formatDate(point.date)}</td><td className="px-5 py-4">{periodRate(index).toFixed(4).replace('.', ',')}%</td><td className="px-5 py-4">{currency(point.contribution)}</td><td className="px-5 py-4 text-emerald-700">{currency(point.interest)}</td><td className="px-5 py-4">{currency(point.totalInvested)}</td><td className="px-5 py-4 text-emerald-700">{currency(point.totalInterest)}</td><td className="px-5 py-4 text-right font-semibold">{currency(point.balance)}</td></tr>)}</tbody>
        </table>
      </div>
      <p className="px-6 py-4 text-xs leading-6 text-slate-400">{result.disclaimer}</p>
    </div>
  );
}

function TaxAndReconciliationPanel() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [importOpen, setImportOpen] = useState(false);
  const taxQuery = useInvestmentTaxSummaryQuery(year);
  const reconciliationQuery = useInvestmentReconciliationQuery(year);
  const tax = taxQuery.data;
  const reconciliation = reconciliationQuery.data;
  const refresh = () => { taxQuery.refetch(); reconciliationQuery.refetch(); };
  return <SectionCard title="Tributação e conciliação">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-4"><div><p className="max-w-2xl text-sm leading-6 text-slate-500">Confira impostos retidos em proventos e compare as movimentações da carteira com seu extrato importado. Vendas ficam sinalizadas para apuração, porque as regras dependem do ativo e do resultado do período.</p></div><div className="flex gap-2"><select aria-label="Ano da apuração" className="h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-700" value={year} onChange={(event) => setYear(Number(event.target.value))}>{[currentYear, currentYear - 1, currentYear - 2].map((option) => <option key={option} value={option}>{option}</option>)}</select><button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={() => setImportOpen((open) => !open)}>{importOpen ? 'Fechar importação' : 'Importar extrato'}</button></div></div>
    {importOpen && <div className="mb-6 rounded-[24px] border border-emerald-100 bg-emerald-50/40 p-4 sm:p-5"><p className="mb-4 text-sm leading-6 text-slate-600">Envie OFX, CSV, TSV ou Excel. Revise as linhas antes de salvar; depois, a conciliação será atualizada. O importador registra lançamentos financeiros e não cria compras ou vendas de ativos automaticamente.</p><OFXUploader compact onImported={() => { setImportOpen(false); refresh(); }} /></div>}
    <div className="grid gap-4 md:grid-cols-3"><FiscalMetric label="Imposto retido" value={taxQuery.isLoading ? '...' : currency(tax?.totalWithheld ?? 0)} helper="Proventos confirmados" tone="positive" /><FiscalMetric label="Eventos para revisar" value={taxQuery.isLoading ? '...' : String(tax?.reviewCount ?? 0)} helper="Sem cálculo automático" tone={(tax?.reviewCount ?? 0) > 0 ? 'warning' : 'neutral'} /><FiscalMetric label="Extrato conciliado" value={reconciliationQuery.isLoading ? '...' : `${reconciliation?.reconciledCount ?? 0}/${(reconciliation?.items ?? []).length}`} helper={`${reconciliation?.pendingCount ?? 0} pendente(s)`} tone={(reconciliation?.pendingCount ?? 0) > 0 ? 'warning' : 'positive'} /></div>
    <div className="mt-6 grid gap-6 xl:grid-cols-2"><div><div className="mb-3 flex items-center justify-between"><h4 className="font-semibold text-slate-900">Eventos fiscais</h4><span className="text-xs text-slate-400">{year}</span></div>{(tax?.events ?? []).length === 0 ? <EmptyFiscal label="Nenhum provento recebido ou venda registrada neste ano." /> : <div className="space-y-2">{tax?.events.slice(0, 7).map((event, index) => <div key={`${event.date}-${event.symbol}-${index}`} className="rounded-2xl bg-slate-50 p-3"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{event.symbol || event.assetName} · {event.eventType.toLowerCase()}</p><p className="mt-1 text-xs text-slate-500">{formatDate(event.date)} · {event.note}</p></div><TaxBadge status={event.status} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs"><span className="text-slate-500">Bruto <b className="text-slate-700">{currency(event.grossAmount)}</b></span><span className="text-slate-500">Retido <b className="text-emerald-700">{currency(event.withheldAmount)}</b></span><span className="text-slate-500">Líquido <b className="text-slate-700">{currency(event.netAmount)}</b></span></div></div>)}</div>}</div><div><div className="mb-3 flex items-center justify-between"><h4 className="font-semibold text-slate-900">Conciliação do extrato</h4><button className="text-xs font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={refresh}>Atualizar</button></div>{(reconciliation?.items ?? []).length === 0 ? <EmptyFiscal label="Ainda não há movimentações de investimento no período." /> : <div className="space-y-2">{reconciliation?.items.slice(0, 7).map((item) => <div key={item.movementId} className="rounded-2xl bg-slate-50 p-3"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{item.symbol || item.assetName} · {movementLabel(item.movementType)}</p><p className="mt-1 text-xs text-slate-500">{formatDate(item.eventDate)} · {item.note}</p></div><ReconciliationBadge status={item.status} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs"><span className="text-slate-500">Carteira <b className="text-slate-700">{numberCurrency(item.expectedAmount, item.currency)}</b></span>{item.transactionAmount != null && <span className="text-slate-500">Extrato <b className="text-slate-700">{currency(item.transactionAmount)}</b></span>}</div></div>)}</div>}</div></div>
    <p className="mt-5 text-xs leading-6 text-slate-400">Este painel organiza valores já registrados. Para DARF, compensações, isenções ou operações complexas, use-o como conferência e valide a apuração com sua documentação fiscal.</p>
  </SectionCard>;
}

function FiscalMetric({ label, value, helper, tone }: { label: string; value: string; helper: string; tone: 'neutral' | 'positive' | 'warning' }) { const color = tone === 'positive' ? 'text-emerald-700' : tone === 'warning' ? 'text-amber-700' : 'text-slate-800'; return <div className="rounded-[20px] border border-slate-100 bg-slate-50 p-4"><p className="text-xs font-semibold uppercase tracking-[.12em] text-slate-400">{label}</p><p className={`mt-2 text-xl font-semibold ${color}`}>{value}</p><p className="mt-1 text-xs text-slate-500">{helper}</p></div>; }
function EmptyFiscal({ label }: { label: string }) { return <div className="rounded-[20px] border border-dashed border-slate-200 bg-slate-50 p-5 text-center text-sm text-slate-500">{label}</div>; }
function TaxBadge({ status }: { status: 'RETIDO' | 'SEM_RETENCAO' | 'REVISAR' }) { const options = { RETIDO: ['Retido', 'bg-emerald-100 text-emerald-800'], SEM_RETENCAO: ['Sem retenção', 'bg-slate-200 text-slate-600'], REVISAR: ['Revisar', 'bg-amber-100 text-amber-800'] } as const; const [label, classes] = options[status]; return <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${classes}`}>{label}</span>; }
function ReconciliationBadge({ status }: { status: 'CONCILIADO' | 'GERADO_PELO_FAROL' | 'PENDENTE' | 'REVISAR' }) { const options = { CONCILIADO: ['Conciliado', 'bg-emerald-100 text-emerald-800'], GERADO_PELO_FAROL: ['Farol', 'bg-sky-100 text-sky-800'], PENDENTE: ['Pendente', 'bg-amber-100 text-amber-800'], REVISAR: ['Revisar', 'bg-slate-200 text-slate-600'] } as const; const [label, classes] = options[status]; return <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${classes}`}>{label}</span>; }

function IncomeCalendar({ schedules, loading, onAdd }: { schedules: InvestmentIncomeScheduleResponse[]; loading: boolean; onAdd: () => void }) {
  const receiveMutation = useReceiveInvestmentIncomeScheduleMutation();
  const deleteMutation = useDeleteInvestmentIncomeScheduleMutation();
  const [error, setError] = useState('');
  const pending = schedules.filter((schedule) => schedule.status === 'AGUARDANDO');
  const upcomingValue = pending.reduce((total, schedule) => total + schedule.netAmount, 0);
  const receive = (id: number) => {
    setError('');
    receiveMutation.mutate(id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível confirmar o recebimento.')) });
  };
  const remove = (id: number) => {
    setError('');
    deleteMutation.mutate(id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível remover o provento.')) });
  };
  return <SectionCard title="Agenda de proventos">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
      <div><p className="text-sm text-slate-500">Planeje o que está anunciado e registre no histórico somente quando o valor cair na conta.</p><p className="mt-2 text-sm font-semibold text-emerald-700">{currency(upcomingValue)} líquidos aguardando</p></div>
      <button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={onAdd}><Plus className="mr-1 inline" size={15} /> Agendar provento</button>
    </div>
    {error && <p className="mb-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
    {loading ? <p className="py-6 text-center text-sm text-slate-500">Carregando agenda...</p> : schedules.length === 0 ? <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 p-6 text-center"><CalendarDays className="mx-auto text-slate-400" size={24} /><p className="mt-3 text-sm font-semibold text-slate-700">Nenhum provento agendado</p><p className="mt-1 text-sm text-slate-500">Adicione a Data Com, o pagamento e o valor por cota quando tiver o comunicado do ativo.</p></div> : <div className="space-y-3">{schedules.slice(0, 6).map((schedule) => <article key={schedule.id} className="rounded-[20px] border border-slate-100 bg-slate-50 p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><div className="flex items-center gap-2"><p className="font-semibold text-slate-900">{schedule.symbol || schedule.assetName}</p><span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${schedule.status === 'RECEBIDO' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{schedule.status === 'RECEBIDO' ? 'Recebido' : 'Aguardando'}</span></div><p className="mt-1 text-xs text-slate-500">{schedule.incomeType === 'DIVIDENDO' ? 'Dividendo' : 'Rendimento'} · {schedule.amountPerUnit.toFixed(4).replace('.', ',')} por cota</p></div><div className="text-right"><p className="font-semibold text-slate-900">{currency(schedule.netAmount)}</p><p className="text-xs text-slate-500">líquido estimado</p></div></div><div className="mt-3 grid gap-2 border-t border-slate-200 pt-3 text-xs text-slate-500 sm:grid-cols-3"><span>Data Com: <strong className="text-slate-700">{schedule.exDate ? formatDate(schedule.exDate) : 'Não informada'}</strong></span><span>Pagamento: <strong className="text-slate-700">{formatDate(schedule.paymentDate)}</strong></span><span>Imposto: <strong className="text-slate-700">{schedule.taxRate.toFixed(2).replace('.', ',')}% · {currency(schedule.taxAmount)}</strong></span></div>{schedule.status === 'AGUARDANDO' && <div className="mt-3 flex justify-end gap-3"><button className="text-sm font-semibold text-slate-500 hover:text-rose-700" disabled={deleteMutation.isPending} type="button" onClick={() => remove(schedule.id)}>Remover</button><button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300" disabled={receiveMutation.isPending} type="button" onClick={() => receive(schedule.id)}>{receiveMutation.isPending ? 'Confirmando...' : 'Confirmar recebimento'}</button></div>}</article>)}</div>}
    {schedules.length > 6 && <p className="mt-4 text-center text-xs text-slate-400">Exibindo os próximos seis eventos da agenda.</p>}
  </SectionCard>;
}

function GoalsPanel({ goals, loading, onAdd, onContribute, onEdit }: { goals: InvestmentGoalResponse[]; loading: boolean; onAdd: () => void; onContribute: (goal: InvestmentGoalResponse) => void; onEdit: (goal: InvestmentGoalResponse) => void }) {
  const deleteMutation = useDeleteInvestmentGoalMutation();
  const [error, setError] = useState('');
  return <SectionCard title="Metas de patrimônio">
    <div className="mb-5 flex items-start justify-between gap-3"><p className="text-sm leading-6 text-slate-500">Cada meta tem saldo próprio: valor inicial e aportes registrados somente nela.</p><button className="shrink-0 rounded-full bg-emerald-500 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-600" type="button" onClick={onAdd}><Plus className="mr-1 inline" size={15} /> Nova meta</button></div>
    {error && <p className="mb-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
    {loading ? <p className="py-6 text-center text-sm text-slate-500">Calculando metas...</p> : goals.length === 0 ? <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 p-6 text-center"><p className="text-sm font-semibold text-slate-700">Sua primeira meta começa aqui</p><p className="mt-1 text-sm text-slate-500">Defina um objetivo e veja quanto falta para atingi-lo.</p></div> : <div className="space-y-4">{goals.map((goal) => <article key={goal.id} className="rounded-[20px] bg-slate-50 p-4"><div className="flex justify-between gap-3"><div><p className="font-semibold text-slate-900">{goal.name}</p><p className="mt-1 text-xs text-slate-500">Objetivo {currency(goal.targetAmount)} · destinado {currency(goal.currentAmount)}</p></div><button className="text-xs font-semibold text-slate-400 hover:text-rose-700" disabled={deleteMutation.isPending} type="button" onClick={() => { setError(''); deleteMutation.mutate(goal.id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível remover a meta.')) }); }}>Remover</button></div><div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${Math.min(100, goal.progressPercent)}%` }} /></div><div className="mt-3 flex justify-between gap-3 text-xs"><span className="font-semibold text-emerald-700">{goal.progressPercent.toFixed(2).replace('.', ',')}% concluído</span><span className="text-slate-500">Faltam {currency(goal.remainingAmount)}</span></div><p className="mt-3 text-xs text-slate-500">Inicial {currency(goal.initialAmount)} · aportes registrados {currency(goal.contributionsAmount)}</p><p className="mt-2 text-xs text-slate-500">{goal.achieved ? 'Meta concluída.' : goal.estimatedMonths != null ? `Conclusão estimada em ${formatMonths(goal.estimatedMonths)} com aporte mensal de ${currency(goal.monthlyContribution)}.` : 'Inclua aporte mensal ou expectativa de rendimento para estimar a conclusão.'}</p><div className="mt-4 flex flex-wrap justify-end gap-3"><button className="text-sm font-semibold text-slate-600 hover:text-emerald-700" type="button" onClick={() => onEdit(goal)}>Editar</button><button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white" type="button" onClick={() => onContribute(goal)}><Plus className="mr-1 inline" size={14} /> Registrar aporte</button></div></article>)}</div>}
  </SectionCard>;
}

function IncomeScheduleDialog({ open, positions, onClose }: { open: boolean; positions: InvestmentPositionResponse[]; onClose: () => void }) {
  const mutation = useCreateInvestmentIncomeScheduleMutation();
  const [positionId, setPositionId] = useState(0);
  const [amountPerUnit, setAmountPerUnit] = useState('');
  const [taxRate, setTaxRate] = useState(0);
  const [exDate, setExDate] = useState('');
  const [paymentDate, setPaymentDate] = useState(today);
  const [error, setError] = useState('');
  if (!open) return null;
  const selected = positions.find((position) => position.id === positionId) ?? positions[0];
  const incomeType = selected?.assetType === 'RENDA_FIXA' ? 'RENDIMENTO' : 'DIVIDENDO';
  const previewQuantity = selected?.quantity ?? 1;
  const amountPerUnitValue = parseDecimalInput(amountPerUnit);
  const gross = previewQuantity * amountPerUnitValue;
  const net = gross * (1 - taxRate / 100);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!selected) return setError('Inclua uma posição na carteira antes de agendar um provento.');
    setError('');
    mutation.mutate({ positionId: selected.id, incomeType, amountPerUnit: amountPerUnitValue, taxRate, exDate: exDate || null, paymentDate }, { onSuccess: onClose, onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível agendar o provento.')) });
  };
  return <ModalShell eyebrow="Agenda" title="Agendar provento" onClose={onClose}><form className="space-y-5" onSubmit={submit}><div className="rounded-[20px] border border-emerald-100 bg-emerald-50/70 p-4"><p className="text-xs font-semibold uppercase tracking-[.14em] text-emerald-700">Evento anunciado</p><p className="mt-1 text-sm text-slate-600">O lançamento financeiro só acontece ao confirmar que o valor foi recebido.</p></div><Field label="Ativo"><select className={inputClass} value={selected?.id ?? ''} onChange={(event) => setPositionId(Number(event.target.value))}><option value="" disabled>Selecione o ativo</option>{positions.map((position) => <option key={position.id} value={position.id}>{position.symbol || position.name} · {position.name}</option>)}</select></Field><div className="grid gap-4 sm:grid-cols-2"><Field label="Valor por cota (R$)"><input className={inputClass} inputMode="decimal" placeholder="0,0000" value={amountPerUnit} onChange={(event) => setAmountPerUnit(event.target.value.replace(/[^0-9,.]/g, ''))} /></Field><NumberField label="Imposto retido (%)" value={taxRate} onChange={setTaxRate} step="0.01" /></div><div className="grid gap-4 sm:grid-cols-2"><Field label="Data Com (opcional)"><input className={inputClass} type="date" value={exDate} onChange={(event) => setExDate(event.target.value)} /></Field><DateField label="Data de pagamento" value={paymentDate} onChange={setPaymentDate} /></div><div className="grid gap-3 rounded-2xl bg-slate-100 p-4 text-sm sm:grid-cols-3"><span><b>{formatQuantity(previewQuantity)}</b><br /><small className="text-slate-500">cotas estimadas</small></span><span><b>{currency(gross)}</b><br /><small className="text-slate-500">bruto estimado</small></span><span className="text-emerald-700"><b>{currency(net)}</b><br /><small className="text-slate-500">líquido estimado</small></span></div>{error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}<div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={!selected || amountPerUnitValue <= 0 || mutation.isPending} type="submit">{mutation.isPending ? 'Agendando...' : 'Salvar na agenda'}</button></div></form></ModalShell>;
}

function GoalDialog({ goal, open, onClose }: { goal: InvestmentGoalResponse | null; open: boolean; onClose: () => void }) {
  const createMutation = useCreateInvestmentGoalMutation();
  const updateMutation = useUpdateInvestmentGoalMutation();
  const contributionsQuery = useInvestmentGoalContributionsQuery(goal?.id ?? null, Boolean(goal && open));
  const deleteContributionMutation = useDeleteInvestmentGoalContributionMutation();
  const [name, setName] = useState(goal?.name ?? 'Patrimônio total');
  const [targetAmount, setTargetAmount] = useState(goal?.targetAmount ?? 10000);
  const [initialAmount, setInitialAmount] = useState(goal?.initialAmount ?? 0);
  const [monthlyContribution, setMonthlyContribution] = useState(goal?.monthlyContribution ?? 500);
  const [annualGrowthRate, setAnnualGrowthRate] = useState(goal?.annualGrowthRate ?? 0);
  const [error, setError] = useState('');
  if (!open) return null;
  const pending = createMutation.isPending || updateMutation.isPending;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setError('');
    const data = { name, targetAmount, initialAmount, monthlyContribution, annualGrowthRate };
    const options = { onSuccess: onClose, onError: (reason: unknown) => setError(getApiErrorMessage(reason, 'Não foi possível salvar a meta.')) };
    if (goal) updateMutation.mutate({ id: goal.id, data }, options); else createMutation.mutate(data, options);
  };
  const removeContribution = (contributionId: number) => {
    if (!goal || !window.confirm('Remover este aporte da meta?')) return;
    setError('');
    deleteContributionMutation.mutate({ goalId: goal.id, contributionId }, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível remover o aporte.')) });
  };
  return <ModalShell eyebrow="Planejamento" title={goal ? 'Editar meta de patrimônio' : 'Criar meta de patrimônio'} onClose={onClose}><form className="space-y-5" onSubmit={submit}><p className="text-sm leading-6 text-slate-500">O saldo desta meta é separado da carteira: informe o valor que você decidiu destinar a ela. A taxa anual pode ser revisada a qualquer momento.</p><Field label="Nome da meta"><input className={inputClass} required value={name} onChange={(event) => setName(event.target.value)} /></Field><div className="grid gap-4 sm:grid-cols-2"><NumberField label="Objetivo (R$)" value={targetAmount} onChange={setTargetAmount} /><NumberField label="Valor inicial destinado (R$)" value={initialAmount} onChange={setInitialAmount} /></div><div className="grid gap-4 sm:grid-cols-2"><NumberField label="Aporte mensal previsto (R$)" value={monthlyContribution} onChange={setMonthlyContribution} /><NumberField label="Variação anual estimada (%)" value={annualGrowthRate} onChange={setAnnualGrowthRate} step="0.0001" /></div>{goal && <div className="rounded-[20px] border border-slate-100 bg-slate-50 p-4"><div className="mb-3 flex items-center justify-between"><div><p className="text-sm font-semibold text-slate-800">Aportes registrados</p><p className="text-xs text-slate-500">Remova somente lançamentos inseridos por engano.</p></div><span className="text-sm font-semibold text-emerald-700">{currency(goal.contributionsAmount)}</span></div>{contributionsQuery.isLoading ? <p className="py-2 text-sm text-slate-500">Carregando aportes...</p> : (contributionsQuery.data ?? []).length === 0 ? <p className="py-2 text-sm text-slate-500">Nenhum aporte avulso registrado.</p> : <div className="space-y-2">{contributionsQuery.data?.map((contribution) => <div key={contribution.id} className="flex items-center justify-between gap-3 rounded-xl bg-white px-3 py-2.5"><span><b className="text-sm text-slate-800">{currency(contribution.amount)}</b><small className="ml-2 text-xs text-slate-500">{formatDate(contribution.eventDate)}</small></span><button className="text-xs font-semibold text-rose-600 disabled:text-slate-300" disabled={deleteContributionMutation.isPending} type="button" onClick={() => removeContribution(contribution.id)}>Remover</button></div>)}</div>}</div>}{error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}<div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={targetAmount <= 0 || pending} type="submit">{pending ? 'Salvando...' : goal ? 'Salvar alterações' : 'Criar meta'}</button></div></form></ModalShell>;
}

function GoalContributionDialog({ goal, onClose }: { goal: InvestmentGoalResponse | null; onClose: () => void }) {
  const mutation = useContributeToInvestmentGoalMutation();
  const [amount, setAmount] = useState(0);
  const [eventDate, setEventDate] = useState(today);
  const [error, setError] = useState('');
  if (!goal) return null;
  const submit = (event: FormEvent) => { event.preventDefault(); setError(''); mutation.mutate({ id: goal.id, data: { amount, eventDate } }, { onSuccess: onClose, onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível registrar o aporte.')) }); };
  return <ModalShell eyebrow="Meta" title={`Registrar aporte em ${goal.name}`} onClose={onClose}><form className="space-y-5" onSubmit={submit}><p className="rounded-[20px] bg-emerald-50 p-4 text-sm leading-6 text-slate-600">Este valor será destinado somente a esta meta. Ele não altera nem redistribui o patrimônio das outras metas.</p><div className="grid gap-4 sm:grid-cols-2"><NumberField label="Valor destinado (R$)" value={amount} onChange={setAmount} /><DateField label="Data do aporte" value={eventDate} onChange={setEventDate} /></div>{error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}<div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={amount <= 0 || mutation.isPending} type="submit">{mutation.isPending ? 'Registrando...' : 'Registrar aporte'}</button></div></form></ModalShell>;
}

function IncomeDialog({ position, onClose }: { position: InvestmentPositionResponse | null; onClose: () => void }) {
  const mutation = useRecordInvestmentIncomeMutation();
  const [amount, setAmount] = useState(0);
  const [eventDate, setEventDate] = useState(today);
  if (!position) return null;
  const movementType = position.assetType === 'RENDA_FIXA' ? 'RENDIMENTO' : 'DIVIDENDO';
  const submit = (event: FormEvent) => {
    event.preventDefault();
    mutation.mutate({ id: position.id, amount, movementType, eventDate }, { onSuccess: () => { setAmount(0); onClose(); } });
  };
  return (
    <ModalShell eyebrow="Provento" title={`Registrar em ${position.symbol || position.name}`} onClose={onClose}>
      <form className="space-y-5" onSubmit={submit}>
        <div className="rounded-[20px] border border-emerald-100 bg-emerald-50/70 p-4"><p className="text-xs font-semibold uppercase tracking-[.14em] text-emerald-700">{movementType === 'DIVIDENDO' ? 'Dividendo recebido' : 'Rendimento recebido'}</p><p className="mt-1 text-sm text-slate-600">O valor também será registrado como receita no seu histórico financeiro.</p></div>
        <div className="grid gap-4 sm:grid-cols-2"><NumberField label="Valor recebido (R$)" value={amount} onChange={setAmount} /><DateField label="Data do recebimento" value={eventDate} onChange={setEventDate} /></div>
        {mutation.isError && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{getApiErrorMessage(mutation.error, 'Não foi possível registrar o provento.')}</p>}
        <div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={amount <= 0 || mutation.isPending} type="submit">{mutation.isPending ? 'Registrando...' : 'Registrar provento'}</button></div>
      </form>
    </ModalShell>
  );
}

function AssetAnalysisDialog({ position, movements, onClose }: { position: InvestmentPositionResponse | null; movements: InvestmentMovementResponse[]; onClose: () => void }) {
  if (!position) return null;
  const currentUnitPrice = position.quote.price ?? position.averagePrice ?? 0;
  const maximumPrice = Math.max(1, currentUnitPrice, position.averagePrice ?? 0);
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/55 backdrop-blur-sm" role="dialog" aria-modal="true" aria-label={`Análise de ${position.symbol || position.name}`}>
      <div className="h-full w-full overflow-y-auto bg-white shadow-2xl sm:max-w-xl">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white/95 px-6 py-5 backdrop-blur"><div><p className="text-xs font-semibold uppercase tracking-[.18em] text-emerald-600">Painel do ativo</p><h3 className="mt-1 text-2xl font-semibold text-slate-950">{position.symbol || position.name}</h3><p className="text-sm text-slate-500">{position.name}</p></div><button className="rounded-full bg-slate-100 p-2 text-slate-500 hover:text-slate-900" type="button" onClick={onClose}><X size={20} /></button></div>
        <div className="space-y-6 p-6">
          <div className="rounded-[26px] bg-slate-950 p-5 text-white"><p className="text-xs uppercase tracking-[.15em] text-slate-400">Retorno total</p><p className={`mt-2 text-3xl font-semibold ${position.totalReturnAmount >= 0 ? 'text-emerald-300' : 'text-rose-300'}`}>{signedCurrency(position.totalReturnAmount)}</p><p className="mt-1 text-sm text-slate-400">{signedPercent(position.totalReturnPercent)} sobre o capital investido</p></div>
          <div className="grid gap-3 sm:grid-cols-2"><AnalysisMetric label="Valor investido" value={currency(position.investedAmount)} /><AnalysisMetric label="Valor atual" value={currency(position.currentValue)} /><AnalysisMetric label="Ganho de capital" value={`${signedCurrency(position.capitalGainAmount)} · ${signedPercent(position.capitalGainPercent)}`} /><AnalysisMetric label="Proventos registrados" value={currency(position.incomeAmount)} /></div>
          {position.assetType !== 'RENDA_FIXA' && <div className="rounded-[22px] border border-slate-100 p-5"><div className="mb-5 flex items-center gap-2"><BarChart3 className="text-emerald-600" size={19} /><h4 className="font-semibold text-slate-900">Preço pago x cotação atual</h4></div><PriceBar label="Preço médio" value={position.averagePrice ?? 0} maximum={maximumPrice} currencyCode={position.currency ?? 'BRL'} tone="bg-slate-400" /><PriceBar label="Cotação atual" value={currentUnitPrice} maximum={maximumPrice} currencyCode={position.quote.currency || position.currency || 'BRL'} tone="bg-emerald-500" /></div>}
          <div><div className="mb-3 flex items-center gap-2"><CalendarDays className="text-emerald-600" size={18} /><h4 className="font-semibold text-slate-900">Movimentações deste ativo</h4></div><div className="space-y-2">{movements.slice(0, 8).map((movement) => <div key={movement.id} className="flex items-center justify-between rounded-2xl bg-slate-50 p-3"><div><p className="text-sm font-semibold">{movementLabel(movement.movementType)}</p><p className="text-xs text-slate-500">{formatDate(movement.eventDate)}</p></div><p className="text-sm font-semibold">{numberCurrency(movement.amount, movement.currency)}</p></div>)}{movements.length === 0 && <p className="rounded-2xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-500">Nenhuma movimentação registrada.</p>}</div></div>
          <p className="text-xs leading-6 text-slate-400">Rentabilidade total = valorização ou desvalorização da posição + proventos registrados. Custos de compra já compõem o preço médio.</p>
        </div>
      </div>
    </div>
  );
}

function ModalShell({ eyebrow, title, onClose, children }: { eyebrow: string; title: string; onClose: () => void; children: ReactNode }) {
  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/55 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="dialog" aria-modal="true"><div className="w-full rounded-t-[30px] bg-white shadow-2xl sm:max-w-xl sm:rounded-[30px]"><div className="flex items-center justify-between border-b border-slate-100 px-6 py-5"><div><p className="text-xs font-semibold uppercase tracking-[.18em] text-emerald-600">{eyebrow}</p><h3 className="mt-1 text-xl font-semibold text-slate-950">{title}</h3></div><button className="rounded-full bg-slate-100 p-2 text-slate-500 hover:text-slate-900" type="button" onClick={onClose}><X size={20} /></button></div><div className="p-6">{children}</div></div></div>;
}

function AnalysisMetric({ label, value }: { label: string; value: string }) { return <div className="rounded-[20px] bg-slate-50 p-4"><p className="text-xs uppercase tracking-[.12em] text-slate-400">{label}</p><p className="mt-2 font-semibold text-slate-900">{value}</p></div>; }
function PriceBar({ label, value, maximum, currencyCode, tone }: { label: string; value: number; maximum: number; currencyCode: string; tone: string }) { return <div className="mb-4 last:mb-0"><div className="mb-2 flex justify-between text-sm"><span className="text-slate-500">{label}</span><strong>{numberCurrency(value, currencyCode)}</strong></div><div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${tone}`} style={{ width: `${Math.max(2, (value / maximum) * 100)}%` }} /></div></div>; }

const inputClass = 'h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white';
function NumberField({ label, value, onChange, step = '0.01' }: { label: string; value: number; onChange: (value: number) => void; step?: string }) { return <Field label={label}><input className={inputClass} min="0" step={step} type="number" value={value} onChange={(e) => onChange(Number(e.target.value))} /></Field>; }
function DateField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <Field label={label}><input className={inputClass} required type="date" value={value} onChange={(e) => onChange(e.target.value)} /></Field>; }
function PositionDatum({ label, value }: { label: string; value: string }) { return <div><p className="text-xs font-semibold uppercase tracking-[.12em] text-slate-400">{label}</p><p className="mt-1 font-semibold text-slate-800">{value}</p></div>; }
function ProjectionMetric({ label, value }: { label: string; value: string }) { return <div><p className="text-xs uppercase tracking-[.16em] text-emerald-300">{label}</p><p className="mt-2 text-2xl font-semibold">{value}</p></div>; }
function EmptyPortfolio({ onAdd }: { onAdd: () => void }) { return <div className="rounded-[22px] border border-dashed border-slate-200 p-8 text-center"><p className="text-sm leading-7 text-slate-500">Sua carteira começa vazia. Busque um ativo verificado para fazer a primeira compra.</p><button className="mt-4 rounded-full bg-emerald-500 px-5 py-2.5 text-sm font-semibold text-white" type="button" onClick={onAdd}>Buscar primeiro ativo</button></div>; }
function assetLabel(type: InvestmentAssetType) { return ({ ACAO: 'Ações', FII: 'FIIs', CRIPTO: 'Cripto', RENDA_FIXA: 'Renda fixa' })[type]; }
function currency(value: number) { return numberCurrency(value, 'BRL'); }
function numberCurrency(value: number, code: string) { try { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: code || 'BRL' }).format(value); } catch { return `${code} ${value.toFixed(2)}`; } }
function compactCurrency(value: number) { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1 }).format(value); }
function parseDecimalInput(value: string) { const normalized = value.includes(',') ? value.replace(/\./g, '').replace(',', '.') : value; const parsed = Number(normalized); return Number.isFinite(parsed) ? parsed : 0; }
function signedCurrency(value: number) { return `${value >= 0 ? '+' : '-'} ${currency(Math.abs(value))}`; }
function signedPercent(value: number) { return `${value >= 0 ? '+' : '-'} ${Math.abs(value).toFixed(2).replace('.', ',')}%`; }
function formatQuantity(value: number | null) { return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 8 }).format(value ?? 0); }
function formatDate(value: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`)); }
function shortDate(value: string) { return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(`${value}T12:00:00`)).replace('.', ''); }
function formatTimestamp(value: string) { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
function formatMonths(months: number) { const years = Math.floor(months / 12); const remainingMonths = months % 12; return [years > 0 ? `${years} ano${years === 1 ? '' : 's'}` : '', remainingMonths > 0 ? `${remainingMonths} mês${remainingMonths === 1 ? '' : 'es'}` : ''].filter(Boolean).join(' e '); }
function movementLabel(type: string) { return ({ COMPRA: 'Compra', VENDA: 'Venda', DIVIDENDO: 'Dividendo', RENDIMENTO: 'Rendimento', APORTE: 'Aporte', RESGATE: 'Resgate' } as Record<string, string>)[type] ?? type; }
