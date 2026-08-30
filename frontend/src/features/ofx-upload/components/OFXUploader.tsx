import { useMemo, useState, type ChangeEvent, type DragEvent } from 'react';
import api from '../../../lib/api';
import { categoryLabels, formatCurrency, getSuggestedCategory, paymentMethodLabels } from '../../../lib/mockFinance';
import type { Category, PaymentMethod, TransactionType } from '../../../types';

type PreviewRow = {
  id: string;
  enabled: boolean;
  type: TransactionType;
  description: string;
  category: Category;
  amount: string;
  paymentMethod: PaymentMethod;
  installments: string;
  transactionDate: string;
  confidence: 'ALTA' | 'MEDIA' | 'BAIXA';
  source: string;
  rationale: string;
};

type ImportAnalysis = {
  format: string;
  layout: string;
  processedRows: number;
  detectedTransactions: number;
  sheets: string[];
  warnings: string[];
};

type UploadPreviewResponse = {
  message: string;
  transactions: Array<{
    type: TransactionType;
    description: string;
    category: Category;
    amount: number | string;
    paymentMethod: PaymentMethod;
    installments: number;
    transactionDate: string | null;
    selectedByDefault: boolean;
    confidence: 'ALTA' | 'MEDIA' | 'BAIXA';
    source: string;
    rationale: string;
  }>;
  analysis: ImportAnalysis | null;
};

type ImportResponse = {
  importedTransactions: number;
  message: string;
};

const maxFileBytes = 5 * 1024 * 1024;
const pageSize = 50;

const categoryOptions = Object.keys(categoryLabels) as Category[];
const paymentMethodOptions = Object.keys(paymentMethodLabels) as PaymentMethod[];

function toPreviewRow(transaction: UploadPreviewResponse['transactions'][number], index: number): PreviewRow {
  const description = transaction.description || `Transação ${index + 1}`;
  const smartCategory = transaction.category ?? getSuggestedCategory(description) ?? 'OUTROS';

  return {
    id: `${Date.now()}-${index}`,
    enabled: transaction.selectedByDefault ?? true,
    type: transaction.type ?? 'DESPESA',
    description,
    category: smartCategory,
    amount: String(transaction.amount ?? ''),
    paymentMethod: transaction.paymentMethod ?? 'PIX',
    installments: String(transaction.installments ?? 1),
    transactionDate: transaction.transactionDate?.slice(0, 10) ?? '',
    confidence: transaction.confidence ?? 'ALTA',
    source: transaction.source ?? 'Arquivo importado',
    rationale: transaction.rationale ?? 'Campos reconhecidos no arquivo.',
  };
}

type OFXUploaderProps = {
  compact?: boolean;
  onImported?: (importedTransactions: number) => void;
};

