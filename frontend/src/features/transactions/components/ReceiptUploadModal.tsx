import { TransactionResponse } from '../../../types';
import { categoryLabels, formatCurrency, formatIsoDate } from '../../../lib/mockFinance';

type ReceiptUploadModalProps = {
  transaction: TransactionResponse | null;
  file: File | null;
  isSubmitting: boolean;
  onFileChange: (file: File | null) => void;
  onSubmit: () => void;
  onClose: () => void;
};

export default function ReceiptUploadModal({
  transaction,
  file,
  isSubmitting,
  onFileChange,
  onSubmit,
  onClose,
}: ReceiptUploadModalProps) {
  if (!transaction) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto bg-slate-950/45 p-4">
      <div className="mx-auto flex min-h-full items-center justify-center">
        <div className="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Nota fiscal</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">Anexar comprovante da transação</h3>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              O arquivo fica salvo na sua conta para consulta futura por ano, mês e dia do envio.
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

        <div className="mt-6 grid gap-4 rounded-[24px] bg-slate-50 p-5 md:grid-cols-2">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">Transação</p>
            <p className="mt-2 text-lg font-semibold text-slate-900">{transaction.description}</p>
            <p className="mt-2 text-sm text-slate-600">{categoryLabels[transaction.category]}</p>
          </div>
          <div className="grid gap-2 text-sm text-slate-600">
            <p>Data da transação: <span className="font-semibold text-slate-900">{formatIsoDate(transaction.transactionDate)}</span></p>
            <p>Tipo: <span className="font-semibold text-slate-900">{transaction.type === 'RECEITA' ? 'Receita' : 'Despesa'}</span></p>
            <p>Valor: <span className="font-semibold text-slate-900">{formatCurrency(transaction.amount)}</span></p>
          </div>
        </div>

        <label className="mt-6 flex min-h-[180px] cursor-pointer flex-col items-center justify-center rounded-[28px] border border-dashed border-emerald-200 bg-emerald-50/50 p-6 text-center">
          <span className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-700">Selecione o arquivo</span>
          <span className="mt-3 text-sm leading-7 text-slate-600">
            Aceita PDF, JPG e PNG com até 10 MB. O back valida o tipo real do arquivo antes de salvar.
          </span>
          <span className="mt-5 rounded-full border border-emerald-200 bg-white px-4 py-2 text-sm font-semibold text-emerald-700">
            {file ? file.name : 'Escolher nota fiscal'}
          </span>
          <input
            accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
            className="hidden"
            type="file"
            onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
          />
        </label>

        {transaction.receipt && (
          <div className="mt-5 rounded-[22px] border border-slate-100 bg-slate-50 p-4 text-sm text-slate-600">
            Arquivo atual: <span className="font-semibold text-slate-900">{transaction.receipt.originalFilename}</span>
          </div>
        )}

        <div className="mt-6 flex flex-wrap justify-end gap-3">
          <button
            className="rounded-full border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            onClick={onClose}
            type="button"
          >
            Cancelar
          </button>
          <button
            className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
            disabled={!file || isSubmitting}
            onClick={onSubmit}
            type="button"
          >
            {isSubmitting ? 'Enviando...' : 'Salvar nota fiscal'}
          </button>
        </div>
      </div>
    </div>
    </div>
  );
}
