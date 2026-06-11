import { TransactionReceiptResponse, TransactionResponse } from '../../../types';
import { categoryLabels, formatCurrency, formatIsoDate, paymentMethodLabels } from '../../../lib/mockFinance';
import { EmptyState, Field, LoadingCard, SectionCard, SelectField, UnavailableCard } from '../../shared/ui';

type ReceiptsPageProps = {
  receipts: TransactionReceiptResponse[];
  availableTransactions: TransactionResponse[];
  selectedTransactionId: string;
  selectedFile: File | null;
  year: number;
  month: number;
  yearOptions: number[];
  monthOptions: Array<{ value: number; label: string }>;
  hasError: boolean;
  isLoading: boolean;
  isUploading: boolean;
  onYearChange: (year: number) => void;
  onMonthChange: (month: number) => void;
  onSelectedTransactionChange: (value: string) => void;
  onSelectedFileChange: (file: File | null) => void;
  onUploadReceipt: () => void;
  onDownloadReceipt: (transactionId: number, filename: string) => void;
};

function formatReceiptDateTime(value: string) {
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatBytes(sizeBytes: number) {
  if (sizeBytes >= 1024 * 1024) {
    return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return `${Math.max(1, Math.round(sizeBytes / 1024))} KB`;
}

export default function ReceiptsPage({
  receipts,
  availableTransactions,
  selectedTransactionId,
  selectedFile,
  year,
  month,
  yearOptions,
  monthOptions,
  hasError,
  isLoading,
  isUploading,
  onYearChange,
  onMonthChange,
  onSelectedTransactionChange,
  onSelectedFileChange,
  onUploadReceipt,
  onDownloadReceipt,
}: ReceiptsPageProps) {
  if (isLoading) {
    return <LoadingCard label="Carregando notas fiscais do período..." />;
  }

  if (hasError) {
    return <UnavailableCard label="Não foi possível carregar as notas fiscais deste período." />;
  }

  return (
    <div className="space-y-6">
      <SectionCard title="Consulta por período">
        <div className="grid gap-4 md:grid-cols-3">
          <SelectField
            label="Ano"
            options={yearOptions.map((value) => ({ value: String(value), label: String(value) }))}
            value={String(year)}
            onChange={(value) => onYearChange(Number(value))}
          />
          <SelectField
            label="Mês"
            options={monthOptions.map((option) => ({ value: String(option.value), label: option.label }))}
            value={String(month)}
            onChange={(value) => onMonthChange(Number(value))}
          />
          <Field label="Leitura do período">
            <div className="flex h-12 items-center rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-600">
              {receipts.length} nota(s) fiscal(is) anexada(s) neste mês
            </div>
          </Field>
        </div>
      </SectionCard>

      <SectionCard title="Anexar nota fiscal">
        <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
          <SelectField
            label="Transação"
            options={[
              { value: '', label: 'Escolha a transação' },
              ...availableTransactions.map((transaction) => ({
                value: String(transaction.id),
                label: `${formatIsoDate(transaction.transactionDate)} · ${transaction.description}`,
              })),
            ]}
            value={selectedTransactionId}
            onChange={onSelectedTransactionChange}
          />
          <label className="flex h-12 cursor-pointer items-center rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-600 transition hover:border-emerald-300 hover:bg-white">
            <span className="truncate">{selectedFile ? selectedFile.name : 'Escolher arquivo'}</span>
            <input
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              className="hidden"
              type="file"
              onChange={(event) => onSelectedFileChange(event.target.files?.[0] ?? null)}
            />
          </label>
          <button
            className="h-12 rounded-full bg-emerald-500 px-5 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
            disabled={!selectedTransactionId || !selectedFile || isUploading}
            onClick={onUploadReceipt}
            type="button"
          >
            {isUploading ? 'Enviando...' : 'Salvar nota'}
          </button>
        </div>
        <p className="mt-4 text-sm leading-7 text-slate-600">
          Se a transação escolhida fizer parte de um parcelamento, uma única nota fiscal será replicada para o grupo inteiro.
        </p>
      </SectionCard>

      <SectionCard title="Notas fiscais do período">
        {receipts.length === 0 ? (
          <EmptyState label="Nenhuma nota fiscal foi anexada no período selecionado." />
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            {receipts.map((receipt) => (
              <article
                key={`${receipt.transactionId}-${receipt.uploadedAt}`}
                className="rounded-[24px] border border-slate-100 bg-slate-50 p-5"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">
                      {receipt.type === 'RECEITA' ? 'Receita' : 'Despesa'}
                    </p>
                    <h4 className="mt-2 text-xl font-semibold text-slate-900">{receipt.description}</h4>
                    <p className="mt-2 text-sm text-slate-600">{categoryLabels[receipt.category]}</p>
                  </div>
                  <button
                    className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100"
                    onClick={() => onDownloadReceipt(receipt.transactionId, receipt.originalFilename)}
                    type="button"
                  >
                    Baixar arquivo
                  </button>
                </div>

                <div className="mt-5 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
                  <p>Data da transação: <span className="font-semibold text-slate-900">{formatIsoDate(receipt.transactionDate)}</span></p>
                  <p>Pagamento: <span className="font-semibold text-slate-900">{paymentMethodLabels[receipt.paymentMethod]}</span></p>
                  <p>Valor: <span className="font-semibold text-slate-900">{formatCurrency(receipt.amount)}</span></p>
                  <p>Arquivo: <span className="font-semibold text-slate-900">{formatBytes(receipt.sizeBytes)}</span></p>
                  <p>
                    Abrangência: <span className="font-semibold text-slate-900">
                      {receipt.coveredTransactions > 1 ? `${receipt.coveredTransactions} transações` : '1 transação'}
                    </span>
                  </p>
                  <p className="sm:col-span-2">
                    Anexada em: <span className="font-semibold text-slate-900">{formatReceiptDateTime(receipt.uploadedAt)}</span>
                  </p>
                </div>

                <div className="mt-5 rounded-[20px] border border-white bg-white px-4 py-3 text-sm text-slate-600">
                  <p className="font-semibold text-slate-900">{receipt.originalFilename}</p>
                  <p className="mt-1">{receipt.contentType}</p>
                </div>
              </article>
            ))}
          </div>
        )}
      </SectionCard>
    </div>
  );
}
