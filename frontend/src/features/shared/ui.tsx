import { ReactNode } from 'react';
import { MonthlyAnalysisResponse, TransactionResponse } from '../../types';
import { categoryLabels, formatCurrency, formatIsoDate } from '../../lib/mockFinance';
import { ToastMessage } from '../workspace/types';

export function SectionCard({ children, title }: { children: ReactNode; title?: string }) {
  return (
    <section className="rounded-[28px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
      {title && <h3 className="text-2xl font-semibold text-slate-900">{title}</h3>}
      {title && <div className="mt-5" />}
      {children}
    </section>
  );
}

export function LoadingCard({ label }: { label: string }) {
  return (
    <SectionCard>
      <div className="flex min-h-[220px] items-center justify-center rounded-[24px] bg-slate-50 px-6 text-center text-sm font-semibold text-slate-500">
        {label}
      </div>
    </SectionCard>
  );
}

export function UnavailableCard({ label }: { label: string }) {
  return (
    <SectionCard>
      <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 rounded-[24px] bg-slate-50 px-6 text-center">
        <p className="text-base font-semibold text-slate-900">Não conseguimos carregar este bloco.</p>
        <p className="max-w-md text-sm leading-7 text-slate-500">{label}</p>
      </div>
    </SectionCard>
  );
}

export function MetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: 'positive' | 'negative' | 'neutral' | 'warning';
}) {
  const tones = {
    positive: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    negative: 'border-rose-100 bg-rose-50 text-rose-700',
    neutral: 'border-slate-100 bg-slate-50 text-slate-900',
    warning: 'border-amber-100 bg-amber-50 text-amber-700',
  };

  return (
    <article className={`rounded-[24px] border p-5 ${tones[tone]}`}>
      <p className="text-sm font-semibold uppercase tracking-[0.16em]">{label}</p>
      <p className="mt-4 text-3xl font-semibold">{value}</p>
    </article>
  );
}

export function LandingCard({ title, body }: { title: string; body: string }) {
  return (
    <article className="rounded-[28px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
      <h4 className="text-xl font-semibold text-slate-900">{title}</h4>
      <p className="mt-4 text-sm leading-7 text-slate-600">{body}</p>
    </article>
  );
}

export function FeatureChip({
  label,
  helper,
  dark = false,
}: {
  label: string;
  helper: string;
  dark?: boolean;
}) {
  return (
    <div className={`rounded-[22px] border px-4 py-4 ${dark ? 'border-white/10 bg-white/5 text-white' : 'border-emerald-100 bg-white text-slate-900'}`}>
      <p className={dark ? 'font-semibold text-white' : 'font-semibold text-slate-900'}>{label}</p>
      <p className={dark ? 'mt-2 text-sm text-slate-300' : 'mt-2 text-sm text-slate-600'}>{helper}</p>
    </div>
  );
}

export function LandingStat({
  title,
  value,
  tone = 'positive',
}: {
  title: string;
  value: string;
  tone?: 'positive' | 'negative';
}) {
  return (
    <div className="rounded-[22px] border border-white/10 bg-white/5 p-4">
      <p className="text-sm text-slate-300">{title}</p>
      <p className={`mt-3 text-2xl font-semibold ${tone === 'positive' ? 'text-emerald-300' : 'text-rose-300'}`}>{value}</p>
    </div>
  );
}

export function InfoStrip({
  label,
  helper,
  value,
}: {
  label: string;
  helper: string;
  value: string;
}) {
  return (
    <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">{label}</p>
      <p className="mt-2 text-xl font-semibold text-slate-900">{value}</p>
      <p className="mt-2 text-sm leading-7 text-slate-500">{helper}</p>
    </div>
  );
}

export function ProgressBar({
  label,
  value,
  total,
  tone,
}: {
  label: string;
  value: number;
  total: number;
  tone: 'positive' | 'negative';
}) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-sm">
        <span className="font-semibold text-slate-700">{label}</span>
        <span className={tone === 'positive' ? 'text-emerald-600' : 'text-rose-600'}>{formatCurrency(value)}</span>
      </div>
      <div className="h-3 rounded-full bg-white">
        <div
          className={`h-3 rounded-full ${tone === 'positive' ? 'bg-emerald-500' : 'bg-rose-500'}`}
          style={{ width: `${Math.max((value / total) * 100, 8)}%` }}
        />
      </div>
    </div>
  );
}

