import { useMemo, useState, type DragEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import { formatCurrency, formatIsoDate } from '../../../lib/mockFinance';
import type { TransactionResponse } from '../../../types';

type Candidate = { transactionId: number; score: number; rationale: string };
type Preview = { fileIndex: number; filename: string; confidence: 'ALTA' | 'MEDIA' | 'BAIXA'; candidates: Candidate[]; rationale: string };
type Row = Preview & { enabled: boolean; transactionId: string };

export default function ReceiptBatchUploader({ transactions }: { transactions: TransactionResponse[] }) {
  const queryClient = useQueryClient();
  const [files, setFiles] = useState<File[]>([]);
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const selectedRows = useMemo(() => rows.filter((row) => row.enabled && row.transactionId), [rows]);
  const transactionById = useMemo(() => new Map(transactions.map((transaction) => [transaction.id, transaction])), [transactions]);

  const analyze = async (selectedFiles: File[]) => {
    const accepted = selectedFiles.filter((file) => /\.(pdf|jpe?g|png)$/i.test(file.name)).slice(0, 30);
    if (!accepted.length) {
      setError('Selecione notas fiscais em PDF, JPG ou PNG.');
      return;
    }
    if (accepted.reduce((total, file) => total + file.size, 0) > 50 * 1024 * 1024) {
      setError('O lote pode ter no máximo 50 MB. Divida a pasta em duas importações.');
      return;
    }
    setFiles(accepted);
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const formData = new FormData();
      accepted.forEach((file) => formData.append('files', file));
      const response = await api.post<Preview[]>('/transactions/receipts/preview', formData, { timeout: 120_000 });
      setRows(response.data.map((preview) => ({
        ...preview,
        enabled: true,
        transactionId: preview.confidence !== 'BAIXA' && preview.candidates[0]
          ? String(preview.candidates[0].transactionId)
          : '',
      })));
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Não foi possível analisar as notas fiscais.');
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    analyze(Array.from(event.dataTransfer.files));
  };

  const update = (fileIndex: number, patch: Partial<Row>) => {
    setRows((current) => current.map((row) => row.fileIndex === fileIndex ? { ...row, ...patch } : row));
  };

  const upload = async () => {
    if (!selectedRows.length) {
      setError('Escolha ao menos uma correspondência antes de anexar.');
      return;
    }
    const transactionIds = selectedRows.map((row) => row.transactionId);
    if (new Set(transactionIds).size !== transactionIds.length) {
      setError('Duas notas estão apontando para a mesma transação. Revise antes de continuar.');
      return;
    }
    setUploading(true);
    setError(null);
    let completed = 0;
    try {
      for (const row of selectedRows) {
        const file = files[row.fileIndex];
        const formData = new FormData();
        formData.append('file', file);
        await api.post(`/transactions/${row.transactionId}/receipt`, formData, { timeout: 120_000 });
        completed++;
      }
      await queryClient.invalidateQueries({ queryKey: ['transaction-receipts'] });
      await queryClient.invalidateQueries({ queryKey: ['transactions'] });
      setMessage(`${completed} nota(s) fiscal(is) anexada(s) com sucesso.`);
      setRows([]);
      setFiles([]);
    } catch (err: any) {
      setError(`${completed} nota(s) foram anexadas; a próxima falhou. ${err?.response?.data?.message || 'Tente novamente.'}`);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-5">
      <div onDragOver={(event) => event.preventDefault()} onDrop={handleDrop} className="rounded-[28px] border-2 border-dashed border-cyan-200 bg-cyan-50/70 p-6">
        <p className="text-xs font-bold uppercase tracking-[0.22em] text-cyan-700">Conciliação fiscal</p>
        <h3 className="mt-2 text-xl font-semibold text-slate-950">Solte várias notas ou escolha uma pasta</h3>
        <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
          PDFs textuais são comparados por valor, data e descrição. Fotos permanecem sem vínculo automático até você escolher a transação correta.
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          <label className="cursor-pointer rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white">{loading ? 'Analisando...' : 'Escolher arquivos'}<input className="hidden" multiple type="file" accept=".pdf,.jpg,.jpeg,.png" disabled={loading || uploading} onChange={(event) => analyze(Array.from(event.target.files ?? []))} /></label>
          <label className="cursor-pointer rounded-full border border-cyan-300 bg-white px-5 py-3 text-sm font-semibold text-cyan-800">Escolher pasta<input className="hidden" type="file" multiple accept=".pdf,.jpg,.jpeg,.png" disabled={loading || uploading} {...({ webkitdirectory: '', directory: '' } as any)} onChange={(event) => analyze(Array.from(event.target.files ?? []))} /></label>
        </div>
      </div>

      {rows.length > 0 && (
        <div className="space-y-3">
          {rows.map((row) => {
            const chosen = transactionById.get(Number(row.transactionId));
            return (
              <article key={row.fileIndex} className={`rounded-[24px] border bg-white p-5 ${row.confidence === 'ALTA' ? 'border-emerald-200' : row.confidence === 'MEDIA' ? 'border-amber-200' : 'border-slate-200'}`}>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="flex items-start gap-3">
                    <input className="mt-1" type="checkbox" checked={row.enabled} onChange={(event) => update(row.fileIndex, { enabled: event.target.checked })} />
                    <div><p className="font-semibold text-slate-950">{row.filename}</p><p className="mt-1 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Confiança {row.confidence}</p><p className="mt-2 text-sm text-slate-600">{row.rationale}</p></div>
                  </div>
                  <select className="min-w-[360px] max-w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm" value={row.transactionId} onChange={(event) => update(row.fileIndex, { transactionId: event.target.value })}>
                    <option value="">Escolha a transação manualmente</option>
                    {transactions.map((transaction) => <option key={transaction.id} value={transaction.id}>{formatIsoDate(transaction.transactionDate)} · {transaction.description} · {formatCurrency(transaction.amount)}</option>)}
                  </select>
                </div>
                {chosen && <p className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">Vínculo escolhido: {chosen.description}. {row.candidates.find((candidate) => candidate.transactionId === chosen.id)?.rationale || 'seleção manual'}</p>}
              </article>
            );
          })}
          <div className="flex justify-end"><button type="button" onClick={upload} disabled={uploading || !selectedRows.length} className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">{uploading ? 'Anexando notas...' : `Anexar ${selectedRows.length} nota(s)`}</button></div>
        </div>
      )}

      {message && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-700">{message}</div>}
      {error && <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{error}</div>}
    </div>
  );
}
