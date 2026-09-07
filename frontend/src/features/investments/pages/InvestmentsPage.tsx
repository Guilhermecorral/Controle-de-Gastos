import { FormEvent, ReactNode, useDeferredValue, useEffect, useState } from 'react';
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
  WalletEarningResponse,
} from '../../../types';
import {
  useCreateInvestmentMutation,
  useContributeToInvestmentGoalMutation,
  useCreateInvestmentGoalMutation,
  useCreateInvestmentIncomeScheduleMutation,
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
  useUpdateInvestmentMovementMutation,
  useDeleteInvestmentMovementMutation,
  useUpdateInvestmentTaxEventMutation,
  useUpdateInvestmentGoalMutation,
  useAdjustWalletEarningMutation,
  useConfirmWalletEarningMutation,
  useRevertWalletEarningMutation,
  useCorporateEventPilotAccessQuery,
  useCorporateEventPreviewMutation,
  usePublishCorporateEventPreviewMutation,
  useWalletEarningsQuery,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { ConfirmationDialog, Field, LoadingCard, MetricCard, SectionCard, UnavailableCard } from '../../shared/ui';
import OFXUploader from '../../ofx-upload/components/OFXUploader';
import { FixedIncomeRedemption, TaxRegimeFields } from '../components/FixedIncomeTools';
import TaxClosingPanel from '../components/TaxClosingPanel';
import InvestmentImportDialog from '../components/InvestmentImportDialog';

const today = new Date().toISOString().slice(0, 10);
const nextYear = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().slice(0, 10);
type TradableType = Exclude<InvestmentAssetType, 'RENDA_FIXA'>;
type MovementMode = 'COMPRA' | 'VENDA' | 'RENDA_FIXA' | 'SALDO_INICIAL';

export default function InvestmentsPage() {
  const portfolioQuery = useInvestmentPortfolioQuery();
  const movementsQuery = useInvestmentMovementsQuery();
  const schedulesQuery = useInvestmentIncomeSchedulesQuery();
  const walletEarningsQuery = useWalletEarningsQuery();
  const goalsQuery = useInvestmentGoalsQuery();
  const projectionMutation = useInvestmentProjectionMutation();
  const [tradeOpen, setTradeOpen] = useState(false);
  const [tradeSession, setTradeSession] = useState(0);
  const [investmentImportOpen, setInvestmentImportOpen] = useState(false);
  const [investmentImportFeedback, setInvestmentImportFeedback] = useState<string | null>(null);
  const [importInitial, setImportInitial] = useState(false);
  const [redemption, setRedemption] = useState<InvestmentPositionResponse | null>(null);
  const [incomePosition, setIncomePosition] = useState<InvestmentPositionResponse | null>(null);
  const [selectedPosition, setSelectedPosition] = useState<InvestmentPositionResponse | null>(null);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [goalOpen, setGoalOpen] = useState(false);
  const [editingGoal, setEditingGoal] = useState<InvestmentGoalResponse | null>(null);
  const [contributionGoal, setContributionGoal] = useState<InvestmentGoalResponse | null>(null);
  const [editingMovement, setEditingMovement] = useState<InvestmentMovementResponse | null>(null);
  const deleteMovementMutation = useDeleteInvestmentMovementMutation();
  const closeInvestmentDialogs = () => {
    setTradeOpen(false);
    setInvestmentImportOpen(false);
    setImportInitial(false);
    setRedemption(null);
    setIncomePosition(null);
    setSelectedPosition(null);
    setScheduleOpen(false);
    setGoalOpen(false);
    setEditingGoal(null);
    setContributionGoal(null);
    setEditingMovement(null);
  };
  const openTradeDialog = (openingBalance = false) => {
    closeInvestmentDialogs();
    setImportInitial(openingBalance);
    setTradeSession((current) => current + 1);
    setTradeOpen(true);
  };
  const [projection, setProjection] = useState<InvestmentProjectionRequest>({
    initialAmount: 1000,
    monthlyContribution: 500,
    interestRate: 12,
    ratePeriod: 'ANNUAL',
    timelinePeriod: 'MONTHLY',
    startDate: today,
    endDate: nextYear,
    taxRegime: 'REGRESSIVO', iofApplicable: true, annualInflationRate: 4.5,
  });
  if (portfolioQuery.isLoading) return <LoadingCard label="Buscando sua carteira e atualizando as cotações." />;
  if (portfolioQuery.isError) return <UnavailableCard label={getApiErrorMessage(portfolioQuery.error, 'Não foi possível carregar os investimentos.')} />;
  const portfolio = portfolioQuery.data;
  const positions = portfolio?.positions ?? [];
  const portfolioHasOverflow = positions.length > 3;
  const distribution = positions.reduce<Record<InvestmentAssetType, number>>((totals, position) => {
    totals[position.assetType] += position.currentValue;
    return totals;
  }, { ACAO: 0, FII: 0, FIAGRO: 0, CRIPTO: 0, RENDA_FIXA: 0 });

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[.2em] text-emerald-600">Carteira inteligente</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-950">Seus investimentos, sem cadastro no escuro</h2>
          <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">Selecione ativos verificados no catálogo e registre cada compra ou venda. A posição é calculada pelo Farol.</p>
        </div>
        <div className="flex flex-wrap gap-2"><button className="rounded-full border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={() => setInvestmentImportOpen(true)}>Importar investimentos</button><button className="button-pop button-glow flex items-center gap-2 rounded-full bg-slate-950 px-5 py-3 font-semibold text-white" type="button" onClick={() => openTradeDialog()}><Plus size={18} /> Nova movimentação</button></div>
      </header>
      {investmentImportFeedback && <p className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">{investmentImportFeedback}</p>}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Total investido" value={currency(portfolio?.totalInvested ?? 0)} tone="neutral" />
        <MetricCard label="Valor atual" value={currency(portfolio?.currentValue ?? 0)} tone="positive" />
        <MetricCard label="Ganho de capital" value={signedCurrency(portfolio?.totalCapitalGain ?? 0)} tone={(portfolio?.totalCapitalGain ?? 0) >= 0 ? 'positive' : 'negative'} />
        <MetricCard label="Retorno total" value={`${signedCurrency(portfolio?.totalReturn ?? 0)} · ${signedPercent(portfolio?.totalReturnPercent ?? 0)}`} tone={(portfolio?.totalReturn ?? 0) >= 0 ? 'positive' : 'negative'} />
      </div>

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

      <SectionCard title="Minha carteira">
          <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
            <p className="max-w-3xl text-sm leading-7 text-slate-500">Ações, FIIs e criptos são consolidados pelas compras e vendas. A fonte e o horário da cotação permanecem visíveis.</p>
            <div className="flex items-center gap-3">{portfolioHasOverflow && <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">Exibindo 3 de {positions.length} ativos</span>}<button className="text-xs font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={() => openTradeDialog(true)}>Já investia antes? Importar posição</button></div>
          </div>
          <div aria-label="Ativos da carteira" className={`space-y-3 overflow-x-hidden pr-2 ${portfolioHasOverflow ? 'max-h-[648px] overflow-y-auto' : ''}`}>
            {positions.length === 0 && <EmptyPortfolio onAdd={() => openTradeDialog()} />}
            {positions.map((position) => (
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
                    {position.assetType !== 'RENDA_FIXA' && <button className="font-semibold text-slate-700 hover:text-emerald-700" type="button" onClick={() => openTradeDialog()}>Comprar ou vender</button>}
                    <button className="font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={() => setIncomePosition(position)}>Registrar provento</button>
                    {position.assetType === 'RENDA_FIXA' && <button className="font-semibold text-emerald-700" type="button" onClick={() => setRedemption(position)}>Simular / resgatar</button>}
                  </div>
                </div>
              </article>
            ))}
          </div>
      </SectionCard>

      <div className="grid items-start gap-6 md:grid-cols-[minmax(0,1.45fr)_minmax(300px,0.8fr)]">
        <div className="min-w-0">
          <IncomeCalendar schedules={schedulesQuery.data ?? []} automaticEarnings={walletEarningsQuery.data ?? []}
            loading={schedulesQuery.isLoading || walletEarningsQuery.isLoading} onAdd={() => setScheduleOpen(true)} />
        </div>

        <div className="min-w-0 space-y-6">
          <SectionCard title="Últimas movimentações">
            <div className="max-h-[252px] space-y-3 overflow-y-auto overflow-x-hidden pr-2">
              {(movementsQuery.data ?? []).map((movement) => (
                <div key={movement.id} className="flex items-center justify-between gap-3 rounded-[18px] bg-slate-50 p-3">
                  <div className="flex items-center gap-3">
                    <span className={`flex h-9 w-9 items-center justify-center rounded-xl ${movement.movementType === 'VENDA' ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-700'}`}>{movement.movementType === 'VENDA' ? <ArrowUpRight size={17} /> : <ArrowDownLeft size={17} />}</span>
                    <div><p className="text-sm font-semibold text-slate-900">{movement.assetName}</p><p className="text-xs text-slate-500">{movementLabel(movement.movementType)} · {formatDate(movement.eventDate)}</p>{movement.realizedGain != null && <p className={`text-xs ${movement.realizedGain >= 0 ? 'text-emerald-700' : 'text-rose-600'}`}>Resultado: {numberCurrency(movement.realizedGain, movement.currency)}</p>}</div>
                  </div>
                  <div className="text-right"><span className="text-sm font-semibold text-slate-700">{numberCurrency(movement.amount, movement.currency)}</span>{!movement.automatic && <div className="mt-1 flex justify-end gap-2"><button className="text-xs font-semibold text-slate-600 hover:text-emerald-700" type="button" onClick={() => setEditingMovement(movement)} disabled={movement.movementType !== 'COMPRA' && movement.movementType !== 'VENDA'}>Editar</button><button className="text-xs font-semibold text-rose-600 hover:text-rose-800" type="button" disabled={deleteMovementMutation.isPending} onClick={() => { if (window.confirm(`Excluir ${movementLabel(movement.movementType).toLowerCase()}? A carteira, o fluxo financeiro e a apuração relacionada serão recalculados.`)) deleteMovementMutation.mutate(movement.id); }}>Excluir</button></div>}</div>
                </div>
              ))}
              {(movementsQuery.data ?? []).length === 0 && <p className="rounded-[20px] border border-dashed border-slate-200 p-5 text-center text-sm text-slate-500">As compras e vendas aparecerão aqui.</p>}
            </div>
          </SectionCard>

          <GoalsPanel goals={goalsQuery.data ?? []} loading={goalsQuery.isLoading} onAdd={() => { setEditingGoal(null); setGoalOpen(true); }} onContribute={setContributionGoal} onEdit={(goal) => { setEditingGoal(goal); setGoalOpen(true); }} />
        </div>
      </div>

      <PortfolioEvolution points={portfolio?.evolution ?? []} />

      <TaxAndReconciliationPanel />
      <TaxClosingPanel />

      <SectionCard title="Simulador de renda fixa">
        <TaxRegimeFields value={projection} onChange={(fields) => setProjection({ ...projection, ...fields, taxRegime: fields.taxRegime ?? projection.taxRegime })} />
        <p className="mb-5 max-w-3xl text-sm leading-7 text-slate-500">Simule juros compostos com aporte único ou mensal. Escolha como a taxa foi informada e como deseja acompanhar a evolução.</p>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <NumberField label="Valor inicial" value={projection.initialAmount} onChange={(initialAmount) => setProjection({ ...projection, initialAmount })} />
          <NumberField label="Aporte mensal" value={projection.monthlyContribution} onChange={(monthlyContribution) => setProjection({ ...projection, monthlyContribution })} />
          <NumberField label="Taxa de juros (%)" value={projection.interestRate} onChange={(interestRate) => setProjection({ ...projection, interestRate })} step="0.0001" />
          <Field label="Período da taxa"><select className={inputClass} value={projection.ratePeriod} onChange={(event) => setProjection({ ...projection, ratePeriod: event.target.value as 'MONTHLY' | 'ANNUAL' })}><option value="MONTHLY">Mensal</option><option value="ANNUAL">Anual</option></select></Field>
          <DateField label="Data inicial" value={projection.startDate} onChange={(startDate) => setProjection({ ...projection, startDate })} />
          <DateField label="Data final" value={projection.endDate} onChange={(endDate) => setProjection({ ...projection, endDate })} />
          <NumberField label="IPCA / inflação projetada (% a.a.)" value={projection.annualInflationRate} onChange={(annualInflationRate) => setProjection({ ...projection, annualInflationRate })} step="0.01" />
          <div className="flex items-end"><button className="h-12 w-full rounded-2xl bg-emerald-500 px-5 font-semibold text-white hover:bg-emerald-600 disabled:bg-slate-300" disabled={projectionMutation.isPending || projection.initialAmount + projection.monthlyContribution <= 0} type="button" onClick={() => projectionMutation.mutate(projection)}>{projectionMutation.isPending ? 'Calculando...' : 'Calcular evolução'}</button></div>
        </div>
        {projectionMutation.isError && <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{getApiErrorMessage(projectionMutation.error, 'Não foi possível calcular esta simulação.')}</p>}
        {projectionMutation.data && <ProjectionResults result={projectionMutation.data} startDate={projection.startDate} />}
      </SectionCard>

      {tradeOpen && <TradeDialog key={`${importInitial ? 'opening' : 'new'}-${tradeSession}`} open positions={portfolio?.positions ?? []} initialMode={importInitial} onClose={closeInvestmentDialogs} />}
      <InvestmentImportDialog open={investmentImportOpen} onClose={() => setInvestmentImportOpen(false)} onFinished={setInvestmentImportFeedback} />
      <FixedIncomeRedemption position={redemption} onClose={() => setRedemption(null)} />
      <IncomeDialog position={incomePosition} onClose={() => setIncomePosition(null)} />
      <IncomeScheduleDialog open={scheduleOpen} positions={portfolio?.positions ?? []} onClose={() => setScheduleOpen(false)} />
      <GoalDialog key={editingGoal?.id ?? 'new'} goal={editingGoal} open={goalOpen} onClose={() => { setGoalOpen(false); setEditingGoal(null); }} />
      <GoalContributionDialog goal={contributionGoal} onClose={() => setContributionGoal(null)} />
      <AssetAnalysisDialog position={selectedPosition} movements={(movementsQuery.data ?? []).filter((movement) => movement.positionId === selectedPosition?.id)} onClose={() => setSelectedPosition(null)} />
      <MovementCorrectionDialog movement={editingMovement} onClose={() => setEditingMovement(null)} />
    </div>
  );
}

function TradeDialog({ open, positions, initialMode, onClose }: { open: boolean; positions: InvestmentPositionResponse[]; initialMode: boolean; onClose: () => void }) {
  const mutation = useRecordInvestmentTradeMutation();
  const fixedIncomeMutation = useCreateInvestmentMutation();
  const [mode, setMode] = useState<MovementMode>(initialMode ? 'SALDO_INICIAL' : 'COMPRA');
  const [assetType, setAssetType] = useState<TradableType>('ACAO');
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  const [selected, setSelected] = useState<InvestmentAssetSearchResponse | null>(null);
  const [positionId, setPositionId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(0);
  const [fees, setFees] = useState(0);
  const [brokerageFee, setBrokerageFee] = useState(0);
  const [b3Fee, setB3Fee] = useState(0);
  const [withheldTax, setWithheldTax] = useState(0);
  const [exchangeRate, setExchangeRate] = useState(0);
  const [requestId, setRequestId] = useState(() => crypto.randomUUID());
  const [eventDate, setEventDate] = useState(today);
  const [error, setError] = useState('');
  const [dismissed, setDismissed] = useState(false);
  const [fixedForm, setFixedForm] = useState<InvestmentPositionRequest>({
    assetType: 'RENDA_FIXA', symbol: null, externalId: null, name: '', quantity: null, averagePrice: null,
    principal: 0, annualRate: 12, purchaseDate: today, maturityDate: nextYear, market: 'BR', currency: 'BRL', exchange: null,
    taxRegime: 'REGRESSIVO', iofApplicable: true, fixedIncomeYieldType: 'PREFIXADO', fixedIncomeIndexer: null, dailyLiquidity: false,
  });
  const search = useInvestmentAssetSearchQuery(deferredQuery, assetType, open && (mode === 'COMPRA' || mode === 'SALDO_INICIAL') && !selected);
  if (!open || dismissed) return null;

  const closeDialog = () => {
    setDismissed(true);
    onClose();
  };

  const chooseAsset = (asset: InvestmentAssetSearchResponse, id: number | null = null, price?: number | null) => {
    setSelected(asset); setPositionId(id); setUnitPrice(price ?? asset.currentPrice ?? 0); setError(''); setRequestId(crypto.randomUUID());
  };
  const choosePosition = (position: InvestmentPositionResponse) => chooseAsset({
    assetType: position.assetType as TradableType, symbol: position.symbol ?? '', externalId: position.externalId ?? '',
    name: position.name, market: (position.market ?? 'BR') as 'BR' | 'US' | 'GLOBAL', exchange: position.exchange ?? '',
    currency: position.currency ?? position.quote.currency, currentPrice: position.quote.price, source: position.quote.source,
  }, position.id, position.quote.price);
  const resetSelection = () => { setSelected(null); setPositionId(null); setQuery(''); setUnitPrice(0); setError(''); };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (mode === 'RENDA_FIXA') {
      fixedIncomeMutation.mutate(fixedForm, {
        onSuccess: () => {
          setFixedForm((current) => ({ ...current, name: '', principal: 0 }));
          onClose();
        },
        onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível salvar a aplicação.')),
      });
      return;
    }
    if (!selected) return setError('Selecione um ativo verificado para continuar.');
    if (mode === 'SALDO_INICIAL') {
      fixedIncomeMutation.mutate({ assetType: selected.assetType, symbol: selected.symbol, externalId: selected.externalId,
        name: selected.name, market: selected.market, exchange: selected.exchange, currency: selected.currency,
        quantity, averagePrice: unitPrice, principal: null, annualRate: null, purchaseDate: eventDate, maturityDate: null, openingDate: eventDate },
        { onSuccess: () => { resetSelection(); onClose(); }, onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível importar o saldo.')) });
      return;
    }
    const payload: InvestmentTradeRequest = { positionId, movementType: mode, assetType: selected.assetType as TradableType, symbol: selected.symbol,
      externalId: selected.externalId, name: selected.name, market: selected.market, exchange: selected.exchange,
      currency: selected.currency, quantity, unitPrice, fees: 0, eventDate,
      // Assets priced in BRL do not need an FX rate. Sending the form's initial zero
      // would fail the API validation before the backend can apply BRL = 1.
      exchangeRate: selected.currency === 'BRL' ? undefined : exchangeRate, requestId,
      costs: { brokerageFee, b3Fee, otherCosts: fees, withheldTax: mode === 'VENDA' ? withheldTax : 0 } };
    mutation.mutate(payload, { onSuccess: () => { onClose(); resetSelection(); }, onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível registrar a movimentação.')) });
  };

  const sellable = positions.filter((position) => position.assetType !== 'RENDA_FIXA' && (position.quantity ?? 0) > 0);
  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-slate-950/55 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="dialog" aria-modal="true" aria-label="Nova movimentação">
      <div className="max-h-[92vh] w-full overflow-y-auto rounded-t-[30px] bg-white shadow-2xl sm:max-w-2xl sm:rounded-[30px]">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white/95 px-6 py-5 backdrop-blur">
          <div><p className="text-xs font-semibold uppercase tracking-[.18em] text-emerald-600">Investimentos</p><h3 className="mt-1 text-xl font-semibold text-slate-950">Nova movimentação</h3></div>
          <button className="rounded-full bg-slate-100 p-2 text-slate-500 hover:text-slate-900" type="button" onClick={closeDialog}><X size={20} /></button>
        </div>
        <form className="space-y-5 p-6" onSubmit={submit}>
          {mode === 'SALDO_INICIAL' ? <><div className="flex items-center justify-between"><div><p className="text-sm font-semibold text-slate-900">Importar posição existente</p><p className="mt-1 text-xs leading-5 text-slate-500">Use o saldo e o preço médio da sua corretora. Não será criada uma despesa antiga no financeiro.</p></div><button className="text-sm font-semibold text-slate-600" type="button" onClick={() => { setMode('COMPRA'); resetSelection(); }}>Voltar</button></div></> : <div className="grid grid-cols-2 gap-1 rounded-2xl bg-slate-100 p-1">
            <button className={`rounded-xl px-3 py-3 text-sm font-semibold transition ${mode !== 'RENDA_FIXA' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} type="button" onClick={() => { setMode('COMPRA'); resetSelection(); }}>Renda variável</button>
            <button className={`rounded-xl px-3 py-3 text-sm font-semibold transition ${mode === 'RENDA_FIXA' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} type="button" onClick={() => { setMode('RENDA_FIXA'); resetSelection(); }}>Renda fixa</button>
          </div>}
          {mode !== 'RENDA_FIXA' && mode !== 'SALDO_INICIAL' && <div className="grid grid-cols-2 gap-1 rounded-2xl bg-slate-100 p-1"><button className={`rounded-xl px-3 py-2.5 text-sm font-semibold ${mode === 'COMPRA' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} type="button" onClick={() => { setMode('COMPRA'); resetSelection(); }}>Compra</button><button className={`rounded-xl px-3 py-2.5 text-sm font-semibold ${mode === 'VENDA' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} type="button" onClick={() => { setMode('VENDA'); resetSelection(); }}>Venda</button></div>}

          {!selected && (mode === 'COMPRA' || mode === 'SALDO_INICIAL') && <>
            <div className="flex flex-wrap gap-2">{(['ACAO', 'FII', 'FIAGRO', 'CRIPTO'] as TradableType[]).map((type) => <button key={type} className={`rounded-full border px-4 py-2 text-sm font-semibold ${assetType === type ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 text-slate-600'}`} type="button" onClick={() => { setAssetType(type); setQuery(''); }}>{assetLabel(type)}</button>)}</div>
            <Field label="Busque pelo ticker ou nome do ativo"><div className="relative"><Search className="absolute left-4 top-3.5 text-slate-400" size={19} /><input autoFocus className={`${inputClass} pl-11`} value={query} onChange={(e) => setQuery(e.target.value)} placeholder={assetType === 'CRIPTO' ? 'Bitcoin, Ethereum...' : 'ITUB4, Itaú, Apple...'} /></div></Field>
            <div className="space-y-2">
              {search.isFetching && <p className="py-4 text-center text-sm text-slate-500">Consultando o catálogo...</p>}
              {!search.isFetching && deferredQuery.length >= 2 && search.data?.length === 0 && <p className="rounded-2xl border border-dashed border-slate-200 p-5 text-center text-sm text-slate-500">Nenhum ativo verificado foi encontrado.</p>}
              {search.data?.map((asset) => <button key={`${asset.market}-${asset.externalId}`} className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-emerald-200 hover:bg-emerald-50/40" type="button" onClick={() => chooseAsset(asset)}><div><p className="font-semibold text-slate-950">{asset.symbol} <span className="ml-2 text-sm font-normal text-slate-500">{asset.name}</span></p><p className="mt-1 text-xs text-slate-500">{asset.market} · {asset.exchange} · {asset.currency}</p></div>{asset.currentPrice != null && <span className="text-sm font-semibold text-slate-700">{numberCurrency(asset.currentPrice, asset.currency)}</span>}</button>)}
            </div>
          </>}

          {!selected && mode === 'VENDA' && <div className="space-y-2"><p className="text-sm font-semibold text-slate-700">Selecione uma posição disponível</p>{sellable.map((position) => <button key={position.id} className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-rose-200" type="button" onClick={() => choosePosition(position)}><div><p className="font-semibold text-slate-950">{position.symbol || position.name}</p><p className="mt-1 text-xs text-slate-500">{position.name} · disponível {formatQuantity(position.quantity)}</p></div><span className="text-sm font-semibold text-slate-700">{numberCurrency(position.quote.price ?? position.averagePrice ?? 0, position.currency ?? 'BRL')}</span></button>)}</div>}

          {mode === 'RENDA_FIXA' && <div className="space-y-4">
            <TaxRegimeFields value={fixedForm} onChange={(fields) => setFixedForm({ ...fixedForm, ...fields })} />
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={!!fixedForm.openingDate} onChange={(e) => setFixedForm({ ...fixedForm, openingDate: e.target.checked ? today : null })} /> Aplicação que eu já possuía (sem nova saída de dinheiro)</label>
            <div className="rounded-[20px] border border-emerald-100 bg-emerald-50/60 p-4"><p className="text-sm font-semibold text-emerald-800">Adicionar uma aplicação de renda fixa</p><p className="mt-1 text-xs leading-5 text-slate-500">Informe o valor aplicado, a taxa anual e o período do investimento.</p></div>
            <Field label="Nome da aplicação"><input className={inputClass} required value={fixedForm.name} onChange={(event) => setFixedForm({ ...fixedForm, name: event.target.value })} placeholder="CDB, Tesouro ou LCI" /></Field>
            <div className="grid gap-4 sm:grid-cols-2"><Field label="Rentabilidade"><select className={inputClass} value={fixedForm.fixedIncomeYieldType ?? 'PREFIXADO'} onChange={(event) => setFixedForm({ ...fixedForm, fixedIncomeYieldType: event.target.value as 'PREFIXADO' | 'POS_FIXADO' | 'HIBRIDO' })}><option value="PREFIXADO">Prefixado</option><option value="POS_FIXADO">Pós-fixado</option><option value="HIBRIDO">Híbrido</option></select></Field>{fixedForm.fixedIncomeYieldType !== 'PREFIXADO' && <Field label="Indexador"><select className={inputClass} value={fixedForm.fixedIncomeIndexer ?? 'CDI'} onChange={(event) => setFixedForm({ ...fixedForm, fixedIncomeIndexer: event.target.value })}><option value="CDI">CDI</option><option value="SELIC">Selic</option><option value="IPCA">IPCA</option></select></Field>}</div>
            <div className="grid gap-4 sm:grid-cols-2"><NumberField label="Valor aplicado" value={fixedForm.principal ?? 0} onChange={(principal) => setFixedForm({ ...fixedForm, principal })} /><NumberField label="Taxa anual (%)" value={fixedForm.annualRate ?? 12} onChange={(annualRate) => setFixedForm({ ...fixedForm, annualRate })} /></div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4"><label className="flex items-center gap-3 text-sm font-semibold text-slate-800"><input type="checkbox" checked={!!fixedForm.dailyLiquidity} onChange={(event) => setFixedForm({ ...fixedForm, dailyLiquidity: event.target.checked, maturityDate: event.target.checked ? null : nextYear })} /> Liquidez diária (sem vencimento definido)</label><p className="mt-2 text-xs leading-5 text-slate-500">Com liquidez diária, você poderá simular ou registrar o resgate em qualquer data posterior à aplicação.</p></div>
            <div className="grid gap-4 sm:grid-cols-2"><DateField label="Aplicação" value={fixedForm.purchaseDate} onChange={(purchaseDate) => setFixedForm({ ...fixedForm, purchaseDate })} max={today} />{!fixedForm.dailyLiquidity && <DateField label="Vencimento" value={fixedForm.maturityDate ?? nextYear} onChange={(maturityDate) => setFixedForm({ ...fixedForm, maturityDate })} />}</div>
          </div>}

          {selected && <>
            <div className="flex items-center justify-between rounded-[22px] border border-emerald-100 bg-emerald-50/60 p-4"><div><p className="text-xs font-semibold uppercase tracking-[.16em] text-emerald-700">Ativo verificado</p><p className="mt-1 text-lg font-semibold text-slate-950">{selected.symbol} · {selected.name}</p><p className="mt-1 text-xs text-slate-500">{selected.market} · {selected.exchange} · preço em {selected.currency}</p></div><button className="text-sm font-semibold text-slate-600" type="button" onClick={resetSelection}>Trocar</button></div>
            <div className="grid gap-4 sm:grid-cols-2"><NumberField label="Quantidade" value={quantity} onChange={setQuantity} step="0.00000001" /><NumberField label={mode === 'SALDO_INICIAL' ? 'Custo médio' : 'Preço unitário'} value={unitPrice} onChange={setUnitPrice} step="0.000001" /><DateField label={mode === 'SALDO_INICIAL' ? 'Data de início do acompanhamento' : 'Data da operação'} value={eventDate} onChange={setEventDate} max={today} /></div>
            {mode !== 'SALDO_INICIAL' && <details className="rounded-2xl border border-slate-200 p-4"><summary className="cursor-pointer text-sm font-semibold text-slate-700">Custos da operação / Nota de corretagem (opcional)</summary><div className="mt-4 grid gap-4 sm:grid-cols-2"><NumberField label="Corretagem" value={brokerageFee} onChange={setBrokerageFee} /><NumberField label="Taxas B3" value={b3Fee} onChange={setB3Fee} /><NumberField label="Outros custos" value={fees} onChange={setFees} />{mode === 'VENDA' && <NumberField label="IRRF antecipado" value={withheldTax} onChange={setWithheldTax} />}</div><p className="mt-3 text-xs text-slate-500">Valores da operação, na moeda do ativo. IRRF é crédito tributário e não compõe os custos.</p></details>}
            {selected.currency !== 'BRL' && mode !== 'SALDO_INICIAL' && <NumberField label={`Câmbio da operação (R$ por ${selected.currency})`} value={exchangeRate} onChange={setExchangeRate} step="0.000001" />}
            <div className="flex items-center justify-between rounded-2xl bg-slate-100 px-4 py-3"><span className="text-sm font-semibold text-slate-600">Valor {mode === 'VENDA' ? 'líquido' : 'investido'}</span><strong className="text-slate-950">{numberCurrency(Math.max(0, quantity * unitPrice + (mode === 'SALDO_INICIAL' ? 0 : mode === 'COMPRA' ? fees + brokerageFee + b3Fee : -fees - brokerageFee - b3Fee - withheldTax)), selected.currency)}</strong></div>
          </>}
          {error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
          <button className="w-full rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={mode === 'RENDA_FIXA' ? fixedIncomeMutation.isPending || !fixedForm.name.trim() || (fixedForm.principal ?? 0) <= 0 : !selected || mutation.isPending || quantity <= 0 || unitPrice <= 0} type="submit">{mode === 'RENDA_FIXA' ? fixedIncomeMutation.isPending ? 'Adicionando...' : 'Adicionar aplicação' : mutation.isPending ? 'Registrando...' : `Registrar ${mode === 'COMPRA' ? 'compra' : 'venda'}`}</button>
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

function ProjectionResults({ result, startDate }: { result: InvestmentProjectionResponse; startDate: string }) {
  const investedShare = result.projectedBalance > 0 ? Math.min(100, (result.totalInvested / result.projectedBalance) * 100) : 0;
  const hasTaxEstimate = [result.incomeTax, result.iof, result.netBalance].every((value) => Number.isFinite(Number(value)));
  const monthlyPoints = projectionMonthlyPoints(result, startDate);
  return (
    <div className="mt-6 overflow-hidden rounded-[26px] bg-slate-950 text-white">
      <div className="grid gap-5 p-6 md:grid-cols-3">
        <ProjectionMetric label="Saldo final" value={currency(result.projectedBalance)} />
        <ProjectionMetric label="Total investido" value={currency(result.totalInvested)} />
        <ProjectionMetric label="Juros ganhos" value={currency(result.projectedEarnings)} />
        <ProjectionMetric label="IR estimado" value={formatOptionalCurrency(result.incomeTax)} />
        <ProjectionMetric label="IOF estimado" value={formatOptionalCurrency(result.iof)} />
        <ProjectionMetric label="Líquido no resgate" value={formatOptionalCurrency(result.netBalance)} />
        <ProjectionMetric label="Poder de compra real" value={formatOptionalCurrency(result.realNetBalance)} />
      </div>
      {!hasTaxEstimate && <p className="border-t border-amber-300/20 bg-amber-400/10 px-6 py-3 text-sm text-amber-100">Os impostos ainda não foram calculados porque esta API não enviou os campos fiscais da v1.4.1. Atualize o backend e tente novamente.</p>}
      <div className="border-y border-white/10 px-6 py-5">
        <div className="mb-3 flex justify-between text-xs text-slate-300"><span>Composição do saldo</span><span>{result.months} meses · {result.effectiveMonthlyRate.toFixed(4).replace('.', ',')}% a.m.</span></div>
        <div className="flex h-4 overflow-hidden rounded-full bg-emerald-400"><div className="bg-slate-400" style={{ width: `${investedShare}%` }} /></div>
        <div className="mt-3 flex flex-wrap gap-5 text-xs"><span className="flex items-center gap-2 text-slate-300"><i className="h-2.5 w-2.5 rounded-full bg-slate-400" />Investido {currency(result.totalInvested)}</span><span className="flex items-center gap-2 text-emerald-300"><i className="h-2.5 w-2.5 rounded-full bg-emerald-400" />Juros {currency(result.projectedEarnings)}</span></div>
      </div>
      <ProjectionLineChart points={projectionChartPoints(monthlyPoints)} />
      <div className="overflow-x-auto bg-white text-slate-800">
        <table className="w-full min-w-[980px] text-left text-sm">
          <thead className="bg-slate-100 text-xs uppercase tracking-[.1em] text-slate-500"><tr>{['Mês/Ano', 'Aportes', 'Investido', 'Saldo bruto', 'IR estimado', 'IOF estimado', 'Saldo líquido'].map((label) => <th key={label} className="px-5 py-4">{label}</th>)}</tr></thead>
          <tbody>{monthlyPoints.map((point) => <tr key={`${point.date}-${point.month}`} className="border-t border-slate-100"><td className="px-5 py-4 text-slate-500">{monthYear(point.date)}</td>{[point.contribution, point.totalInvested, point.balance, point.incomeTax, point.iof, point.netBalance].map((amount, index) => <td key={index} className="px-5 py-4 text-right">{formatOptionalCurrency(amount)}</td>)}</tr>)}</tbody>
        </table>
      </div>
      <p className="px-6 py-4 text-xs leading-6 text-slate-400">{result.disclaimer}</p>
    </div>
  );
}

function projectionMonthlyPoints(result: InvestmentProjectionResponse, startDate: string) {
  const initialPoint: InvestmentProjectionResponse['timeline'][number] = { month: 0, date: startDate, contribution: 0, interest: 0, totalInvested: result.initialAmount, totalInterest: 0, balance: result.initialAmount, incomeTax: 0, iof: 0, netBalance: result.initialAmount };
  return [initialPoint, ...result.timeline];
}

function projectionChartPoints(points: InvestmentProjectionResponse['timeline']) {
  const indexes = [0, 0.25, 0.5, 0.75, 1].map((ratio) => Math.round((points.length - 1) * ratio));
  return [...new Set(indexes)].map((index) => points[index]);
}

function ProjectionLineChart({ points }: { points: InvestmentProjectionResponse['timeline'] }) {
  if (points.length < 2) return null;
  const width = 960;
  const height = 300;
  const chart = { top: 24, right: 28, bottom: 38, left: 88 };
  const plotWidth = width - chart.left - chart.right;
  const plotHeight = height - chart.top - chart.bottom;
  const values = points.map((point) => point.balance);
  const rawMinimum = Math.min(...values);
  const rawMaximum = Math.max(...values);
  const rangePadding = Math.max((rawMaximum - rawMinimum) * 0.16, rawMaximum * 0.025, 1);
  const minimum = Math.max(0, rawMinimum - rangePadding);
  const maximum = rawMaximum + rangePadding;
  const domain = Math.max(1, maximum - minimum);
  const xAt = (index: number) => chart.left + (index / Math.max(1, points.length - 1)) * plotWidth;
  const yAt = (value: number) => chart.top + ((maximum - value) / domain) * plotHeight;
  const linePath = points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${xAt(index)} ${yAt(point.balance)}`).join(' ');
  const areaPath = `${linePath} L ${xAt(points.length - 1)} ${chart.top + plotHeight} L ${xAt(0)} ${chart.top + plotHeight} Z`;
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    y: chart.top + ratio * plotHeight,
    value: maximum - ratio * domain,
  }));
  const labelIndexes = Array.from(new Set([0, Math.floor((points.length - 1) / 2), points.length - 1]));

  return <div className="bg-white px-6 py-5 text-slate-900"><div className="mb-4 flex flex-wrap items-end justify-between gap-4"><div><p className="text-sm font-semibold">Evolução do saldo</p><p className="mt-1 text-xs text-slate-400">Cinco marcos da projeção mensal: início, quartis e vencimento.</p></div><div className="flex items-center gap-2 text-xs font-semibold text-slate-600"><i className="h-2.5 w-2.5 rounded-full bg-emerald-500" />Saldo bruto projetado</div></div><div className="overflow-x-auto rounded-[24px] border border-slate-100 bg-slate-50/80 p-3 sm:p-5"><svg className="h-[280px] min-w-[720px] w-full" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gráfico de linha da evolução do saldo"><defs><linearGradient id="projectionArea" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#10b981" stopOpacity="0.2" /><stop offset="100%" stopColor="#10b981" stopOpacity="0.01" /></linearGradient></defs>{yTicks.map((tick) => <g key={tick.y}><line stroke="#e2e8f0" strokeWidth="1" x1={chart.left} x2={width - chart.right} y1={tick.y} y2={tick.y} /><text fill="#94a3b8" fontSize="12" textAnchor="end" x={chart.left - 12} y={tick.y + 4}>{compactCurrency(tick.value)}</text></g>)}<path d={areaPath} fill="url(#projectionArea)" /><path d={linePath} fill="none" stroke="#10b981" strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" />{points.map((point, index) => <circle key={point.month} cx={xAt(index)} cy={yAt(point.balance)} fill="white" r="5" stroke="#10b981" strokeWidth="3"><title>{`${formatDate(point.date)} · Saldo bruto ${currency(point.balance)} · Investido ${currency(point.totalInvested)}`}</title></circle>)}{labelIndexes.map((index) => <text key={points[index].month} fill="#94a3b8" fontSize="12" textAnchor={index === 0 ? 'start' : index === points.length - 1 ? 'end' : 'middle'} x={xAt(index)} y={height - 8}>{shortDate(points[index].date)}</text>)}</svg><p className="mt-1 text-right text-[11px] text-slate-400">Escala ajustada aos marcos exibidos</p></div></div>;
}

function TaxAndReconciliationPanel() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [importOpen, setImportOpen] = useState(false);
  const taxQuery = useInvestmentTaxSummaryQuery(year);
  const reconciliationQuery = useInvestmentReconciliationQuery(year);
  const taxEventMutation = useUpdateInvestmentTaxEventMutation();
  const tax = taxQuery.data;
  const reconciliation = reconciliationQuery.data;
  const refresh = () => { taxQuery.refetch(); reconciliationQuery.refetch(); };
  return <SectionCard title="Tributação e conciliação">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-4"><div><p className="max-w-2xl text-sm leading-6 text-slate-500">Confira impostos retidos em proventos e compare as movimentações da carteira com seu extrato importado. Vendas ficam sinalizadas para apuração, porque as regras dependem do ativo e do resultado do período.</p></div><div className="flex gap-2"><select aria-label="Ano da apuração" className="h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-700" value={year} onChange={(event) => setYear(Number(event.target.value))}>{[currentYear, currentYear - 1, currentYear - 2].map((option) => <option key={option} value={option}>{option}</option>)}</select><button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={() => setImportOpen((open) => !open)}>{importOpen ? 'Fechar importação' : 'Importar extrato'}</button></div></div>
    {importOpen && <div className="mb-6 rounded-[24px] border border-emerald-100 bg-emerald-50/40 p-4 sm:p-5"><p className="mb-4 text-sm leading-6 text-slate-600">Envie OFX, CSV, TSV ou Excel. Revise as linhas antes de salvar; depois, a conciliação será atualizada. O importador registra lançamentos financeiros e não cria compras ou vendas de ativos automaticamente.</p><OFXUploader compact onImported={() => { setImportOpen(false); refresh(); }} /></div>}
    <div className="grid gap-4 md:grid-cols-3"><FiscalMetric label="Imposto retido (BRL)" value={taxQuery.isLoading ? '...' : currency(tax?.totalWithheld ?? 0)} helper="Eventos em reais ou convertidos pelo câmbio informado" tone="positive" /><FiscalMetric label="Eventos para revisar" value={taxQuery.isLoading ? '...' : String(tax?.reviewCount ?? 0)} helper="Sem cálculo automático" tone={(tax?.reviewCount ?? 0) > 0 ? 'warning' : 'neutral'} /><FiscalMetric label="Extrato conciliado" value={reconciliationQuery.isLoading ? '...' : `${reconciliation?.reconciledCount ?? 0}/${(reconciliation?.items ?? []).length}`} helper={`${reconciliation?.pendingCount ?? 0} pendente(s)`} tone={(reconciliation?.pendingCount ?? 0) > 0 ? 'warning' : 'positive'} /></div>
    <div className="mt-6 grid gap-6 md:grid-cols-2"><div className="min-w-0"><div className="mb-3 flex items-center justify-between"><h4 className="font-semibold text-slate-900">Eventos fiscais</h4><span className="text-xs text-slate-400">{year}</span></div>{(tax?.events ?? []).length === 0 ? <EmptyFiscal label="Nenhum provento recebido ou venda registrada neste ano." /> : <div className="max-h-[360px] space-y-2 overflow-y-auto overflow-x-hidden pr-2">{tax?.events.map((event, index) => <div key={`${event.date}-${event.symbol}-${index}`} className="rounded-2xl bg-slate-50 p-3"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{event.symbol || event.assetName} · {event.eventType.toLowerCase()}</p><p className="mt-1 text-xs text-slate-500">{formatDate(event.date)} · {event.note}</p></div><TaxBadge status={event.status} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs"><span className="text-slate-500">Bruto <b className="text-slate-700">{numberCurrency(event.grossAmount, event.currency ?? 'BRL')}</b></span><span className="text-slate-500">Retido <b className="text-emerald-700">{numberCurrency(event.withheldAmount, event.currency ?? 'BRL')}</b></span><span className="text-slate-500">Líquido <b className="text-slate-700">{numberCurrency(event.netAmount, event.currency ?? 'BRL')}</b></span></div>{event.movementId != null && <div className="mt-3 flex flex-wrap gap-2"><button className="text-xs font-semibold text-emerald-700 hover:text-emerald-900" type="button" disabled={taxEventMutation.isPending} onClick={() => taxEventMutation.mutate({ movementId: event.movementId!, status: 'ISENTO', withheldAmount: 0, note: 'Evento marcado como isento pelo usuário.' })}>Marcar como isento</button><button className="text-xs font-semibold text-slate-600 hover:text-slate-950" type="button" disabled={taxEventMutation.isPending} onClick={() => { const value = window.prompt('Informe o imposto retido no comprovante (R$).', String(event.withheldAmount)); if (value == null) return; const withheldAmount = Number(value.replace(',', '.')); if (Number.isFinite(withheldAmount) && withheldAmount >= 0) taxEventMutation.mutate({ movementId: event.movementId!, status: withheldAmount > 0 ? 'RETIDO_INTEGRAL' : 'A_RECOLHER', withheldAmount, note: 'Retenção ajustada pelo usuário.' }); }}>Editar retenção</button></div>}</div>)}</div>}</div><div className="min-w-0"><div className="mb-3 flex items-center justify-between"><h4 className="font-semibold text-slate-900">Conciliação do extrato</h4><button className="text-xs font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={refresh}>Atualizar</button></div>{(reconciliation?.items ?? []).length === 0 ? <EmptyFiscal label="Ainda não há movimentações de investimento no período." /> : <div className="max-h-[360px] space-y-2 overflow-y-auto overflow-x-hidden pr-2">{reconciliation?.items.map((item) => <div key={item.movementId} className="rounded-2xl bg-slate-50 p-3"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{item.symbol || item.assetName} · {movementLabel(item.movementType)}</p><p className="mt-1 text-xs text-slate-500">{formatDate(item.eventDate)} · {item.note}</p></div><ReconciliationBadge status={item.status} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs"><span className="text-slate-500">Carteira <b className="text-slate-700">{numberCurrency(item.expectedAmount, item.currency)}</b></span>{item.transactionAmount != null && <span className="text-slate-500">Extrato <b className="text-slate-700">{currency(item.transactionAmount)}</b></span>}</div></div>)}</div>}</div></div>
    <p className="mt-5 text-xs leading-6 text-slate-400">Este painel organiza valores já registrados. Para DARF, compensações, isenções ou operações complexas, use-o como conferência e valide a apuração com sua documentação fiscal.</p>
  </SectionCard>;
}

function FiscalMetric({ label, value, helper, tone }: { label: string; value: string; helper: string; tone: 'neutral' | 'positive' | 'warning' }) { const color = tone === 'positive' ? 'text-emerald-700' : tone === 'warning' ? 'text-amber-700' : 'text-slate-800'; return <div className="rounded-[20px] border border-slate-100 bg-slate-50 p-4"><p className="text-xs font-semibold uppercase tracking-[.12em] text-slate-400">{label}</p><p className={`mt-2 text-xl font-semibold ${color}`}>{value}</p><p className="mt-1 text-xs text-slate-500">{helper}</p></div>; }
function EmptyFiscal({ label }: { label: string }) { return <div className="rounded-[20px] border border-dashed border-slate-200 bg-slate-50 p-5 text-center text-sm text-slate-500">{label}</div>; }
function TaxBadge({ status }: { status: 'RETIDO_INTEGRAL' | 'RETIDO_ANTECIPACAO' | 'A_RECOLHER' | 'ISENTO' }) { const options = { RETIDO_INTEGRAL: ['Retido na fonte', 'bg-emerald-100 text-emerald-800'], RETIDO_ANTECIPACAO: ['IRRF antecipado', 'bg-sky-100 text-sky-800'], A_RECOLHER: ['Pode gerar DARF', 'bg-amber-100 text-amber-800'], ISENTO: ['Isento', 'bg-slate-200 text-slate-600'] } as const; const [label, classes] = options[status]; return <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${classes}`}>{label}</span>; }
function ReconciliationBadge({ status }: { status: 'CONCILIADO' | 'GERADO_PELO_FAROL' | 'PENDENTE' | 'REVISAR' }) { const options = { CONCILIADO: ['Conciliado', 'bg-emerald-100 text-emerald-800'], GERADO_PELO_FAROL: ['Farol', 'bg-sky-100 text-sky-800'], PENDENTE: ['Pendente', 'bg-amber-100 text-amber-800'], REVISAR: ['Revisar', 'bg-slate-200 text-slate-600'] } as const; const [label, classes] = options[status]; return <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${classes}`}>{label}</span>; }

function IncomeCalendar({ schedules, automaticEarnings, loading, onAdd }: { schedules: InvestmentIncomeScheduleResponse[]; automaticEarnings: WalletEarningResponse[]; loading: boolean; onAdd: () => void }) {
  const receiveMutation = useReceiveInvestmentIncomeScheduleMutation();
  const deleteMutation = useDeleteInvestmentIncomeScheduleMutation();
  const confirmEarningMutation = useConfirmWalletEarningMutation();
  const revertEarningMutation = useRevertWalletEarningMutation();
  const adjustEarningMutation = useAdjustWalletEarningMutation();
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const [adjustingEarning, setAdjustingEarning] = useState<WalletEarningResponse | null>(null);
  const [cancellingEarning, setCancellingEarning] = useState<WalletEarningResponse | null>(null);
  const [revertingEarning, setRevertingEarning] = useState<WalletEarningResponse | null>(null);
  const pending = schedules.filter((schedule) => schedule.status === 'AGUARDANDO');
  const automaticPending = automaticEarnings.filter((earning) => earning.status === 'PROVISIONADO' || earning.status === 'PENDENTE_CONCILIACAO');
  const receivedSchedules = schedules.filter((schedule) => schedule.status === 'RECEBIDO');
  const historicalEarnings = automaticEarnings.filter((earning) => earning.status === 'EFETIVADO' || earning.status === 'CANCELADO');
  const visibleSchedules = showHistory ? receivedSchedules : pending;
  const visibleAutomaticEarnings = showHistory ? historicalEarnings : automaticPending;
  const historyCount = historicalEarnings.length + receivedSchedules.length;
  const upcomingValue = [...pending, ...automaticPending].reduce((total, item) => total + item.netAmount, 0);
  const receive = (id: number) => {
    setError('');
    receiveMutation.mutate(id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível confirmar o recebimento.')) });
  };
  const remove = (id: number) => {
    setError('');
    deleteMutation.mutate(id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível remover o provento.')) });
  };
  const adjust = (earning: WalletEarningResponse) => {
    setAdjustingEarning(earning);
  };
  const cancel = (earning: WalletEarningResponse) => {
    setCancellingEarning(earning);
  };
  return <>
    <SectionCard title="Agenda de proventos">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
      <div><p className="text-sm text-slate-500">Eventos automáticos congelam a quantidade na Data Com e só viram receita após sua confirmação.</p><p className="mt-2 text-sm font-semibold text-emerald-700">{currency(upcomingValue)} líquidos aguardando</p></div>
      <div className="flex flex-wrap gap-2"><CorporateEventPilotPanel />{historyCount > 0 && <button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={() => setShowHistory((current) => !current)}>{showHistory ? 'Ocultar histórico' : `Ver histórico (${historyCount})`}</button>}<button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={onAdd}><Plus className="mr-1 inline" size={15} /> Agendar manualmente</button></div>
    </div>
    {error && <p className="mb-3 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
    {notice && <p className="mb-3 rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{notice}</p>}
    {showHistory && <WalletEarningHistoryActions earnings={historicalEarnings} onRevert={setRevertingEarning} onRestore={(earning) => {
      setError('');
      setNotice('');
      adjustEarningMutation.mutate({ id: earning.id, data: { reopened: true } }, {
        onSuccess: () => setNotice('Previsão restaurada na Agenda para nova revisão.'),
        onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível restaurar a previsão.')),
      });
    }} busy={adjustEarningMutation.isPending || revertEarningMutation.isPending} />}
    {loading ? <p className="py-6 text-center text-sm text-slate-500">Carregando agenda...</p> : visibleSchedules.length === 0 && visibleAutomaticEarnings.length === 0 ? <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 p-6 text-center"><CalendarDays className="mx-auto text-slate-400" size={24} /><p className="mt-3 text-sm font-semibold text-slate-700">{showHistory ? 'Nenhum registro no histórico' : 'Nenhum provento aguardando'}</p><p className="mt-1 text-sm text-slate-500">{showHistory ? 'Os proventos confirmados aparecem no Histórico Financeiro como receitas de investimento.' : 'Atualize a agenda beta para revisar os eventos encontrados na B3. Se uma fonte ainda não trouxer o evento, use o agendamento manual como contingência.'}</p></div> : <div className="max-h-[720px] space-y-3 overflow-y-auto overflow-x-hidden pr-2">{visibleAutomaticEarnings.map((earning) => <article key={`automatic-${earning.id}`} className="rounded-[20px] border border-emerald-100 bg-emerald-50/40 p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><div className="flex items-center gap-2"><p className="font-semibold text-slate-900">{earning.symbol || earning.assetName}</p><WalletEarningBadge status={earning.status} /></div><p className="mt-1 text-xs text-slate-500">{earning.eventType === 'JCP' ? 'JCP' : earning.eventType === 'RENDIMENTO' ? 'Rendimento' : 'Dividendo'} automático · fonte {earning.source}{earning.payerCnpj ? ` · CNPJ ${earning.payerCnpj}` : ''}</p></div><div className="text-right"><p className="font-semibold text-slate-900">{currency(earning.netAmount)}</p><p className="text-xs text-slate-500">{earning.status === 'EFETIVADO' ? 'recebido' : 'líquido previsto'}</p></div></div><div className="mt-3 grid gap-2 border-t border-emerald-100 pt-3 text-xs text-slate-500 sm:grid-cols-3"><span>Data Com: <strong className="text-slate-700">{formatDate(earning.exDate)}</strong></span><span>Pagamento: <strong className="text-slate-700">{formatDate(earning.paymentDate)}</strong></span><span>{formatQuantity(earning.quantityEligible)} cotas · <strong className="text-slate-700">{currency(earning.grossAmount)} bruto</strong></span></div><p className="mt-2 text-xs text-slate-500">{earning.eventType === 'JCP' ? `JCP com IRRF de 15,00%: ${currency(earning.withheldAmount)} retidos.` : `${earning.eventType === 'RENDIMENTO' ? 'Rendimento' : 'Dividendo'} sem retenção prevista: ${currency(earning.withheldAmount)} retidos.`}</p>{(earning.status === 'PROVISIONADO' || earning.status === 'PENDENTE_CONCILIACAO') && <div className="mt-3 flex flex-wrap justify-end gap-3"><button className="text-sm font-semibold text-slate-600 hover:text-slate-950" disabled={adjustEarningMutation.isPending} type="button" onClick={() => adjust(earning)}>Ajustar valor</button><button className="text-sm font-semibold text-slate-500 hover:text-rose-700" disabled={adjustEarningMutation.isPending} type="button" onClick={() => cancel(earning)}>Cancelar</button>{earning.status === 'PENDENTE_CONCILIACAO' && <button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300" disabled={confirmEarningMutation.isPending} type="button" onClick={() => confirmEarningMutation.mutate(earning.id, { onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível confirmar o recebimento.')) })}>{confirmEarningMutation.isPending ? 'Confirmando...' : 'Confirmar recebimento'}</button>}</div>}</article>)}{visibleSchedules.map((schedule) => <article key={schedule.id} className="rounded-[20px] border border-slate-100 bg-slate-50 p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><div className="flex items-center gap-2"><p className="font-semibold text-slate-900">{schedule.symbol || schedule.assetName}</p><span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${schedule.status === 'RECEBIDO' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{schedule.status === 'RECEBIDO' ? 'Recebido' : 'Aguardando'}</span></div><p className="mt-1 text-xs text-slate-500">Manual · {schedule.incomeType === 'DIVIDENDO' ? 'Dividendo' : 'Rendimento'} · {schedule.amountPerUnit.toFixed(4).replace('.', ',')} por cota</p></div><div className="text-right"><p className="font-semibold text-slate-900">{currency(schedule.netAmount)}</p><p className="text-xs text-slate-500">líquido estimado</p></div></div><div className="mt-3 grid gap-2 border-t border-slate-200 pt-3 text-xs text-slate-500 sm:grid-cols-3"><span>Data Com: <strong className="text-slate-700">{schedule.exDate ? formatDate(schedule.exDate) : 'Não informada'}</strong></span><span>Pagamento: <strong className="text-slate-700">{formatDate(schedule.paymentDate)}</strong></span><span>Imposto: <strong className="text-slate-700">{schedule.taxRate.toFixed(2).replace('.', ',')}% · {currency(schedule.taxAmount)}</strong></span></div>{schedule.status === 'AGUARDANDO' && <div className="mt-3 flex justify-end gap-3"><button className="text-sm font-semibold text-slate-500 hover:text-rose-700" disabled={deleteMutation.isPending} type="button" onClick={() => remove(schedule.id)}>Remover</button><button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300" disabled={receiveMutation.isPending} type="button" onClick={() => receive(schedule.id)}>{receiveMutation.isPending ? 'Confirmando...' : 'Confirmar recebimento'}</button></div>}</article>)}</div>}
    </SectionCard>
    <WalletEarningAdjustmentDialog earning={adjustingEarning} busy={adjustEarningMutation.isPending} onClose={() => setAdjustingEarning(null)} onSave={(grossAmount, withheldAmount) => {
      setError('');
      adjustEarningMutation.mutate({ id: adjustingEarning!.id, data: { grossAmount, withheldAmount } }, {
        onSuccess: () => setAdjustingEarning(null),
        onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível ajustar o provento.')),
      });
    }} />
    <ConfirmationDialog open={cancellingEarning != null} title="Cancelar previsão de provento" description={`A previsão de ${cancellingEarning?.symbol || cancellingEarning?.assetName || ''} sairá da Agenda, sem criar receita. Você poderá restaurá-la pelo histórico se mudar de ideia.`} confirmLabel="Cancelar previsão" busy={adjustEarningMutation.isPending} onClose={() => setCancellingEarning(null)} onConfirm={() => {
      if (!cancellingEarning) return;
      setError('');
      adjustEarningMutation.mutate({ id: cancellingEarning.id, data: { cancelled: true } }, {
        onSuccess: () => setCancellingEarning(null),
        onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível cancelar o provento.')),
      });
    }} />
    <ConfirmationDialog open={revertingEarning != null} title="Desfazer recebimento confirmado" description={`Isso removerá a receita de ${revertingEarning?.symbol || revertingEarning?.assetName || ''} do Histórico Financeiro, o movimento vinculado e o reflexo fiscal. A previsão voltará para a Agenda para você revisar ou corrigir.`} confirmLabel="Desfazer recebimento" busy={revertEarningMutation.isPending} onClose={() => setRevertingEarning(null)} onConfirm={() => {
      if (!revertingEarning) return;
      setError('');
      revertEarningMutation.mutate(revertingEarning.id, {
        onSuccess: () => {
          setRevertingEarning(null);
          setNotice('Recebimento desfeito. A previsão voltou para a Agenda e a receita vinculada foi removida.');
        },
        onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível desfazer o recebimento.')),
      });
    }} />
  </>;
}

function WalletEarningHistoryActions({ earnings, onRevert, onRestore, busy }: { earnings: WalletEarningResponse[]; onRevert: (earning: WalletEarningResponse) => void; onRestore: (earning: WalletEarningResponse) => void; busy: boolean }) {
  const reversible = earnings.filter((earning) => earning.status === 'EFETIVADO' || earning.status === 'CANCELADO');
  if (reversible.length === 0) return null;
  return <div className="mt-4 rounded-[20px] border border-slate-200 bg-slate-50 p-4"><p className="text-sm font-semibold text-slate-800">Correções do histórico</p><p className="mt-1 text-xs leading-5 text-slate-500">Recebimentos confirmados podem ser desfeitos com segurança. Previsões canceladas podem voltar para a Agenda.</p><div className="mt-3 space-y-2">{reversible.map((earning) => <div key={earning.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-white px-3 py-2.5"><span className="text-sm font-semibold text-slate-700">{earning.symbol || earning.assetName} · {currency(earning.netAmount)}</span>{earning.status === 'EFETIVADO' ? <button className="text-xs font-semibold text-rose-700 hover:text-rose-900 disabled:text-slate-300" type="button" disabled={busy} onClick={() => onRevert(earning)}>Desfazer recebimento</button> : <button className="text-xs font-semibold text-emerald-700 hover:text-emerald-900 disabled:text-slate-300" type="button" disabled={busy} onClick={() => onRestore(earning)}>Restaurar previsão</button>}</div>)}</div></div>;
}

function WalletEarningAdjustmentDialog({ earning, busy, onClose, onSave }: { earning: WalletEarningResponse | null; busy: boolean; onClose: () => void; onSave: (grossAmount: number, withheldAmount: number) => void }) {
  const [gross, setGross] = useState('');
  const [withheld, setWithheld] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    setGross(earning ? String(earning.grossAmount).replace('.', ',') : '');
    setWithheld(earning ? String(earning.withheldAmount).replace('.', ',') : '');
    setError('');
  }, [earning]);
  if (!earning) return null;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const grossAmount = parseDecimalInput(gross);
    const withheldAmount = parseDecimalInput(withheld);
    if (grossAmount <= 0 || withheldAmount < 0 || withheldAmount > grossAmount) {
      setError('Informe um valor bruto positivo e uma retenção entre zero e o valor bruto.');
      return;
    }
    onSave(grossAmount, withheldAmount);
  };
  return <ModalShell eyebrow="Agenda" title={`Ajustar ${earning.symbol || earning.assetName}`} onClose={onClose}><form className="space-y-5" onSubmit={submit}><p className="rounded-[20px] bg-amber-50 p-4 text-sm leading-6 text-amber-900">Use os valores do comprovante. A receita só será criada quando você confirmar o recebimento.</p><div className="grid gap-4 sm:grid-cols-2"><Field label="Valor bruto (R$)"><input className={inputClass} inputMode="decimal" value={gross} onChange={(event) => setGross(event.target.value.replace(/[^0-9,.]/g, ''))} /></Field><Field label="IRRF retido (R$)"><input className={inputClass} inputMode="decimal" value={withheld} onChange={(event) => setWithheld(event.target.value.replace(/[^0-9,.]/g, ''))} /></Field></div>{error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}<div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" disabled={busy} onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={busy} type="submit">{busy ? 'Salvando...' : 'Salvar ajuste'}</button></div></form></ModalShell>;
}

function CorporateEventPilotPanel() {
  const accessQuery = useCorporateEventPilotAccessQuery();
  const previewMutation = useCorporateEventPreviewMutation();
  const publishMutation = usePublishCorporateEventPreviewMutation();
  const [selectedReferences, setSelectedReferences] = useState<string[]>([]);
  const [error, setError] = useState('');
  const preview = previewMutation.data ?? [];
  const providerLabel = accessQuery.data?.provider === 'B3CorporateEventProvider' ? 'B3 experimental' : 'fonte demonstrativa';
  const validCount = preview.filter((event) => event.status === 'VALIDO').length;
  const toggle = (reference: string) => setSelectedReferences((current) => current.includes(reference)
    ? current.filter((item) => item !== reference)
    : [...current, reference]);
  const loadPreview = () => {
    setError('');
    previewMutation.mutate(undefined, {
      onSuccess: (events) => setSelectedReferences(events.filter((event) => event.status === 'VALIDO').map((event) => event.sourceReference)),
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível consultar a fonte experimental da B3.')),
    });
  };
  const publish = () => {
    setError('');
    publishMutation.mutate(selectedReferences, {
      onSuccess: () => { setSelectedReferences([]); },
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível adicionar as previsões à Agenda.')),
    });
  };
  if (!accessQuery.data?.available) return null;

  return <div className="w-full">
    <button className="rounded-full border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-100 disabled:bg-slate-100 disabled:text-slate-400" disabled={previewMutation.isPending} type="button" onClick={loadPreview}>{previewMutation.isPending ? 'Consultando B3...' : 'Atualizar agenda (beta)'}</button>
    {(previewMutation.data || error) && <div className="mt-3 w-full rounded-[20px] border border-amber-200 bg-amber-50/70 p-4 text-sm text-slate-700">
      <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-semibold text-amber-900">Agenda de Proventos Experimental</p><p className="mt-1 max-w-2xl text-xs leading-5 text-amber-800">Fonte ativa: {providerLabel}. Confira valor, Data Com e pagamento antes de considerar o recebimento. Nenhuma receita é criada automaticamente.</p></div><span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-amber-800">{validCount} previsão(ões) válida(s)</span></div>
      {error && <p className="mt-3 rounded-xl bg-rose-50 px-3 py-2 text-xs text-rose-700">{error}</p>}
      {preview.length > 0 && <div className="mt-4 max-h-72 space-y-2 overflow-y-auto pr-1">{preview.map((event) => <label key={event.sourceReference} className={`flex gap-3 rounded-xl border p-3 ${event.status === 'VALIDO' ? 'border-emerald-200 bg-white' : 'border-slate-200 bg-slate-100/80'}`}><input className="mt-1" type="checkbox" checked={selectedReferences.includes(event.sourceReference)} disabled={event.status !== 'VALIDO'} onChange={() => toggle(event.sourceReference)} /><span className="min-w-0 flex-1"><span className="flex flex-wrap items-center gap-2"><b>{event.symbol || 'Ticker não identificado'}</b><span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${event.status === 'VALIDO' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'}`}>{event.status === 'VALIDO' ? 'Pronto' : 'Revisar'}</span></span><span className="mt-1 block text-xs text-slate-500">{event.eventType === 'JCP' ? 'JCP' : event.eventType === 'RENDIMENTO' ? 'Rendimento' : 'Dividendo'} · ISIN {event.isinCode || 'não informado'} · Data Com {formatDate(event.exDate)} · Pagamento {formatDate(event.paymentDate)}</span><span className="mt-1 block text-xs text-slate-600">{event.status === 'VALIDO' ? `${formatQuantity(event.quantityEligible)} cotas · ${currency(event.netAmount)} líquido previsto` : event.reason}</span></span></label>)}</div>}
      {preview.length === 0 && !error && <p className="mt-3 text-xs text-slate-600">Nenhum evento foi encontrado pela fonte ativa. No piloto B3, confira no log do servidor as linhas `[B3_EVENTS]` após esta atualização.</p>}
      {selectedReferences.length > 0 && <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><p className="text-xs text-slate-600">Adicionar cria somente previsões. O recebimento continua dependendo de confirmação manual.</p><button className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300" disabled={publishMutation.isPending} type="button" onClick={publish}>{publishMutation.isPending ? 'Adicionando...' : 'Adicionar previsões à Agenda'}</button></div>}
    </div>}
  </div>;
}

function WalletEarningBadge({ status }: { status: WalletEarningResponse['status'] }) {
  const labels = { PROVISIONADO: ['Provisionado', 'bg-sky-100 text-sky-800'], PENDENTE_CONCILIACAO: ['A confirmar', 'bg-amber-100 text-amber-800'], EFETIVADO: ['Efetivado', 'bg-emerald-100 text-emerald-800'], CANCELADO: ['Cancelado', 'bg-slate-200 text-slate-600'] } as const;
  const [label, classes] = labels[status];
  return <span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${classes}`}>{label}</span>;
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

function MovementCorrectionDialog({ movement, onClose }: { movement: InvestmentMovementResponse | null; onClose: () => void }) {
  const mutation = useUpdateInvestmentMovementMutation();
  const [quantity, setQuantity] = useState(movement?.quantity ?? 0);
  const [unitPrice, setUnitPrice] = useState(movement?.unitPrice ?? 0);
  const [eventDate, setEventDate] = useState(movement?.eventDate ?? today);
  const [fees, setFees] = useState(movement?.fees ?? 0);
  const [error, setError] = useState('');
  if (!movement) return null;
  const costs = movement.costs ?? { brokerageFee: 0, b3Fee: 0, otherCosts: fees, withheldTax: 0 };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setError('');
    mutation.mutate({ id: movement.id, data: { quantity, unitPrice, fees, eventDate,
      exchangeRate: movement.currency === 'BRL' ? undefined : movement.exchangeRate ?? undefined,
      costs: { ...costs, otherCosts: fees } } }, {
      onSuccess: onClose,
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível atualizar a movimentação.')),
    });
  };
  return <ModalShell eyebrow="Correção" title={`Editar ${movementLabel(movement.movementType).toLowerCase()}`} onClose={onClose}><form className="space-y-5" onSubmit={submit}>
    <p className="rounded-[20px] border border-amber-100 bg-amber-50 p-4 text-sm leading-6 text-amber-900">Esta alteração recalcula a quantidade, o preço médio e a transação financeira vinculada. Se a venda já entrou em uma DARF paga, confirme novamente a apuração daquela competência.</p>
    <div className="grid gap-4 sm:grid-cols-2"><NumberField label="Quantidade" value={quantity} onChange={setQuantity} step="0.00000001" /><NumberField label={`Preço unitário (${movement.currency})`} value={unitPrice} onChange={setUnitPrice} step="0.000001" /><NumberField label="Custos da operação" value={fees} onChange={setFees} /><DateField label="Data da operação" value={eventDate} onChange={setEventDate} max={today} /></div>
    {movement.currency !== 'BRL' && <p className="rounded-2xl bg-slate-50 p-3 text-xs text-slate-600">O câmbio histórico atual será preservado nesta edição. Para corrigi-lo, exclua a movimentação e registre-a novamente com o comprovante.</p>}
    {error && <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
    <div className="flex justify-end gap-3"><button className="rounded-full px-5 py-3 font-semibold text-slate-600" type="button" onClick={onClose}>Cancelar</button><button className="rounded-full bg-slate-950 px-5 py-3 font-semibold text-white disabled:bg-slate-300" disabled={mutation.isPending || quantity <= 0 || unitPrice <= 0} type="submit">{mutation.isPending ? 'Salvando...' : 'Salvar correção'}</button></div>
  </form></ModalShell>;
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
function DateField({ label, value, onChange, max }: { label: string; value: string; onChange: (value: string) => void; max?: string }) { return <Field label={label}><input className={inputClass} required type="date" max={max} value={value} onChange={(e) => onChange(e.target.value)} /></Field>; }
function PositionDatum({ label, value }: { label: string; value: string }) { return <div><p className="text-xs font-semibold uppercase tracking-[.12em] text-slate-400">{label}</p><p className="mt-1 font-semibold text-slate-800">{value}</p></div>; }
function ProjectionMetric({ label, value }: { label: string; value: string }) { return <div><p className="text-xs uppercase tracking-[.16em] text-emerald-300">{label}</p><p className="mt-2 text-2xl font-semibold">{value}</p></div>; }
function EmptyPortfolio({ onAdd }: { onAdd: () => void }) { return <div className="rounded-[22px] border border-dashed border-slate-200 p-8 text-center"><p className="text-sm leading-7 text-slate-500">Sua carteira começa vazia. Busque um ativo verificado para fazer a primeira compra.</p><button className="mt-4 rounded-full bg-emerald-500 px-5 py-2.5 text-sm font-semibold text-white" type="button" onClick={onAdd}>Buscar primeiro ativo</button></div>; }
function assetLabel(type: InvestmentAssetType) { return ({ ACAO: 'Ações', FII: 'FIIs', FIAGRO: 'Fiagros', CRIPTO: 'Cripto', RENDA_FIXA: 'Renda fixa' })[type]; }
function currency(value: number) { return numberCurrency(value, 'BRL'); }
function numberCurrency(value: number, code: string) { try { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: code || 'BRL' }).format(value); } catch { return `${code} ${value.toFixed(2)}`; } }
function formatOptionalCurrency(value: number | null | undefined) { return Number.isFinite(Number(value)) ? currency(Number(value)) : 'A calcular'; }
function compactCurrency(value: number) { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1 }).format(value); }
function parseDecimalInput(value: string) { const normalized = value.includes(',') ? value.replace(/\./g, '').replace(',', '.') : value; const parsed = Number(normalized); return Number.isFinite(parsed) ? parsed : 0; }
function signedCurrency(value: number) { return `${value >= 0 ? '+' : '-'} ${currency(Math.abs(value))}`; }
function signedPercent(value: number) { return `${value >= 0 ? '+' : '-'} ${Math.abs(value).toFixed(2).replace('.', ',')}%`; }
function formatQuantity(value: number | null) { return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 8 }).format(value ?? 0); }
function formatDate(value: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`)); }
function monthYear(value: string) { return new Intl.DateTimeFormat('pt-BR', { month: '2-digit', year: 'numeric' }).format(new Date(`${value}T12:00:00`)); }
function shortDate(value: string) { return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(`${value}T12:00:00`)).replace('.', ''); }
function formatTimestamp(value: string) { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
function formatMonths(months: number) { const years = Math.floor(months / 12); const remainingMonths = months % 12; return [years > 0 ? `${years} ano${years === 1 ? '' : 's'}` : '', remainingMonths > 0 ? `${remainingMonths} mês${remainingMonths === 1 ? '' : 'es'}` : ''].filter(Boolean).join(' e '); }
function movementLabel(type: string) { return ({ COMPRA: 'Compra', VENDA: 'Venda', DIVIDENDO: 'Dividendo', RENDIMENTO: 'Rendimento', APORTE: 'Aporte', RESGATE: 'Resgate', SALDO_INICIAL: 'Saldo inicial' } as Record<string, string>)[type] ?? type; }
