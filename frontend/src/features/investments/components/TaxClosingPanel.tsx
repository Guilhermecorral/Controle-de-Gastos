import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { SectionCard } from '../../shared/ui';

type Opening = { startDate: string; commonLoss: number; dayTradeLoss: number; fundLoss: number; commonCredit: number; dayTradeCredit: number; pendingTax: number; source: string };
type Bucket = { result: number; exemptProfit: number; lossUsed: number; remainingLoss: number; tax: number; creditUsed: number; remainingCredit: number };
type Payment = { id: number; period: string; revenueCode: string; amount: number; paidAt: string };
type Month = { period: string; sales: number; common: Bucket; funds: Bucket; estimatedDue: number | null; carriedTax: number; review: boolean; note: string; payment: Payment | null };
type Overview = { opening: Opening | null; months: Month[]; payments: Payment[] };
const money = (value: number) => value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const input = 'mt-1 h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3';
const button = 'rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50';
function useOverview() { return useQuery({ queryKey: ['investments', 'tax-closing'], queryFn: async () => (await api.get<Overview>('/investments/tax')).data }); }

export default function TaxClosingPanel({ compact = false }: { compact?: boolean }) {
  const query = useOverview();
  const [editing, setEditing] = useState(false);
  const [paying, setPaying] = useState<Month | 'manual' | null>(null);
  const pending = (query.data?.months ?? []).filter((m) => m.review || (!m.payment && (m.estimatedDue ?? 0) > 0));
  if (compact) return pending.length ? <SectionCard title="Tributação de investimentos"><p className="text-sm text-slate-600">{pending.length} competência(s) com imposto estimado ou dados para revisar. Consulte o fechamento mensal na aba Investimentos antes de registrar o pagamento.</p></SectionCard> : null;
  return <SectionCard title="Impostos sobre investimentos">
    <p className="mb-4 max-w-3xl text-sm leading-6 text-slate-500">Esta área só é necessária quando você vende ativos com possível imposto. Compras, aportes e proventos continuam registrados normalmente na carteira.</p>
    {query.isLoading && <p className="text-sm text-slate-500">Calculando competências...</p>}
    {query.isError && <div className="mb-4 rounded-2xl bg-amber-50 p-4 text-sm text-amber-900"><b>O resumo tributário não está disponível neste ambiente ainda.</b><p className="mt-1">Sua carteira e seus lançamentos continuam seguros. Este recurso depende do backend da v1.4.0-beta.1 e das migrações V11 e V12 publicados juntos.</p><details className="mt-2 text-xs"><summary className="cursor-pointer">Ver detalhe técnico</summary>{getApiErrorMessage(query.error, 'Sem detalhe retornado pela API.')}</details></div>}
    {!query.data?.opening && !query.isError ? <QuickStart onOpenHistory={() => setEditing(true)} /> : <div className="mb-4 flex flex-wrap gap-3"><button className={button} onClick={() => setEditing(!editing)}>{query.data?.opening ? 'Ajustar histórico anterior' : 'Já investia antes'}</button>{query.data?.opening && <button className="rounded-full border border-slate-200 px-4 py-2 text-sm" onClick={() => setPaying('manual')}>Registrar DARF já paga</button>}</div>}
    {editing && <OpeningForm initial={query.data?.opening} onClose={() => setEditing(false)} />}
    {!query.data?.opening && !query.isError && <p className="rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-800">Se começou agora, use o botão principal. O formulário de histórico só é necessário quando você já tinha vendas, prejuízos ou créditos de imposto antes de usar o Farol.</p>}
    <div className="max-h-[600px] space-y-3 overflow-y-auto">{query.data?.months.map((month) => <article key={month.period} className="rounded-2xl bg-slate-50 p-4"><div className="flex flex-wrap items-center justify-between gap-3"><strong>{month.period} · DARF 6015</strong><span className={month.review ? 'text-amber-700' : 'text-emerald-700'}>{month.review ? 'Revisão necessária' : month.payment ? `Pagamento registrado: ${money(month.payment.amount)}` : `Estimativa: ${money(month.estimatedDue ?? 0)}`}</span></div><p className="mt-2 text-xs text-slate-500">{month.note}</p>
      {!month.review && <details className="mt-3 text-sm"><summary className="cursor-pointer text-slate-700">Memória de cálculo</summary><div className="mt-3 grid gap-3 sm:grid-cols-2">{[['Operações comuns', month.common], ['FIIs', month.funds]].map(([label, value]) => { const bucket = value as Bucket; return <div key={label as string} className="rounded-xl bg-white p-3"><b>{label as string}</b><p>Resultado: {money(bucket.result)}</p><p>Lucro isento: {money(bucket.exemptProfit)}</p><p>Prejuízo compensado: {money(bucket.lossUsed)}</p><p>Prejuízo para próximos meses: {money(bucket.remainingLoss)}</p><p>IRRF utilizado: {money(bucket.creditUsed)}</p><p>Imposto após créditos: {money(bucket.tax)}</p></div>; })}</div><p className="mt-2">Imposto abaixo do mínimo acumulado: {money(month.carriedTax)}</p></details>}
      {!month.payment && <button className={`${button} mt-3`} onClick={() => setPaying(month)}>Registrar pagamento</button>}
    </article>)}</div>
    {(query.data?.payments.length ?? 0) > 0 && <details className="mt-4 text-sm"><summary>Pagamentos registrados</summary>{query.data?.payments.map((p) => <p key={p.id} className="mt-2">{p.period} · {p.revenueCode} · {money(p.amount)} · {p.paidAt}</p>)}</details>}
    {paying && <PaymentForm month={paying === 'manual' ? null : paying} onClose={() => setPaying(null)} />}
  </SectionCard>;
}

