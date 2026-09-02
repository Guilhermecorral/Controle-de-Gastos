import { FormEvent, useState } from 'react';
import { InvestmentAssetType, InvestmentPositionRequest } from '../../../types';
import {
  useCreateInvestmentMutation,
  useDeleteInvestmentMutation,
  useInvestmentPortfolioQuery,
  useInvestmentProjectionMutation,
  useRecordInvestmentIncomeMutation,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { Field, LoadingCard, MetricCard, SectionCard, UnavailableCard } from '../../shared/ui';

const today = new Date().toISOString().slice(0, 10);
const nextYear = new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().slice(0, 10);

export default function InvestmentsPage() {
  const portfolioQuery = useInvestmentPortfolioQuery();
  const createMutation = useCreateInvestmentMutation();
  const deleteMutation = useDeleteInvestmentMutation();
  const projectionMutation = useInvestmentProjectionMutation();
  const incomeMutation = useRecordInvestmentIncomeMutation();
  const [form, setForm] = useState<InvestmentPositionRequest>({
    assetType: 'ACAO', symbol: '', externalId: null, name: '', quantity: 1, averagePrice: 0,
    principal: null, annualRate: null, purchaseDate: today, maturityDate: null,
  });
  const [projection, setProjection] = useState({ principal: 10000, annualRate: 12, startDate: today, maturityDate: nextYear });
  const [feedback, setFeedback] = useState('');
  const fixed = form.assetType === 'RENDA_FIXA';
  const crypto = form.assetType === 'CRIPTO';

  if (portfolioQuery.isLoading) return <LoadingCard label="Buscando sua carteira e atualizando as cotações." />;
  if (portfolioQuery.isError) return <UnavailableCard label={getApiErrorMessage(portfolioQuery.error, 'Não foi possível carregar os investimentos.')} />;
  const portfolio = portfolioQuery.data;
  const distribution = (portfolio?.positions ?? []).reduce<Record<InvestmentAssetType, number>>((totals, position) => {
    totals[position.assetType] += position.currentValue;
    return totals;
  }, { ACAO: 0, FII: 0, CRIPTO: 0, RENDA_FIXA: 0 });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setFeedback('');
    createMutation.mutate(form, {
      onSuccess: () => {
        setFeedback('Investimento adicionado à carteira.');
        setForm((current) => ({ ...current, symbol: '', externalId: crypto ? '' : null, name: '', quantity: fixed ? null : 1, averagePrice: fixed ? null : 0, principal: fixed ? 0 : null }));
      },
      onError: (error) => setFeedback(getApiErrorMessage(error, 'Não foi possível salvar o investimento.')),
    });
  };

  return (
    <div className="space-y-6">
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

      <div className="grid gap-6 xl:grid-cols-[1.15fr_.85fr]">
        <SectionCard title="Minha carteira">
          <p className="mb-5 text-sm leading-7 text-slate-500">Cotações são atualizadas pelo BFF e mantidas em cache para proteger os provedores e acelerar o painel.</p>
          <div className="space-y-3">
            {(portfolio?.positions ?? []).length === 0 && <EmptyState />}
            {(portfolio?.positions ?? []).map((position) => (
              <article key={position.id} className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[.16em] text-emerald-600">{assetLabel(position.assetType)}</p>
                    <h4 className="mt-2 text-lg font-semibold text-slate-900">{position.symbol || position.name}</h4>
                    <p className="mt-1 text-sm text-slate-500">{position.name}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-slate-900">{currency(position.currentValue)}</p>
                    <p className={`mt-1 text-sm font-semibold ${position.returnAmount >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{signedCurrency(position.returnAmount)}</p>
                  </div>
                </div>
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 pt-3 text-xs text-slate-500">
                  <span>{position.quote.available ? `Cotação ${currency(position.quote.price ?? 0)} · DY 12m ${position.quote.dividendYield == null ? 'n/d' : `${position.quote.dividendYield.toFixed(2)}%`} · ${position.quote.source}` : 'Cotação indisponível · usando preço médio'}</span>
                  <div className="flex gap-3">
                    <button className="font-semibold text-emerald-700 hover:text-emerald-900" type="button" onClick={() => {
                      const raw = window.prompt(`Valor do ${position.assetType === 'RENDA_FIXA' ? 'rendimento' : 'dividendo'} recebido:`);
                      if (!raw) return;
                      const amount = Number(raw.replace(',', '.'));
                      if (Number.isFinite(amount) && amount > 0) incomeMutation.mutate({ id: position.id, amount, movementType: position.assetType === 'RENDA_FIXA' ? 'RENDIMENTO' : 'DIVIDENDO' });
                    }}>Registrar provento</button>
                    <button className="font-semibold text-rose-600 hover:text-rose-800" type="button" onClick={() => {
                      if (window.confirm(`Excluir ${position.name} da carteira?`)) deleteMutation.mutate(position.id);
                    }}>Excluir</button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="Adicionar posição">
          <form className="space-y-4" onSubmit={submit}>
            <Field label="Tipo"><select className={inputClass} value={form.assetType} onChange={(e) => {
              const assetType = e.target.value as InvestmentAssetType;
              setForm({ ...form, assetType, symbol: '', externalId: assetType === 'CRIPTO' ? '' : null, quantity: assetType === 'RENDA_FIXA' ? null : 1, averagePrice: assetType === 'RENDA_FIXA' ? null : 0, principal: assetType === 'RENDA_FIXA' ? 0 : null, annualRate: assetType === 'RENDA_FIXA' ? 12 : null, maturityDate: assetType === 'RENDA_FIXA' ? nextYear : null });
            }}><option value="ACAO">Ação</option><option value="FII">FII</option><option value="CRIPTO">Cripto</option><option value="RENDA_FIXA">Renda fixa</option></select></Field>
            <Field label="Nome"><input className={inputClass} required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder={fixed ? 'CDB Banco Exemplo' : 'Nome do ativo'} /></Field>
            {!fixed && <Field label={crypto ? 'ID CoinGecko' : 'Ticker'}><input className={inputClass} required value={(crypto ? form.externalId : form.symbol) ?? ''} onChange={(e) => setForm({ ...form, [crypto ? 'externalId' : 'symbol']: e.target.value })} placeholder={crypto ? 'bitcoin' : 'BBAS3'} /></Field>}
            {!fixed && <div className="grid gap-3 sm:grid-cols-2"><NumberField label="Quantidade" value={form.quantity ?? 0} onChange={(value) => setForm({ ...form, quantity: value })} /><NumberField label="Preço médio" value={form.averagePrice ?? 0} onChange={(value) => setForm({ ...form, averagePrice: value })} /></div>}
            {fixed && <div className="grid gap-3 sm:grid-cols-2"><NumberField label="Valor aplicado" value={form.principal ?? 0} onChange={(value) => setForm({ ...form, principal: value })} /><NumberField label="Taxa anual (%)" value={form.annualRate ?? 12} onChange={(value) => setForm({ ...form, annualRate: value })} /></div>}
            <div className="grid gap-3 sm:grid-cols-2"><DateField label="Data da compra" value={form.purchaseDate} onChange={(value) => setForm({ ...form, purchaseDate: value })} />{fixed && <DateField label="Vencimento" value={form.maturityDate ?? nextYear} onChange={(value) => setForm({ ...form, maturityDate: value })} />}</div>
            {feedback && <p className="text-sm text-emerald-700">{feedback}</p>}
            <button className="w-full rounded-full bg-slate-900 px-5 py-3 font-semibold text-white disabled:bg-slate-400" disabled={createMutation.isPending} type="submit">{createMutation.isPending ? 'Salvando...' : 'Adicionar à carteira'}</button>
          </form>
        </SectionCard>
      </div>

      <SectionCard title="Simulador de renda fixa">
        <div className="grid gap-4 lg:grid-cols-4"><NumberField label="Valor inicial" value={projection.principal} onChange={(principal) => setProjection({ ...projection, principal })} /><NumberField label="Taxa anual (%)" value={projection.annualRate} onChange={(annualRate) => setProjection({ ...projection, annualRate })} /><DateField label="Início" value={projection.startDate} onChange={(startDate) => setProjection({ ...projection, startDate })} /><DateField label="Vencimento" value={projection.maturityDate} onChange={(maturityDate) => setProjection({ ...projection, maturityDate })} /></div>
        <button className="mt-4 rounded-full bg-emerald-500 px-5 py-3 font-semibold text-white hover:bg-emerald-600" type="button" onClick={() => projectionMutation.mutate(projection)}>Projetar rendimento</button>
        {projectionMutation.data && <div className="mt-5 grid gap-4 rounded-[24px] bg-slate-950 p-5 text-white md:grid-cols-3"><ProjectionMetric label="Saldo projetado" value={currency(projectionMutation.data.projectedBalance)} /><ProjectionMetric label="Rendimento" value={currency(projectionMutation.data.projectedEarnings)} /><ProjectionMetric label="Prazo" value={`${projectionMutation.data.months} meses`} /><p className="md:col-span-3 text-xs leading-6 text-slate-400">{projectionMutation.data.disclaimer}</p></div>}
      </SectionCard>
    </div>
  );
}

const inputClass = 'h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none focus:border-emerald-400 focus:bg-white';
function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) { return <Field label={label}><input className={inputClass} min="0" step="0.01" type="number" value={value} onChange={(e) => onChange(Number(e.target.value))} /></Field>; }
function DateField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <Field label={label}><input className={inputClass} required type="date" value={value} onChange={(e) => onChange(e.target.value)} /></Field>; }
function ProjectionMetric({ label, value }: { label: string; value: string }) { return <div><p className="text-xs uppercase tracking-[.16em] text-emerald-300">{label}</p><p className="mt-2 text-2xl font-semibold">{value}</p></div>; }
function EmptyState() { return <div className="rounded-[22px] border border-dashed border-slate-200 p-8 text-center text-sm leading-7 text-slate-500">Sua carteira começa vazia. Cadastre o primeiro ativo ao lado para acompanhar patrimônio e projeções.</div>; }
function assetLabel(type: InvestmentAssetType) { return ({ ACAO: 'Ação', FII: 'Fundo imobiliário', CRIPTO: 'Criptoativo', RENDA_FIXA: 'Renda fixa' })[type]; }
function currency(value: number) { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value); }
function signedCurrency(value: number) { return `${value >= 0 ? '+' : '-'} ${currency(Math.abs(value))}`; }