const OFXUploader = ({ compact = false, onImported }: OFXUploaderProps) => {
  const [file, setFile] = useState<File | null>(null);
  const [rows, setRows] = useState<PreviewRow[]>([]);
  const [analysis, setAnalysis] = useState<ImportAnalysis | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedRows = useMemo(() => rows.filter((row) => row.enabled), [rows]);
  const receitasCount = useMemo(() => selectedRows.filter((row) => row.type === 'RECEITA').length, [selectedRows]);
  const despesasCount = useMemo(() => selectedRows.filter((row) => row.type === 'DESPESA').length, [selectedRows]);
  const totalValue = useMemo(
    () => selectedRows.reduce((sum, row) => sum + (Number(row.amount) || 0), 0),
    [selectedRows],
  );
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const visibleRows = useMemo(
    () => rows.slice((currentPage - 1) * pageSize, currentPage * pageSize),
    [currentPage, rows],
  );

  const resetState = (options?: { keepSuccessMessage?: boolean }) => {
    setFile(null);
    setRows([]);
    setAnalysis(null);
    setPage(1);
    setError(null);
    if (!options?.keepSuccessMessage) {
      setSuccessMessage(null);
    }
    setLoading(false);
    setImporting(false);
    setDragActive(false);
  };

  const loadFile = async (selectedFile: File) => {
    if (selectedFile.size > maxFileBytes) {
      setError('O arquivo excede o limite de 5 MB.');
      setRows([]);
      setAnalysis(null);
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await api.post<UploadPreviewResponse>('/ofx/upload', formData);

      const previewRows = response.data.transactions.map(toPreviewRow);
      setRows(previewRows);
      setAnalysis(response.data.analysis);
      setPage(1);
      if (!previewRows.length) {
        setError(response.data.message || 'Nenhuma transação segura foi reconhecida automaticamente.');
      }
    } catch (err: any) {
      const message =
        err?.response?.data?.message
        || err?.message
        || 'Não foi possível ler o arquivo agora.';
      setError(message);
      setRows([]);
      setAnalysis(null);
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0];
    if (!selectedFile) {
      return;
    }

    setFile(selectedFile);
    await loadFile(selectedFile);
  };

  const handleDrop = async (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(false);

    const droppedFile = event.dataTransfer.files?.[0];
    if (!droppedFile) {
      return;
    }

    setFile(droppedFile);
    await loadFile(droppedFile);
  };

  const updateRow = (rowId: string, patch: Partial<PreviewRow>) => {
    setRows((currentRows) =>
      currentRows.map((row) => (row.id === rowId ? { ...row, ...patch } : row)),
    );
  };

  const removeRow = (rowId: string) => {
    setRows((currentRows) => currentRows.filter((row) => row.id !== rowId));
  };

  const selectAll = (enabled: boolean) => {
    setRows((currentRows) => currentRows.map((row) => ({ ...row, enabled })));
  };

  const handleImport = async () => {
    if (!selectedRows.length) {
      setError('Selecione ao menos uma linha para importar.');
      return;
    }

    const invalidRowIndex = selectedRows.findIndex((row) => {
      const amount = Number(row.amount);
      const installments = Number(row.installments);
      return !row.description.trim()
        || !row.transactionDate
        || !Number.isFinite(amount)
        || amount <= 0
        || !Number.isInteger(installments)
        || installments < 1;
    });

    if (invalidRowIndex >= 0) {
      setError(`Revise a linha selecionada ${invalidRowIndex + 1}: descrição, data, valor e parcelas devem ser válidos.`);
      return;
    }

    setImporting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const response = await api.post<ImportResponse>('/transactions/import', {
        transactions: selectedRows.map((row) => ({
          type: row.type,
          description: row.description.trim(),
          category: row.category,
          amount: Number(row.amount),
          paymentMethod: row.paymentMethod,
          installments: Number(row.installments),
          transactionDate: row.transactionDate,
        })),
      });

      const importedCount = response.data.importedTransactions;
      resetState({ keepSuccessMessage: true });
      setSuccessMessage(
        response.data.message
          || `${importedCount} ${importedCount === 1 ? 'transação importada' : 'transações importadas'} com sucesso.`,
      );
      onImported?.(importedCount);
    } catch (err: any) {
      const message =
        err?.response?.data?.message
        || err?.message
        || 'Falha ao importar as transações selecionadas.';
      setError(message);
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className={compact ? 'space-y-4' : 'space-y-6'}>
      <div
        className={`rounded-3xl border-2 border-dashed p-6 transition ${
          dragActive ? 'border-emerald-500 bg-emerald-50' : 'border-emerald-200 bg-emerald-50/60'
        }`}
        onDragOver={(event) => {
          event.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={handleDrop}
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-emerald-600">
              Entrada de dados automática
            </p>
            <h2 className="text-xl font-semibold text-slate-900">
              Arraste OFX, CSV ou Excel, revise e confirme antes de salvar
            </h2>
            <p className="max-w-2xl text-sm leading-6 text-slate-600">
              O arquivo é lido em memória, sem ficar salvo no servidor. Se aparecer
              uma tabela, matriz mensal ou blocos livres, mostramos como cada valor foi
              entendido para você ajustar em segundos.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <input
              id="ofx-upload-input"
              type="file"
              accept=".ofx,.csv,.tsv,.xls,.xlsx"
              className="hidden"
              onChange={handleFileChange}
              disabled={loading || importing}
            />
            <label
              htmlFor="ofx-upload-input"
              className="inline-flex cursor-pointer items-center justify-center rounded-full bg-emerald-600 px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-500"
            >
              {loading ? 'Lendo arquivo...' : 'Selecionar arquivo'}
            </label>
            <button
              type="button"
              onClick={() => resetState()}
              className="inline-flex items-center justify-center rounded-full border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-white"
            >
              Limpar tudo
            </button>
          </div>
        </div>

        {file && (
          <div className="mt-4 inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-xs font-medium text-slate-600 shadow-sm ring-1 ring-slate-200">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            {file.name} · {(file.size / 1024).toFixed(0)} KB
          </div>
        )}
      </div>

      {analysis && (
        <div className="rounded-3xl border border-sky-200 bg-sky-50 p-5 text-slate-700">
          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">Formato</p>
              <p className="mt-2 font-semibold text-slate-900">{analysis.format}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">Estrutura detectada</p>
              <p className="mt-2 font-semibold text-slate-900">{analysis.layout.split('_').join(' ')}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">Leitura</p>
              <p className="mt-2 font-semibold text-slate-900">
                {analysis.processedRows} linhas · {analysis.detectedTransactions} sugestões
              </p>
            </div>
          </div>
          {analysis.sheets.length > 0 && (
            <p className="mt-4 text-sm"><strong>Abas:</strong> {analysis.sheets.join(', ')}</p>
          )}
          {analysis.warnings.length > 0 && (
            <ul className="mt-3 space-y-1 text-sm leading-6 text-slate-600">
              {analysis.warnings.map((warning) => <li key={warning}>• {warning}</li>)}
            </ul>
          )}
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-4">
        <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-600">Linhas</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{rows.length}</p>
        </div>
        <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-600">Selecionadas</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{selectedRows.length}</p>
        </div>
        <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-600">Receitas / Despesas</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">
            {receitasCount} / {despesasCount}
          </p>
        </div>
        <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-600">Valor total</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{formatCurrency(totalValue)}</p>
        </div>
      </div>

      <div className="rounded-3xl bg-slate-950 px-6 py-5 text-slate-100 shadow-xl">
        <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 className="text-lg font-semibold">Pré-visualização editável</h3>
            <p className="text-sm text-slate-300">
              Ajuste descrição, categoria, tipo, pagamento, parcelas ou remova linhas antes de importar.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => selectAll(true)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
            >
              Marcar todas
            </button>
            <button
              type="button"
              onClick={() => selectAll(false)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
            >
              Desmarcar todas
            </button>
          </div>
        </div>

        <div className="mt-4 overflow-x-auto rounded-2xl bg-white text-slate-900">
          <table className="min-w-[1320px] w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-[0.2em] text-slate-500">
              <tr>
                <th className="px-4 py-3 text-left">Usar</th>
                <th className="px-4 py-3 text-left">Leitura</th>
                <th className="px-4 py-3 text-left">Data</th>
                <th className="px-4 py-3 text-left">Descrição</th>
                <th className="px-4 py-3 text-left">Tipo</th>
                <th className="px-4 py-3 text-left">Categoria</th>
                <th className="px-4 py-3 text-left">Valor</th>
                <th className="px-4 py-3 text-left">Pagamento</th>
                <th className="px-4 py-3 text-left">Parcelas</th>
                <th className="px-4 py-3 text-left">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {visibleRows.map((row) => (
                <tr key={row.id} className={!row.enabled ? 'opacity-55' : ''}>
                  <td className="px-4 py-3 align-top">
                    <input
                      type="checkbox"
                      checked={row.enabled}
                      onChange={(event) => updateRow(row.id, { enabled: event.target.checked })}
                      className="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                    />
                  </td>
                  <td className="max-w-[240px] px-4 py-3 align-top">
                    <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-bold tracking-wide ${
                      row.confidence === 'ALTA'
                        ? 'bg-emerald-100 text-emerald-700'
                        : row.confidence === 'MEDIA'
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-rose-100 text-rose-700'
                    }`}>
                      {row.confidence}
                    </span>
                    <p className="mt-2 text-xs font-semibold text-slate-700">{row.source}</p>
                    <p className="mt-1 text-xs leading-5 text-slate-500">{row.rationale}</p>
                  </td>
                  <td className="px-4 py-3 align-top">
                    <input
                      type="date"
                      value={row.transactionDate}
                      onChange={(event) => updateRow(row.id, { transactionDate: event.target.value })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    />
                  </td>
                  <td className="px-4 py-3 align-top">
                    <input
                      type="text"
                      value={row.description}
                      onChange={(event) => {
                        const description = event.target.value;
                        const suggestion = getSuggestedCategory(description);
                        updateRow(row.id, {
                          description,
                          category: suggestion ?? row.category,
                        });
                      }}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    />
                  </td>
                  <td className="px-4 py-3 align-top">
                    <select
                      value={row.type}
                      onChange={(event) => updateRow(row.id, { type: event.target.value as TransactionType })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    >
                      <option value="RECEITA">Receita</option>
                      <option value="DESPESA">Despesa</option>
                    </select>
                  </td>
                  <td className="px-4 py-3 align-top">
                    <select
                      value={row.category}
                      onChange={(event) => updateRow(row.id, { category: event.target.value as Category })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    >
                      {categoryOptions.map((category) => (
                        <option key={category} value={category}>
                          {categoryLabels[category]}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 align-top">
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={row.amount}
                      onChange={(event) => updateRow(row.id, { amount: event.target.value })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    />
                  </td>
                  <td className="px-4 py-3 align-top">
                    <select
                      value={row.paymentMethod}
                      onChange={(event) => updateRow(row.id, { paymentMethod: event.target.value as PaymentMethod })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    >
                      {paymentMethodOptions.map((paymentMethod) => (
                        <option key={paymentMethod} value={paymentMethod}>
                          {paymentMethodLabels[paymentMethod]}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 align-top">
                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={row.installments}
                      onChange={(event) => updateRow(row.id, { installments: event.target.value })}
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-emerald-500"
                    />
                  </td>
                  <td className="px-4 py-3 align-top">
                    <button
                      type="button"
                      onClick={() => removeRow(row.id)}
                      className="rounded-full border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-600 transition hover:bg-rose-50"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan={10} className="px-4 py-10 text-center text-slate-500">
                    Nenhum arquivo carregado ainda. Arraste OFX, CSV, TSV ou Excel para começar.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {rows.length > pageSize && (
          <div className="mt-4 flex items-center justify-between text-sm text-slate-300">
            <span>
              Página {currentPage} de {totalPages} · exibindo até {pageSize} sugestões por vez
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={currentPage === 1}
                onClick={() => setPage(Math.max(1, currentPage - 1))}
                className="rounded-full border border-white/15 px-4 py-2 disabled:opacity-40"
              >
                Anterior
              </button>
              <button
                type="button"
                disabled={currentPage === totalPages}
                onClick={() => setPage(Math.min(totalPages, currentPage + 1))}
                className="rounded-full border border-white/15 px-4 py-2 disabled:opacity-40"
              >
                Próxima
              </button>
            </div>
          </div>
        )}

        <div className="mt-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <p className="text-sm text-slate-300">
            O arquivo é processado em memória e descartado depois da leitura. Não guardamos o extrato bruto no servidor.
          </p>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={handleImport}
              disabled={importing || !selectedRows.length}
              className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-emerald-900"
            >
              {importing ? 'Importando...' : 'Confirmar importação'}
            </button>
          </div>
        </div>
      </div>

      {successMessage && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
          {successMessage}
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-600">
          {error}
        </div>
      )}
    </div>
  );
};

export default OFXUploader;
