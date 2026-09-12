import { formatCurrency, formatIsoDate } from '../../../lib/mockFinance';
import type { TaxObligation } from '../types';

const tones: Record<TaxObligation['status'], string> = {
  A_PAGAR: 'bg-amber-50 text-amber-800',
  PAGA: 'bg-emerald-50 text-emerald-800',
  ATRASADA: 'bg-rose-50 text-rose-800',
  ISENTA: 'bg-slate-100 text-slate-600',
  EM_REVISAO: 'bg-orange-50 text-orange-800',
};

export default function TaxObligationCard({ obligation, onEdit, onPay, onDelete }: {
  obligation: TaxObligation;
  onEdit: () => void;
  onPay: () => void;
  onDelete: () => void;
}) {
  const editable = obligation.origin === 'MANUAL' && !obligation.paidDate;
  const payable = editable && obligation.status !== 'ISENTA';
  return (
    <article className="glass-panel rounded-[24px] border border-slate-100 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-emerald-700">{obligation.category} · {obligation.origin === 'INVESTIMENTO' ? 'Investimentos' : 'Pessoal'}</p>
          <h3 className="mt-2 text-xl font-semibold text-slate-900">{obligation.name}</h3>
          <p className="mt-1 text-sm text-slate-500">{obligation.issuingAuthority || 'Órgão não informado'} · Vence em {formatIsoDate(obligation.dueDate)}</p>
        </div>
        <span className={`rounded-full px-3 py-1.5 text-xs font-bold ${tones[obligation.status]}`}>{obligation.statusDisplay}</span>
      </div>
      <div className="mt-5 flex flex-wrap items-end justify-between gap-3 border-t border-slate-100 pt-4">
        <div>
          <p className="text-xs text-slate-500">{obligation.paidDate ? 'Valor pago' : 'Valor estimado'}</p>
          <p className="text-2xl font-semibold text-slate-900">{formatCurrency(obligation.paidAmount ?? obligation.estimatedAmount)}</p>
          <p className="text-xs text-slate-500">{obligation.paidDate ? `Pago em ${formatIsoDate(obligation.paidDate)}` : obligation.documentStage === 'GUIA_EMITIDA' ? 'Guia emitida' : 'Estimativa, sem efeito no saldo'}</p>
        </div>
        {editable && <div className="flex flex-wrap gap-2">
          <button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50" onClick={onEdit} type="button">Editar</button>
          {payable && <button className="rounded-full bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-800" onClick={onPay} type="button">Confirmar pagamento</button>}
          <button className="rounded-full border border-rose-200 px-4 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50" onClick={onDelete} type="button">Excluir</button>
        </div>}
      </div>
    </article>
  );
}