function QuickStart({ onOpenHistory }: { onOpenHistory: () => void }) {
  const client = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => api.put('/investments/tax/opening', {
      startDate: new Date().toISOString().slice(0, 7) + '-01',
      commonLoss: 0, dayTradeLoss: 0, fundLoss: 0, commonCredit: 0, dayTradeCredit: 0, pendingTax: 0,
      source: 'Comecei a registrar investimentos pelo Farol Financeiro',
    }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['investments'] }),
  });
  return <div className="mb-4 rounded-[24px] border border-emerald-100 bg-emerald-50/50 p-5"><h4 className="text-lg font-semibold text-slate-900">É sua primeira vez usando o Farol para investimentos?</h4><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Se todas as suas compras e vendas começaram aqui, não precisa preencher campos fiscais. Vamos iniciar o acompanhamento a partir deste mês com saldo zero.</p><div className="mt-4 flex flex-wrap gap-3"><button className={button} disabled={mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? 'Configurando...' : 'Comecei agora no Farol'}</button><button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700" onClick={onOpenHistory}>Já investia antes</button></div>{mutation.isError && <p className="mt-3 text-sm text-rose-700">{getApiErrorMessage(mutation.error, 'Não foi possível iniciar o acompanhamento.')}</p>}</div>;
}

