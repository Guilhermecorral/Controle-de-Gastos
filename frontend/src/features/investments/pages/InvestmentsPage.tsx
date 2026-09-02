import { FormEvent, useDeferredValue, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, Clock3, Plus, Search, X } from 'lucide-react';
import {
  InvestmentAssetSearchResponse,
  InvestmentAssetType,
  InvestmentPositionRequest,
  InvestmentPositionResponse,
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
  const incomeMutation = useRecordInvestmentIncomeMutation();
  const [tradeOpen, setTradeOpen] = useState(false);
  const [fixedForm, setFixedForm] = useState<InvestmentPositionRequest>({
    assetType: 'RENDA_FIXA', symbol: null, externalId: null, name: '', quantity: null, averagePrice: null,
    principal: 0, annualRate: 12, purchaseDate: today, maturityDate: nextYear, market: 'BR', currency: 'BRL', exchange: null,
  });
  const [projection, setProjection] = useState({ principal: 10000, annualRate: 12, startDate: today, maturityDate: nextYear });
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

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Total investido" value={currency(portfolio?.totalInvested ?? 0)} tone="neutral" />
        <MetricCard label="Valor atual" value={currency(portfolio?.currentValue ?? 0)} tone="positive" />
        <MetricCard label="Retorno estimado" value={signedCurrency(portfolio?.totalReturn ?? 0)} tone={(portfolio?.totalReturn ?? 0) >= 0 ? 'positive' : 'negative'} />
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
                    <p className={`mt-1 text-sm font-semibold ${position.returnAmount >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{signedCurrency(position.returnAmount)}</p>
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
                    {position.assetType !== 'RENDA_FIXA' && <button className="font-semibold text-slate-700 hover:text-emerald-700" type="button" onClick={() => setTradeOpen(true)}>Comprar ou vender</button>}
                    <button className="font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={() => recordIncome(position, incomeMutation.mutate)}>Registrar provento</button>
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
        <div className="grid gap-4 lg:grid-cols-4"><NumberField label="Valor inicial" value={projection.principal} onChange={(principal) => setProjection({ ...projection, principal })} /><NumberField label="Taxa anual (%)" value={projection.annualRate} onChange={(annualRate) => setProjection({ ...projection, annualRate })} /><DateField label="Início" value={projection.startDate} onChange={(startDate) => setProjection({ ...projection, startDate })} /><DateField label="Vencimento" value={projection.maturityDate} onChange={(maturityDate) => setProjection({ ...projection, maturityDate })} /></div>
        <button className="mt-4 rounded-full bg-emerald-500 px-5 py-3 font-semibold text-white hover:bg-emerald-600" type="button" onClick={() => projectionMutation.mutate(projection)}>Projetar rendimento</button>
        {projectionMutation.data && <div className="mt-5 grid gap-4 rounded-[24px] bg-slate-950 p-5 text-white md:grid-cols-3"><ProjectionMetric label="Saldo projetado" value={currency(projectionMutation.data.projectedBalance)} /><ProjectionMetric label="Rendimento" value={currency(projectionMutation.data.projectedEarnings)} /><ProjectionMetric label="Prazo" value={`${projectionMutation.data.months} meses`} /><p className="md:col-span-3 text-xs leading-6 text-slate-400">{projectionMutation.data.disclaimer}</p></div>}
      </SectionCard>

      <TradeDialog open={tradeOpen} positions={portfolio?.positions ?? []} onClose={() => setTradeOpen(false)} />
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
function formatQuantity(value: number | null) { return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 8 }).format(value ?? 0); }
function formatDate(value: string) { return new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`)); }
function formatTimestamp(value: string) { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
function movementLabel(type: string) { return ({ COMPRA: 'Compra', VENDA: 'Venda', DIVIDENDO: 'Dividendo', RENDIMENTO: 'Rendimento', APORTE: 'Aporte', RESGATE: 'Resgate' } as Record<string, string>)[type] ?? type; }
function recordIncome(position: InvestmentPositionResponse, mutate: (variables: { id: number; amount: number; movementType: 'DIVIDENDO' | 'RENDIMENTO' }) => void) { const raw = window.prompt(`Valor do ${position.assetType === 'RENDA_FIXA' ? 'rendimento' : 'dividendo'} recebido:`); if (!raw) return; const amount = Number(raw.replace(',', '.')); if (Number.isFinite(amount) && amount > 0) mutate({ id: position.id, amount, movementType: position.assetType === 'RENDA_FIXA' ? 'RENDIMENTO' : 'DIVIDENDO' }); }
