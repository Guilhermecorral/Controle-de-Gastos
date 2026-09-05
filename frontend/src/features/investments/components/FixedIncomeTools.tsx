import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { InvestmentPositionResponse } from '../../../types';

type TaxFields = { taxRegime?: 'REGRESSIVO' | 'ISENTO' | 'MANUAL' | null; manualTaxRate?: number; iofApplicable?: boolean };
const input = 'h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4';
const money = (value: number) => value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export function TaxRegimeFields({ value, onChange }: { value: TaxFields; onChange: (value: TaxFields) => void }) {
  return <div className="mb-4 grid gap-3 rounded-2xl border border-slate-100 p-4 sm:grid-cols-2">
    <label className="text-sm text-slate-600">Tributação<select className={input} value={value.taxRegime ?? ''} required onChange={(e) => onChange({ taxRegime: e.target.value as TaxFields['taxRegime'] })}><option value="" disabled>Selecione o regime</option><option value="REGRESSIVO">IR regressivo</option><option value="ISENTO">Isento de IR</option><option value="MANUAL">Alíquota de IR informada</option></select></label>
    {value.taxRegime === 'MANUAL' && <label className="text-sm text-slate-600">IR sobre rendimento (%)<input className={input} type="number" min="0" max="100" step="0.01" required value={value.manualTaxRate ?? 0} onChange={(e) => onChange({ manualTaxRate: Number(e.target.value) })} /></label>}
    <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={value.iofApplicable ?? true} onChange={(e) => onChange({ iofApplicable: e.target.checked })} /> Aplicar IOF em resgate antes de 30 dias</label>
    <p className="text-xs text-slate-500 sm:col-span-2">Confira o regime do produto no comprovante. A isenção de IR não determina automaticamente a regra de IOF.</p>
  </div>;
}

type Result = { gross: number; earnings: number; incomeTax: number; iof: number; net: number; days: number; rate: number };
export function FixedIncomeRedemption({ position, onClose }: { position: InvestmentPositionResponse | null; onClose: () => void }) {
  return position ? <RedemptionForm key={position.id} position={position} onClose={onClose} /> : null;
}
function RedemptionForm({ position, onClose }: { position: InvestmentPositionResponse; onClose: () => void }) {
  const client = useQueryClient();
  const [form, setForm] = useState({ eventDate: new Date().toLocaleDateString('en-CA'), grossAmount: position.currentValue,
    taxRegime: position.taxRegime ?? 'REGRESSIVO', manualTaxRate: position.manualTaxRate ?? 0, iofApplicable: position.iofApplicable ?? true });
  const [preview, setPreview] = useState<Result | null>(null);
  const mutation = useMutation({ mutationFn: async (confirm: boolean) => (await api.post<Result>(`/investments/positions/${position.id}/${confirm ? 'redeem' : 'redemption-preview'}`, form)).data,
    onSuccess: (result, confirm) => { if (confirm) { client.invalidateQueries(); onClose(); } else setPreview(result); } });
  const change = (fields: Partial<typeof form>) => { setForm({ ...form, ...fields }); setPreview(null); };
  return <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/55 p-4" role="dialog" aria-modal="true" aria-label="Resgate de renda fixa"><form className="max-h-[90vh] w-full max-w-xl space-y-4 overflow-y-auto rounded-[28px] bg-white p-6" onSubmit={(e) => { e.preventDefault(); mutation.mutate(false); }}>
    <div className="flex items-start justify-between gap-4"><h3 className="text-xl font-semibold">Resgate total · {position.name}</h3><button type="button" onClick={onClose}>Fechar</button></div>
    <p className="text-sm text-slate-500">Informe o valor bruto do resgate. A confirmação encerra a aplicação e registra o valor líquido recebido no financeiro.</p>
    <label className="block text-sm">Data do resgate<input className={input} type="date" required value={form.eventDate} onChange={(e) => change({ eventDate: e.target.value })} /></label>
    <label className="block text-sm">Valor bruto (R$)<input className={input} type="number" min="0.01" step="0.01" required value={form.grossAmount} onChange={(e) => change({ grossAmount: Number(e.target.value) })} /></label>
    <TaxRegimeFields value={form} onChange={(fields) => change({ ...fields, taxRegime: fields.taxRegime ?? form.taxRegime })} />
    {preview && <div className="grid grid-cols-2 gap-3 rounded-2xl bg-emerald-50 p-4 text-sm"><span>Rendimento: {money(preview.earnings)}</span><span>Prazo: {preview.days} dias</span><span>IR ({preview.rate}%): {money(preview.incomeTax)}</span><span>IOF: {money(preview.iof)}</span><strong className="col-span-2 text-emerald-800">Líquido estimado: {money(preview.net)}</strong></div>}
    {mutation.isError && <p className="text-sm text-rose-600">{getApiErrorMessage(mutation.error, 'Não foi possível calcular o resgate.')}</p>}
    <button className="rounded-full border border-emerald-200 px-5 py-3 text-sm font-semibold text-emerald-700" disabled={mutation.isPending}>Calcular resgate</button>
    {preview && <button type="button" className="ml-2 rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" disabled={mutation.isPending} onClick={() => mutation.mutate(true)}>Confirmar recebimento</button>}
  </form></div>;
}
