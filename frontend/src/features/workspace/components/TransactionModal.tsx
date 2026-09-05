import { Dispatch, SetStateAction, useEffect, useState } from 'react';
import { Category, PaymentMethod } from '../../../types';
import { categoryLabels, paymentMethodLabels } from '../../../lib/mockFinance';
import { Field, SelectField } from '../../shared/ui';
import { TransactionDraft } from '../types';
import OFXUploader from '../../ofx-upload/components/OFXUploader';

type TransactionModalProps = {
  isOpen: boolean;
  draft: TransactionDraft;
  receiptFile: File | null;
  suggestion: Category | null;
  onDraftChange: Dispatch<SetStateAction<TransactionDraft>>;
  onReceiptFileChange: (file: File | null) => void;
  onDescriptionChange: (value: string) => void;
  onCategoryTouched: (value: boolean) => void;
  onSubmit: () => void;
  onTransactionsImported: (importedTransactions: number) => void;
  onClose: () => void;
};

export default function TransactionModal({
  isOpen,
  draft,
  receiptFile,
  suggestion,
  onDraftChange,
  onReceiptFileChange,
  onDescriptionChange,
  onCategoryTouched,
  onSubmit,
  onTransactionsImported,
  onClose,
}: TransactionModalProps) {
  const [mode, setMode] = useState<'single' | 'batch'>('single');

  useEffect(() => {
    if (!isOpen) {
      setMode('single');
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const isInstallmentPayment = draft.paymentMethod === 'CARTAO_CREDITO_PARCELADO';

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto bg-slate-950/45 p-4">
      <div className="mx-auto flex min-h-full items-center justify-center">
        <div
          className={`max-h-[calc(100vh-2rem)] w-full overflow-y-auto rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)] transition-all ${
            mode === 'batch' ? 'max-w-6xl' : 'max-w-2xl'
          }`}
        >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">
              {mode === 'single' ? 'Lançamento rápido' : 'Importação em lote'}
            </p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">
              {mode === 'single' ? (draft.category === 'INVESTIMENTO' ? 'Novo investimento' : draft.type === 'RECEITA' ? 'Nova receita' : 'Nova despesa') : 'Importar extrato bancário'}
            </h3>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              {mode === 'single'
                ? 'Registre uma movimentação com sugestão automática de categoria, parcelamento e anexo fiscal.'
                : 'Envie OFX, CSV, TSV ou Excel, revise cada sugestão e confirme apenas as movimentações que deseja importar.'}
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

        <div className="mt-6 grid grid-cols-2 gap-2 rounded-[20px] bg-slate-100 p-1.5">
          <ModeButton active={mode === 'single'} label="Lançamento único" onClick={() => setMode('single')} />
          <ModeButton active={mode === 'batch'} label="Importar planilha/OFX" onClick={() => setMode('batch')} />
        </div>

        {mode === 'batch' ? (
          <div className="mt-6">
            <OFXUploader compact onImported={onTransactionsImported} />
          </div>
        ) : (
          <>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <SelectField
            label="Tipo"
            options={[
              { value: 'DESPESA', label: 'Despesa' },
              { value: 'RECEITA', label: 'Receita' },
              { value: 'INVESTIMENTO', label: 'Investimento' },
            ]}
            value={draft.category === 'INVESTIMENTO' ? 'INVESTIMENTO' : draft.type}
            onChange={(value) => {
              onCategoryTouched(true);
              onDraftChange((currentValue) => ({ ...currentValue,
                type: value === 'INVESTIMENTO' ? 'DESPESA' : value as 'RECEITA' | 'DESPESA',
                category: value === 'INVESTIMENTO' ? 'INVESTIMENTO' : 'OUTROS' }));
            }}
          />
          {draft.category === 'INVESTIMENTO' && <SelectField label="Fluxo do investimento" value={draft.type}
            options={[{ value: 'DESPESA', label: 'Saída: aplicação / compra' }, { value: 'RECEITA', label: 'Entrada: resgate / provento' }]}
            onChange={(value) => onDraftChange((current) => ({ ...current, type: value as 'RECEITA' | 'DESPESA' }))} />}
          {draft.category === 'INVESTIMENTO' && <p className="text-xs leading-5 text-slate-500 md:col-span-2">Este lançamento registra apenas o dinheiro. Para atualizar quantidade e custo médio de um ativo, use Nova movimentação na aba Investimentos, que já cria o lançamento financeiro automaticamente.</p>}
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
            options={Object.entries(categoryLabels).filter(([value]) => draft.category !== 'INVESTIMENTO' || value === 'INVESTIMENTO').map(([value, label]) => ({ value, label }))}
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

        <label className="mt-5 flex min-h-[140px] cursor-pointer flex-col items-center justify-center rounded-[24px] border border-dashed border-emerald-200 bg-emerald-50/50 p-5 text-center">
          <span className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-700">Nota fiscal opcional</span>
          <span className="mt-3 text-sm leading-7 text-slate-600">
            Se existir comprovante agora, você já pode anexar aqui. No parcelado, um upload cobre o grupo inteiro.
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
          </>
        )}
      </div>
    </div>
    </div>
  );
}

function ModeButton({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button
      className={`rounded-2xl px-4 py-3 text-sm font-semibold transition ${
        active ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
      }`}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}
