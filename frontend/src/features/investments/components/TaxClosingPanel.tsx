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
  return <SectionCard title="Fechamento mensal de investimentos">
    <p className="mb-4 text-sm leading-6 text-slate-500">A estimativa usa o histórico informado, incluindo todas as corretoras. Ações comuns e FIIs em reais são apurados aqui. Day trade, ativos no exterior e cripto precisam de revisão específica.</p>
    {query.isLoading && <p className="text-sm text-slate-500">Calculando competências...</p>}
    {query.isError && <p className="text-sm text-rose-600">Não foi possível carregar a apuração.</p>}
    <div className="mb-4 flex flex-wrap gap-3"><button className={button} onClick={() => setEditing(!editing)}>{query.data?.opening ? 'Revisar saldo tributário inicial' : 'Informar saldo tributário inicial'}</button><button className="rounded-full border border-slate-200 px-4 py-2 text-sm" onClick={() => setPaying('manual')}>Registrar DARF paga</button></div>
    {editing && <OpeningForm initial={query.data?.opening} onClose={() => setEditing(false)} />}
    {!query.data?.opening && <p className="rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-800">Informe o mês em que seu histórico está completo e os prejuízos anteriores. Se começou agora, preencha os saldos com zero.</p>}
    <div className="max-h-[600px] space-y-3 overflow-y-auto">{query.data?.months.map((month) => <article key={month.period} className="rounded-2xl bg-slate-50 p-4"><div className="flex flex-wrap items-center justify-between gap-3"><strong>{month.period} · DARF 6015</strong><span className={month.review ? 'text-amber-700' : 'text-emerald-700'}>{month.review ? 'Revisão necessária' : month.payment ? `Pagamento registrado: ${money(month.payment.amount)}` : `Estimativa: ${money(month.estimatedDue ?? 0)}`}</span></div><p className="mt-2 text-xs text-slate-500">{month.note}</p>
      {!month.review && <details className="mt-3 text-sm"><summary className="cursor-pointer text-slate-700">Memória de cálculo</summary><div className="mt-3 grid gap-3 sm:grid-cols-2">{[['Operações comuns', month.common], ['FIIs', month.funds]].map(([label, value]) => { const bucket = value as Bucket; return <div key={label as string} className="rounded-xl bg-white p-3"><b>{label as string}</b><p>Resultado: {money(bucket.result)}</p><p>Lucro isento: {money(bucket.exemptProfit)}</p><p>Prejuízo compensado: {money(bucket.lossUsed)}</p><p>Prejuízo para próximos meses: {money(bucket.remainingLoss)}</p><p>IRRF utilizado: {money(bucket.creditUsed)}</p><p>Imposto após créditos: {money(bucket.tax)}</p></div>; })}</div><p className="mt-2">Imposto abaixo do mínimo acumulado: {money(month.carriedTax)}</p></details>}
      {!month.payment && <button className={`${button} mt-3`} onClick={() => setPaying(month)}>Registrar pagamento</button>}
    </article>)}</div>
    {(query.data?.payments.length ?? 0) > 0 && <details className="mt-4 text-sm"><summary>Pagamentos registrados</summary>{query.data?.payments.map((p) => <p key={p.id} className="mt-2">{p.period} · {p.revenueCode} · {money(p.amount)} · {p.paidAt}</p>)}</details>}
    {paying && <PaymentForm month={paying === 'manual' ? null : paying} onClose={() => setPaying(null)} />}
  </SectionCard>;
}

