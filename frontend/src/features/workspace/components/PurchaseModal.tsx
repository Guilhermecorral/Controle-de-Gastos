import { Dispatch, SetStateAction } from 'react';
import { PaymentMethod, WishlistHistoryResponse, WishlistItemResponse } from '../../../types';
import { categoryLabels, formatCurrency, paymentMethodLabels } from '../../../lib/mockFinance';
import { EmptyState, Field, InfoStrip, SelectField, Tag } from '../../shared/ui';
import { historyLabel, historyTone, PurchaseDraft } from '../types';

type PurchaseModalProps = {
  isOpen: boolean;
  item: WishlistItemResponse | null;
  draft: PurchaseDraft;
  receiptFile: File | null;
  history: WishlistHistoryResponse[];
  onDraftChange: Dispatch<SetStateAction<PurchaseDraft>>;
  onReceiptFileChange: (file: File | null) => void;
  onSubmit: () => void;
  onClose: () => void;
};

export default function PurchaseModal({
  isOpen,
  item,
  draft,
  receiptFile,
  history,
  onDraftChange,
  onReceiptFileChange,
  onSubmit,
  onClose,
}: PurchaseModalProps) {
  if (!isOpen || !item) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto bg-slate-950/45 p-4">
      <div className="mx-auto flex min-h-full items-center justify-center">
        <div className="grid max-h-[calc(100vh-2rem)] w-full max-w-4xl gap-5 overflow-y-auto rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)] xl:grid-cols-[1.1fr_0.9fr]">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Compra da lista</p>
          <h3 className="mt-2 text-2xl font-semibold text-slate-900">{item.description}</h3>
          <p className="mt-2 text-sm leading-7 text-slate-600">
            Ao confirmar, o item sai dos desejados, vai para comprados e gera lançamentos financeiros automáticos.
            Se houver comprovante, ele já pode nascer junto da compra.
          </p>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <InfoStrip helper="Preço final com desconto aplicado." label="Valor final" value={formatCurrency(item.finalPrice)} />
            <InfoStrip helper="Categoria que entrará no financeiro." label="Categoria" value={categoryLabels[item.category]} />
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <Field label="Data da compra">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                type="date"
                value={draft.purchaseDate}
                onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, purchaseDate: event.target.value }))}
              />
            </Field>
            <SelectField
              label="Forma de pagamento"
              options={Object.entries(paymentMethodLabels).map(([value, label]) => ({ value, label }))}
              value={draft.paymentMethod}
              onChange={(value) =>
                onDraftChange((currentValue) => ({
                  ...currentValue,
                  paymentMethod: value as PaymentMethod,
                  installments: value === 'CARTAO_CREDITO_PARCELADO' ? Math.max(currentValue.installments, 2) : 1,
                }))
              }
            />
            <Field label="Parcelas">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                max={24}
                min={1}
                type="number"
                value={draft.installments}
                onChange={(event) =>
                  onDraftChange((currentValue) => ({
                    ...currentValue,
                    installments: Number(event.target.value),
                  }))
                }
              />
            </Field>
            <label className="flex items-center gap-3 rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              <input
                checked={draft.firstInstallmentNextMonth}
                className="h-5 w-5 rounded border-slate-300 text-emerald-500 focus:ring-emerald-400"
                type="checkbox"
                onChange={(event) =>
                  onDraftChange((currentValue) => ({
                    ...currentValue,
                    firstInstallmentNextMonth: event.target.checked,
                  }))
                }
              />
              Primeira parcela cai no mês seguinte
            </label>
          </div>

          <label className="mt-6 flex min-h-[140px] cursor-pointer flex-col items-center justify-center rounded-[24px] border border-dashed border-emerald-200 bg-emerald-50/50 p-5 text-center">
            <span className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-700">Nota fiscal da compra</span>
            <span className="mt-3 text-sm leading-7 text-slate-600">
              Se a compra for parcelada, anexar uma vez aqui já cobre todas as parcelas geradas dessa compra.
            </span>
            <span className="mt-5 rounded-full border border-emerald-200 bg-white px-4 py-2 text-sm font-semibold text-emerald-700">
              {receiptFile ? receiptFile.name : 'Escolher arquivo'}
            </span>
            <input
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              className="hidden"
              type="file"
              onChange={(event) => onReceiptFileChange(event.target.files?.[0] ?? null)}
            />
          </label>

          <div className="mt-6 flex flex-wrap gap-3">
            <button
              className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
              onClick={onSubmit}
              type="button"
            >
              Confirmar compra
            </button>
            <button
              className="rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
              onClick={onClose}
              type="button"
            >
              Cancelar
            </button>
          </div>
        </div>

        <div className="rounded-[28px] bg-slate-50 p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Histórico recente</p>
          <div className="mt-4 grid gap-3">
            {history.length === 0 && <EmptyState label="Esse item ainda não tem histórico relevante." />}
            {history.map((entry) => (
              <div key={entry.id} className="rounded-[20px] border border-white bg-white p-4">
                <div className="flex items-center justify-between gap-3">
                  <Tag tone={historyTone(entry.actionType)}>{historyLabel(entry.actionType)}</Tag>
                  <span className="text-xs text-slate-500">
                    {new Date(entry.createdAt).toLocaleDateString('pt-BR', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                    })}
                  </span>
                </div>
                <p className="mt-3 text-sm leading-7 text-slate-600">{entry.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
    </div>
  );
}
