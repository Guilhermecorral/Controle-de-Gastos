import { FormEvent, ReactNode, useDeferredValue, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, BarChart3, CalendarDays, Clock3, Plus, Search, X } from 'lucide-react';
import {
  InvestmentAssetSearchResponse,
  InvestmentAssetType,
  InvestmentMovementResponse,
  InvestmentPositionRequest,
  InvestmentPositionResponse,
  InvestmentProjectionRequest,
  InvestmentProjectionResponse,
  InvestmentTradeRequest,
} from '../../../types';
import {
  useCreateInvestmentMutation,
  useDeleteInvestmentMutation,
  useInvestmentAssetSearchQuery,
  useInvestmentMovementsQuery,
  useInvestmentPortfolioQuery,
  useInvestmentProjectionMutation,
  useRecordInvestmentIncomeMutation,
  useRecordInvestmentTradeMutation,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { Field, LoadingCard, MetricCard, SectionCard, UnavailableCard } from '../../shared/ui';

const today = new Date().toISOString().slice(0, 10);
const nextYear = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().slice(0, 10);
type TradableType = Exclude<InvestmentAssetType, 'RENDA_FIXA'>;

export default function InvestmentsPage() {
  const portfolioQuery = useInvestmentPortfolioQuery();
  const movementsQuery = useInvestmentMovementsQuery();
  const createMutation = useCreateInvestmentMutation();
  const deleteMutation = useDeleteInvestmentMutation();
  const projectionMutation = useInvestmentProjectionMutation();
  const [tradeOpen, setTradeOpen] = useState(false);
  const [incomePosition, setIncomePosition] = useState<InvestmentPositionResponse | null>(null);
  const [selectedPosition, setSelectedPosition] = useState<InvestmentPositionResponse | null>(null);
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
  const width = 900;
  const height = 230;
  const padding = 24;
  const maximum = Math.max(1, ...points.flatMap((point) => [point.investedAmount, point.currentValue]));
  const coordinates = (key: 'investedAmount' | 'currentValue') => points.map((point, index) => {
    const x = points.length === 1 ? width / 2 : padding + (index / (points.length - 1)) * (width - padding * 2);
    const y = height - padding - (point[key] / maximum) * (height - padding * 2);
    return `${x},${y}`;
  }).join(' ');

  return (
    <SectionCard title="Evolução patrimonial">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-500">Retratos diários reais da carteira, sem estimar cotações passadas.</p>
        <div className="flex gap-4 text-xs font-semibold text-slate-600"><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-slate-400" />Investido</span><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-emerald-500" />Valor atual</span></div>
      </div>
      {points.length > 1 ? (
        <div className="overflow-hidden rounded-[22px] bg-slate-50 p-3">
          <svg aria-label="Gráfico da evolução patrimonial" className="h-56 w-full" preserveAspectRatio="none" role="img" viewBox={`0 0 ${width} ${height}`}>
            {[0.25, 0.5, 0.75].map((ratio) => <line key={ratio} stroke="#e2e8f0" strokeWidth="1" x1="0" x2={width} y1={height * ratio} y2={height * ratio} />)}
            <polyline fill="none" points={coordinates('investedAmount')} stroke="#94a3b8" strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" />
            <polyline fill="none" points={coordinates('currentValue')} stroke="#10b981" strokeLinecap="round" strokeLinejoin="round" strokeWidth="5" />
          </svg>
          <div className="flex justify-between px-2 text-xs text-slate-400"><span>{formatDate(points[0].date)}</span><span>{formatDate(points[points.length - 1].date)}</span></div>
        </div>
      ) : (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-[22px] border border-dashed border-slate-200 bg-slate-50 p-5">
          <div><p className="font-semibold text-slate-800">O histórico começa hoje</p><p className="mt-1 text-sm text-slate-500">Novos pontos serão registrados diariamente conforme a carteira for consultada.</p></div>
          {points[0] && <div className="text-right"><p className="text-xs uppercase tracking-[.14em] text-slate-400">Patrimônio em {formatDate(points[0].date)}</p><p className="mt-1 text-xl font-semibold text-emerald-700">{currency(points[0].currentValue)}</p></div>}
        </div>
      )}
    </SectionCard>
  );
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
          <thead className="bg-slate-100 text-xs uppercase tracking-[.1em] text-slate-500"><tr><th className="px-5 py-4">Período</th><th className="px-5 py-4">Data</th><th className="px-5 py-4">Taxa no período</th><th className="px-5 py-4">Aporte mensal</th><th className="px-5 py-4">Juros do período</th><th className="px-5 py-4">Total investido</th><th className="px-5 py-4">Total em juros</th><th className="px-5 py-4 text-right">Saldo</th></tr></thead>
          <tbody>{result.timeline.map((point, index) => <tr key={point.month} className="border-t border-slate-100"><td className="px-5 py-4 font-semibold">{result.timelinePeriod === 'YEARLY' ? `${Math.ceil(point.month / 12)}º ano` : `${point.month}º mês`}</td><td className="px-5 py-4 text-slate-500">{formatDate(point.date)}</td><td className="px-5 py-4">{periodRate(index).toFixed(4).replace('.', ',')}%</td><td className="px-5 py-4">{currency(point.contribution)}</td><td className="px-5 py-4 text-emerald-700">{currency(point.interest)}</td><td className="px-5 py-4">{currency(point.totalInvested)}</td><td className="px-5 py-4 text-emerald-700">{currency(point.totalInterest)}</td><td className="px-5 py-4 text-right font-semibold">{currency(point.balance)}</td></tr>)}</tbody>
        </table>
      </div>
      <p className="px-6 py-4 text-xs leading-6 text-slate-400">{result.disclaimer}</p>
    </div>
  );
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
function signedCurrency(value: number) { return `${value >= 0 ? '+' : '-'} ${currency(Math.abs(value))}`; }
function signedPercent(value: number) { return `${value >= 0 ? '+' : '-'} ${Math.abs(value).toFixed(2).replace('.', ',')}%`; }
function formatQuantity(value: number | null) { return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 8 }).format(value ?? 0); }
function formatDate(value: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`)); }
function formatTimestamp(value: string) { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
function movementLabel(type: string) { return ({ COMPRA: 'Compra', VENDA: 'Venda', DIVIDENDO: 'Dividendo', RENDIMENTO: 'Rendimento', APORTE: 'Aporte', RESGATE: 'Resgate' } as Record<string, string>)[type] ?? type; }