function OpeningForm({ initial, onClose }: { initial?: Opening | null; onClose: () => void }) {
  const client = useQueryClient();
  const [form, setForm] = useState<Opening>(initial ?? { startDate: new Date().toLocaleDateString('en-CA').slice(0, 7) + '-01', commonLoss: 0, dayTradeLoss: 0, fundLoss: 0, commonCredit: 0, dayTradeCredit: 0, pendingTax: 0, source: '' });
  const mutation = useMutation({ mutationFn: () => api.put('/investments/tax/opening', form), onSuccess: () => { client.invalidateQueries({ queryKey: ['investments'] }); onClose(); } });
  const labels: [keyof Opening, string][] = [['commonLoss', 'Prejuízo anterior em ações'], ['fundLoss', 'Prejuízo anterior em FIIs'], ['commonCredit', 'Crédito de IRRF de ações neste ano'], ['dayTradeLoss', 'Prejuízo day trade (avançado)'], ['dayTradeCredit', 'Crédito day trade (avançado)'], ['pendingTax', 'DARF acumulada abaixo de R$ 10']];
  return <form className="mb-4 grid gap-3 rounded-2xl border border-slate-200 p-4 sm:grid-cols-2" onSubmit={(e: FormEvent) => { e.preventDefault(); mutation.mutate(); }}><div className="sm:col-span-2"><h4 className="font-semibold text-slate-900">Trazer histórico anterior</h4><p className="mt-1 text-sm leading-6 text-slate-500">Escolha o primeiro mês a partir do qual todas as suas compras e vendas já estão registradas no Farol. Use zero se não tiver prejuízos ou créditos anteriores.</p></div><label className="text-sm">Primeiro mês registrado aqui<input className={input} type="month" required value={form.startDate.slice(0, 7)} onChange={(e) => setForm({ ...form, startDate: e.target.value + '-01' })} /></label>{labels.map(([key, label]) => <label key={key} className="text-sm">{label} (R$)<input className={input} type="number" min="0" step="0.01" required value={form[key]} onChange={(e) => setForm({ ...form, [key]: Number(e.target.value) })} /></label>)}<label className="text-sm sm:col-span-2">Como você encontrou esses valores?<input className={input} required maxLength={255} placeholder="Ex.: informe da corretora ou minha apuração anterior" value={form.source} onChange={(e) => setForm({ ...form, source: e.target.value })} /></label><p className="text-xs text-slate-500 sm:col-span-2">Saldos anteriores são usados apenas para calcular vendas futuras. Eles não criam receitas ou despesas no seu financeiro.</p>{mutation.isError && <p className="text-sm text-rose-600 sm:col-span-2">{getApiErrorMessage(mutation.error, 'Não foi possível salvar.')}</p>}<button className={button} disabled={mutation.isPending}>Salvar e calcular</button></form>;
}

function PaymentForm({ month, onClose }: { month: Month | null; onClose: () => void }) {
  const client = useQueryClient();
  const [form, setForm] = useState({ period: month?.period ?? new Date().toLocaleDateString('en-CA').slice(0, 7), revenueCode: '6015', amount: month?.estimatedDue ?? 0, paidAt: new Date().toLocaleDateString('en-CA'), dueDate: '', accountLabel: '', note: '' });
  const mutation = useMutation({ mutationFn: () => api.post('/investments/tax/payments', form), onSuccess: () => { client.invalidateQueries(); onClose(); } });
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 p-4" role="dialog" aria-modal="true" aria-label="Registrar DARF paga"><form className="max-h-[90vh] w-full max-w-xl space-y-4 overflow-y-auto rounded-[28px] bg-white p-6" onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}><div className="flex justify-between"><h3 className="text-xl font-semibold">Registrar DARF paga</h3><button type="button" onClick={onClose}>Fechar</button></div><p className="text-sm text-slate-500">Use o valor efetivamente pago e o vencimento da guia Sicalc. A confirmação cria uma despesa de impostos.</p><div className="grid gap-3 sm:grid-cols-2"><label className="text-sm">Competência<input className={input} required type="month" value={form.period} onChange={(e) => setForm({ ...form, period: e.target.value })} /></label><label className="text-sm">Código<select className={input} value={form.revenueCode} onChange={(e) => setForm({ ...form, revenueCode: e.target.value })}><option value="6015">6015 · Renda variável</option><option value="4600">4600 · Ganho de capital</option></select></label><label className="text-sm">Valor pago<input className={input} required type="number" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })} /></label>{(['paidAt', 'dueDate'] as const).map((key) => <label key={key} className="text-sm">{key === 'paidAt' ? 'Data do pagamento' : 'Vencimento da guia'}<input className={input} required type="date" value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} /></label>)}<label className="text-sm">Conta de origem (identificação)<input className={input} required maxLength={255} value={form.accountLabel} onChange={(e) => setForm({ ...form, accountLabel: e.target.value })} /></label><label className="text-sm sm:col-span-2">Referência do comprovante<input className={input} required maxLength={255} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></label></div>{mutation.isError && <p className="text-sm text-rose-600">{getApiErrorMessage(mutation.error, 'Não foi possível registrar o pagamento.')}</p>}<button className={button} disabled={mutation.isPending}>Confirmar pagamento realizado</button></form></div>;
}
