import { Dispatch, SetStateAction } from 'react';
import { Category, PaymentMethod } from '../../../types';
import { categoryLabels, paymentMethodLabels } from '../../../lib/mockFinance';
import { Field, SelectField } from '../../shared/ui';
import { TransactionDraft } from '../types';

type TransactionModalProps = {
  isOpen: boolean;
  draft: TransactionDraft;
  suggestion: Category | null;
  onDraftChange: Dispatch<SetStateAction<TransactionDraft>>;
  onDescriptionChange: (value: string) => void;
  onCategoryTouched: (value: boolean) => void;
  onSubmit: () => void;
  onClose: () => void;
};

export default function TransactionModal({
  isOpen,
  draft,
  suggestion,
  onDraftChange,
  onDescriptionChange,
  onCategoryTouched,
  onSubmit,
  onClose,
}: TransactionModalProps) {
  if (!isOpen) {
    return null;
  }

  const isInstallmentPayment = draft.paymentMethod === 'CARTAO_CREDITO_PARCELADO';

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Lançamento rápido</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">
              {draft.type === 'RECEITA' ? 'Nova receita' : 'Nova despesa'}
            </h3>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              Modal pensado para o usuário registrar algo sem sair do contexto, com sugestão automática de categoria e chance de editar tudo.
            </p>
          </div>
          <button
            className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
            onClick={onClose}
            type="button"
          >
            Fechar
          </button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <SelectField
            label="Tipo"
            options={[
              { value: 'DESPESA', label: 'Despesa' },
              { value: 'RECEITA', label: 'Receita' },
            ]}
            value={draft.type}
            onChange={(value) => onDraftChange((currentValue) => ({ ...currentValue, type: value as 'RECEITA' | 'DESPESA' }))}
          />
          <Field label="Data">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              type="date"
              value={draft.transactionDate}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, transactionDate: event.target.value }))}
            />
          </Field>
          <Field label="Descrição">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="Ex.: consulta médica, lanche, aluguel"
              value={draft.description}
              onChange={(event) => onDescriptionChange(event.target.value)}
            />
          </Field>
          <Field label="Valor">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="0,00"
              type="number"
              value={draft.amount}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, amount: event.target.value }))}
            />
          </Field>
          <SelectField
            label="Categoria"
            options={Object.entries(categoryLabels).map(([value, label]) => ({ value, label }))}
            value={draft.category}
            onChange={(value) => {
              onCategoryTouched(true);
              onDraftChange((currentValue) => ({ ...currentValue, category: value as Category }));
            }}
          />
          <SelectField
            label="Pagamento"
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
          {isInstallmentPayment && (
            <Field label="Parcelas">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                min={2}
                type="number"
                value={draft.installments}
                onChange={(event) =>
                  onDraftChange((currentValue) => ({
                    ...currentValue,
                    installments: Math.max(2, Number(event.target.value) || 2),
                  }))
                }
              />
            </Field>
          )}
        </div>

        <Field label="Observação">
          <textarea
            className="min-h-[110px] w-full rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Notas rápidas para lembrar do contexto."
            value={draft.notes}
            onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, notes: event.target.value }))}
          />
        </Field>

        <div className="mt-5 flex flex-col gap-4 rounded-[24px] bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
          <div className="text-sm leading-7 text-slate-600">
            <p>
              Sugestão automática:{' '}
              <span className="font-semibold text-emerald-700">{suggestion ? categoryLabels[suggestion] : 'sem sugestão suficiente'}</span>
            </p>
            <p>Se a sugestão errar, o usuário continua livre para trocar manualmente.</p>
          </div>
          <button
            className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
            onClick={onSubmit}
            type="button"
          >
            Salvar lançamento
          </button>
        </div>
      </div>
    </div>
  );
}
