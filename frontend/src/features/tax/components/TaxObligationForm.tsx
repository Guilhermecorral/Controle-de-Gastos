import { FormEvent, useState } from 'react';
import type { TaxCategory, TaxObligation, TaxObligationInput, TaxRecurrence } from '../types';

const categories: TaxCategory[] = ['IRPF', 'IPVA', 'IPTU', 'ISS', 'INSS', 'LICENCIAMENTO', 'DARF', 'PERSONALIZADO'];
const recurrenceLabels: Record<TaxRecurrence, string> = { UNICA: 'Única', MENSAL: 'Mensal', ANUAL: 'Anual', PERSONALIZADA: 'Personalizada' };
const inputClass = 'mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 p-3 outline-none focus:border-emerald-500';

export default function TaxObligationForm({ obligation, pending, error, onClose, onSave }: {
  obligation: TaxObligation | null;
  pending: boolean;
  error: string;
  onClose: () => void;
  onSave: (input: TaxObligationInput) => void;
}) {
  const [name, setName] = useState(obligation?.name ?? '');
  const [authority, setAuthority] = useState(obligation?.issuingAuthority ?? '');
  const [category, setCategory] = useState<TaxCategory>(obligation?.category ?? 'IPVA');
  const [dueDate, setDueDate] = useState(obligation?.dueDate ?? '');
  const [amount, setAmount] = useState(obligation ? String(obligation.estimatedAmount) : '');
  const [recurrence, setRecurrence] = useState<TaxRecurrence>(obligation?.recurrence ?? 'UNICA');
  const [year, setYear] = useState(String(obligation?.competenceYear ?? new Date().getFullYear()));
  const [month, setMonth] = useState(String(obligation?.competenceMonth ?? new Date().getMonth() + 1));
  const [notes, setNotes] = useState(obligation?.notes ?? '');
  const [status, setStatus] = useState<TaxObligationInput['status']>(
    obligation?.status === 'ISENTA' || obligation?.status === 'EM_REVISAO' ? obligation.status : 'A_PAGAR',
  );
  const [stage, setStage] = useState<TaxObligationInput['documentStage']>(obligation?.documentStage ?? 'ESTIMATIVA');

  function submit(event: FormEvent) {
    event.preventDefault();
    onSave({ name: name.trim(), issuingAuthority: authority.trim(), category, dueDate,
      estimatedAmount: Number(amount), recurrence, competenceYear: Number(year),
      competenceMonth: recurrence === 'MENSAL' ? Number(month) : null, notes: notes.trim(),
      origin: 'MANUAL', status, documentStage: stage });
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
      <form aria-labelledby="tax-form-title" aria-modal="true" className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[28px] bg-white p-6 shadow-2xl" onSubmit={submit} role="dialog">
        <h2 className="text-xl font-semibold text-slate-900" id="tax-form-title">{obligation ? 'Editar obrigação' : 'Nova obrigação tributária'}</h2>
        <p className="mt-2 text-sm text-slate-600">Cadastrar uma estimativa ou guia não cria despesa nem altera seu saldo.</p>
        <div className="mt-5 grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-semibold text-slate-700">Nome<input className={inputClass} maxLength={160} onChange={(event) => setName(event.target.value)} required value={name} /></label>
          <label className="text-sm font-semibold text-slate-700">Órgão cobrador<input className={inputClass} maxLength={120} onChange={(event) => setAuthority(event.target.value)} value={authority} /></label>
          <label className="text-sm font-semibold text-slate-700">Categoria<select className={inputClass} onChange={(event) => setCategory(event.target.value as TaxCategory)} value={category}>{categories.map((item) => <option key={item} value={item}>{item === 'PERSONALIZADO' ? 'Personalizado' : item}</option>)}</select></label>
          <label className="text-sm font-semibold text-slate-700">Vencimento<input className={inputClass} onChange={(event) => setDueDate(event.target.value)} required type="date" value={dueDate} /></label>
          <label className="text-sm font-semibold text-slate-700">Valor estimado<input className={inputClass} min="0" onChange={(event) => setAmount(event.target.value)} required step="0.01" type="number" value={amount} /></label>
          <label className="text-sm font-semibold text-slate-700">Recorrência<select className={inputClass} onChange={(event) => setRecurrence(event.target.value as TaxRecurrence)} value={recurrence}>{Object.entries(recurrenceLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          <label className="text-sm font-semibold text-slate-700">Ano de competência<input className={inputClass} max="2200" min="1900" onChange={(event) => setYear(event.target.value)} required type="number" value={year} /></label>
          {recurrence === 'MENSAL' && <label className="text-sm font-semibold text-slate-700">Mês de competência<input className={inputClass} max="12" min="1" onChange={(event) => setMonth(event.target.value)} required type="number" value={month} /></label>}
          <label className="text-sm font-semibold text-slate-700">Situação<select className={inputClass} onChange={(event) => setStatus(event.target.value as TaxObligationInput['status'])} value={status}><option value="A_PAGAR">A pagar</option><option value="EM_REVISAO">Em revisão</option><option value="ISENTA">Isenta</option></select></label>
          <label className="text-sm font-semibold text-slate-700">Documento<select className={inputClass} onChange={(event) => setStage(event.target.value as TaxObligationInput['documentStage'])} value={stage}><option value="ESTIMATIVA">Estimativa</option><option value="GUIA_EMITIDA">Guia emitida</option></select></label>
        </div>
        <label className="mt-4 block text-sm font-semibold text-slate-700">Observações<textarea className={inputClass} maxLength={1000} onChange={(event) => setNotes(event.target.value)} rows={3} value={notes} /></label>
        {error && <p className="mt-4 text-sm text-rose-700" role="alert">{error}</p>}
        <div className="mt-6 flex justify-end gap-3"><button className="rounded-full border border-slate-200 px-5 py-2" disabled={pending} onClick={onClose} type="button">Cancelar</button><button className="rounded-full bg-slate-900 px-5 py-2 font-semibold text-white disabled:opacity-50" disabled={pending} type="submit">{pending ? 'Salvando...' : 'Salvar obrigação'}</button></div>
      </form>
    </div>
  );
}