export function ComparisonCard({
  title,
  subtitle,
  summary,
}: {
  title: string;
  subtitle: string;
  summary: MonthlyAnalysisResponse['comparativoMesAnterior'];
}) {
  return (
    <SectionCard title={title}>
      <p className="text-sm text-slate-500">{subtitle}</p>
      <div className="mt-5 grid gap-3">
        <TrendRow label="Receitas" tone={summary.tendenciaReceitas} value={summary.diferencaReceitas} />
        <TrendRow label="Despesas" tone={summary.tendenciaDespesas} value={summary.diferencaDespesas} inverse />
        <TrendRow label="Saldo" tone={summary.tendenciaSaldo} value={summary.diferencaSaldo} />
      </div>
      <div className="mt-5 rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
        Tendência geral do período: <span className="font-semibold text-slate-900">{summary.tendenciaGeral.toLowerCase()}</span>.
      </div>
    </SectionCard>
  );
}

export function TrendRow({
  label,
  value,
  tone,
  inverse = false,
}: {
  label: string;
  value: number;
  tone: 'MELHOR' | 'PIOR' | 'IGUAL';
  inverse?: boolean;
}) {
  const color = tone === 'MELHOR' ? 'text-emerald-600' : tone === 'PIOR' ? 'text-rose-600' : 'text-slate-500';
  const trendLabel = inverse && tone === 'MELHOR' ? 'Menos gasto' : tone;

  return (
    <div className="flex items-center justify-between rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4">
      <div>
        <p className="font-semibold text-slate-900">{label}</p>
        <p className="mt-1 text-sm text-slate-500">Comparação automática com a referência</p>
      </div>
      <div className="text-right">
        <p className={`font-semibold ${color}`}>{value >= 0 ? '+' : ''}{formatCurrency(value)}</p>
        <p className="mt-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{trendLabel}</p>
      </div>
    </div>
  );
}

export function ExpenseHighlight({
  transaction,
}: {
  transaction: Pick<TransactionResponse, 'description' | 'amount' | 'category' | 'transactionDate'>;
}) {
  return (
    <div className="rounded-[22px] bg-slate-50 p-5">
      <p className="text-lg font-semibold text-slate-900">{transaction.description}</p>
      <p className="mt-2 text-sm text-slate-500">{categoryLabels[transaction.category]}</p>
      <p className="mt-4 text-2xl font-semibold text-rose-600">{formatCurrency(transaction.amount)}</p>
      <p className="mt-3 text-sm text-slate-500">
        {formatIsoDate(transaction.transactionDate)}
      </p>
    </div>
  );
}

export function Tag({
  children,
  tone,
}: {
  children: ReactNode;
  tone: 'positive' | 'negative' | 'warning' | 'neutral';
}) {
  const styles = {
    positive: 'bg-emerald-100 text-emerald-700',
    negative: 'bg-rose-100 text-rose-700',
    warning: 'bg-amber-100 text-amber-700',
    neutral: 'bg-slate-100 text-slate-700',
  };

  return <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] ${styles[tone]}`}>{children}</span>;
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-slate-700">{label}</span>
      {children}
    </label>
  );
}

export function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<{ value: string; label: string }>;
  onChange: (value: string) => void;
}) {
  return (
    <Field label={label}>
      <select
        className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </Field>
  );
}

export function EmptyState({ label }: { label: string }) {
  return (
    <div className="rounded-[24px] border border-dashed border-slate-200 bg-white p-6 text-center text-sm leading-7 text-slate-500">
      {label}
    </div>
  );
}

export function ToastStack({ toasts }: { toasts: ToastMessage[] }) {
  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="fixed bottom-24 right-4 z-50 grid w-[min(380px,calc(100vw-2rem))] gap-3">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`rounded-[22px] border px-4 py-4 shadow-[0_20px_60px_rgba(15,23,42,0.14)] ${
            toast.tone === 'success'
              ? 'border-emerald-100 bg-white text-slate-900'
              : 'border-sky-100 bg-white text-slate-900'
          }`}
        >
          <p className={`text-xs font-semibold uppercase tracking-[0.16em] ${toast.tone === 'success' ? 'text-emerald-600' : 'text-sky-600'}`}>
            {toast.tone === 'success' ? 'Tudo certo' : 'Aviso'}
          </p>
          <p className="mt-2 text-sm leading-7 text-slate-600">{toast.message}</p>
        </div>
      ))}
    </div>
  );
}
