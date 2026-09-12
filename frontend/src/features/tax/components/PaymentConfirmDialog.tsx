import { FormEvent, useState } from 'react';
import { formatCurrency } from '../../../lib/mockFinance';
import type { PaymentConfirmationInput, TaxObligation } from '../types';

export default function PaymentConfirmDialog({ obligation, pending, error, onClose, onConfirm }: {
  obligation: TaxObligation;
  pending: boolean;
  error: string;
  onClose: () => void;
  onConfirm: (input: PaymentConfirmationInput) => void;
}) {
  const [amount, setAmount] = useState(String(obligation.estimatedAmount));
  const [date, setDate] = useState(new Date().toLocaleDateString('en-CA'));
  const [account, setAccount] = useState('');
  const [receipt, setReceipt] = useState('');
  function submit(event: FormEvent) {
    event.preventDefault();
    if (Number(amount) > 0 && account.trim() && date) onConfirm({ paidAmount: Number(amount), paidDate: date, accountDescription: account.trim(), receiptReference: receipt.trim() });
  }
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
      <form aria-labelledby="tax-payment-title" aria-modal="true" className="w-full max-w-xl space-y-4 rounded-[28px] bg-white p-6 shadow-2xl" onSubmit={submit} role="dialog">
        <h2 className="text-xl font-semibold text-slate-900" id="tax-payment-title">Confirmar pagamento de {obligation.name}</h2>
        <p className="text-sm text-slate-600">Estimativa: {formatCurrency(obligation.estimatedAmount)}. Você pode informar o valor efetivamente pago.</p>
        <label className="block text-sm font-semibold text-slate-700">Valor pago<input className="mt-1 w-full rounded-xl border border-slate-200 p-3" min="0.01" onChange={(event) => setAmount(event.target.value)} required step="0.01" type="number" value={amount} /></label>
        <label className="block text-sm font-semibold text-slate-700">Data do pagamento<input className="mt-1 w-full rounded-xl border border-slate-200 p-3" max={new Date().toLocaleDateString('en-CA')} onChange={(event) => setDate(event.target.value)} required type="date" value={date} /></label>
        <label className="block text-sm font-semibold text-slate-700">Conta de origem<input className="mt-1 w-full rounded-xl border border-slate-200 p-3" maxLength={255} onChange={(event) => setAccount(event.target.value)} placeholder="Ex.: conta corrente principal" required value={account} /></label>
        <label className="block text-sm font-semibold text-slate-700">Referência do comprovante (opcional)<input className="mt-1 w-full rounded-xl border border-slate-200 p-3" maxLength={255} onChange={(event) => setReceipt(event.target.value)} value={receipt} /></label>
        <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Uma despesa vinculada será criada no Histórico Financeiro somente após esta confirmação.</p>
        {error && <p className="text-sm text-rose-700" role="alert">{error}</p>}
        <div className="flex justify-end gap-3"><button className="rounded-full border border-slate-200 px-5 py-2" disabled={pending} onClick={onClose} type="button">Cancelar</button><button className="rounded-full bg-emerald-700 px-5 py-2 font-semibold text-white disabled:opacity-50" disabled={pending} type="submit">{pending ? 'Confirmando...' : 'Confirmar pagamento'}</button></div>
      </form>
    </div>
  );
}