function OpeningForm({ initial, onClose }: { initial?: Opening | null; onClose: () => void }) {
  const client = useQueryClient();
  const [form, setForm] = useState<Opening>(initial ?? { startDate: new Date().toLocaleDateString('en-CA').slice(0, 7) + '-01', commonLoss: 0, dayTradeLoss: 0, fundLoss: 0, commonCredit: 0, dayTradeCredit: 0, pendingTax: 0, source: '' });
  const mutation = useMutation({ mutationFn: () => api.put('/investments/tax/opening', form), onSuccess: () => { client.invalidateQueries({ queryKey: ['investments'] }); onClose(); } });
  const labels: [keyof Opening, string][] = [['commonLoss', 'Prejuízo comum'], ['dayTradeLoss', 'Prejuízo day trade (guardado para revisão)'], ['fundLoss', 'Prejuízo FII'], ['commonCredit', 'Crédito IRRF comum do ano'], ['dayTradeCredit', 'Crédito day trade (guardado para revisão)'], ['pendingTax', 'DARF acumulada abaixo de R$ 10']];
  return <form className="mb-4 grid gap-3 rounded-2xl border border-slate-200 p-4 sm:grid-cols-2" onSubmit={(e: FormEvent) => { e.preventDefault(); mutation.mutate(); }}><label className="text-sm">Primeiro mês com histórico completo<input className={input} type="month" required value={form.startDate.slice(0, 7)} onChange={(e) => setForm({ ...form, startDate: e.target.value + '-01' })} /></label>{labels.map(([key, label]) => <label key={key} className="text-sm">{label} (R$)<input className={input} type="number" min="0" step="0.01" required value={form[key]} onChange={(e) => setForm({ ...form, [key]: Number(e.target.value) })} /></label>)}<label className="text-sm">Origem dos valores<input className={input} required maxLength={255} placeholder="Apuração anterior, informe ou início sem saldo" value={form.source} onChange={(e) => setForm({ ...form, source: e.target.value })} /></label><p className="text-xs text-slate-500 sm:col-span-2">Informe saldos anteriores ao primeiro dia escolhido. Alterações recalculam as competências posteriores. Créditos de IRRF comum não são transferidos para o próximo ano.</p>{mutation.isError && <p className="text-sm text-rose-600 sm:col-span-2">{getApiErrorMessage(mutation.error, 'Não foi possível salvar.')}</p>}<button className={button} disabled={mutation.isPending}>Salvar saldos iniciais</button></form>;
}

function PaymentForm({ month, onClose }: { month: Month | null; onClose: () => void }) {
  const client = useQueryClient();
  const [form, setForm] = useState({ period: month?.period ?? new Date().toLocaleDateString('en-CA').slice(0, 7), revenueCode: '6015', amount: month?.estimatedDue ?? 0, paidAt: new Date().toLocaleDateString('en-CA'), dueDate: '', accountLabel: '', note: '' });
  const mutation = useMutation({ mutationFn: () => api.post('/investments/tax/payments', form), onSuccess: () => { client.invalidateQueries(); onClose(); } });
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 p-4" role="dialog" aria-modal="true" aria-label="Registrar DARF paga"><form className="max-h-[90vh] w-full max-w-xl space-y-4 overflow-y-auto rounded-[28px] bg-white p-6" onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}><div className="flex justify-between"><h3 className="text-xl font-semibold">Registrar DARF paga</h3><button type="button" onClick={onClose}>Fechar</button></div><p className="text-sm text-slate-500">Use o valor efetivamente pago e o vencimento da guia Sicalc. A confirmação cria uma despesa de impostos.</p><div className="grid gap-3 sm:grid-cols-2"><label className="text-sm">Competência<input className={input} required type="month" value={form.period} onChange={(e) => setForm({ ...form, period: e.target.value })} /></label><label className="text-sm">Código<select className={input} value={form.revenueCode} onChange={(e) => setForm({ ...form, revenueCode: e.target.value })}><option value="6015">6015 · Renda variável</option><option value="4600">4600 · Ganho de capital</option></select></label><label className="text-sm">Valor pago<input className={input} required type="number" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })} /></label>{(['paidAt', 'dueDate'] as const).map((key) => <label key={key} className="text-sm">{key === 'paidAt' ? 'Data do pagamento' : 'Vencimento da guia'}<input className={input} required type="date" value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} /></label>)}<label className="text-sm">Conta de origem (identificação)<input className={input} required maxLength={255} value={form.accountLabel} onChange={(e) => setForm({ ...form, accountLabel: e.target.value })} /></label><label className="text-sm sm:col-span-2">Referência do comprovante<input className={input} required maxLength={255} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></label></div>{mutation.isError && <p className="text-sm text-rose-600">{getApiErrorMessage(mutation.error, 'Não foi possível registrar o pagamento.')}</p>}<button className={button} disabled={mutation.isPending}>Confirmar pagamento realizado</button></form></div>;
}
