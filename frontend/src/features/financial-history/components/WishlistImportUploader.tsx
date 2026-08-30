import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import { categoryLabels } from '../../../lib/mockFinance';
import type { Category, WishlistListResponse, WishlistPriority } from '../../../types';

type PreviewItem = {
  index: number;
  description: string;
  originalPrice: number;
  priority: WishlistPriority;
  category: Category;
  notes: string;
  suggestedListName: string;
  selectedByDefault: boolean;
  rationale: string;
};

type EditableItem = PreviewItem & { enabled: boolean; price: string; listId: string };
type PreviewResponse = { format: string; items: PreviewItem[]; warnings: string[] };

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

export default function WishlistImportUploader({ lists }: { lists: WishlistListResponse[] }) {
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [items, setItems] = useState<EditableItem[]>([]);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [format, setFormat] = useState('');
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const selected = useMemo(() => items.filter((item) => item.enabled), [items]);

  const preview = async (selectedFile: File) => {
    setFile(selectedFile);
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      const response = await api.post<PreviewResponse>('/wishlist/import/preview', formData, { timeout: 120_000 });
      const defaultList = lists.find((list) => list.isDefault) ?? lists[0];
      setItems(response.data.items.map((item) => {
        const suggestedList = lists.find((list) => normalize(list.name) === normalize(item.suggestedListName));
        return {
          ...item,
          enabled: item.selectedByDefault,
          price: item.originalPrice > 0 ? String(item.originalPrice) : '',
          listId: String(suggestedList?.id ?? defaultList?.id ?? ''),
        };
      }));
      setWarnings(response.data.warnings);
      setFormat(response.data.format);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Não foi possível interpretar esta lista agora.');
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const update = (index: number, patch: Partial<EditableItem>) => {
    setItems((current) => current.map((item) => item.index === index ? { ...item, ...patch } : item));
  };

  const confirm = async () => {
    if (!selected.length) {
      setError('Selecione ao menos um desejo para importar.');
      return;
    }
    if (selected.some((item) => !item.description.trim() || !item.listId)) {
      setError('Revise os nomes e a lista de destino dos itens selecionados.');
      return;
    }
    setImporting(true);
    setError(null);
    try {
      const response = await api.post<{ importedItems: number; message: string }>('/wishlist/import', {
        items: selected.map((item) => ({
          description: item.description.trim(),
          originalPrice: Number(item.price) || 0,
          priority: item.priority,
          category: item.category,
          notes: item.notes,
          listId: Number(item.listId),
        })),
      }, { timeout: 120_000 });
      await queryClient.invalidateQueries({ queryKey: ['wishlist'] });
      setMessage(response.data.message);
      setItems([]);
      setFile(null);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Não foi possível importar os desejos selecionados.');
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="rounded-[28px] border-2 border-dashed border-amber-200 bg-amber-50/70 p-6">
        <p className="text-xs font-bold uppercase tracking-[0.22em] text-amber-700">Lista livre</p>
        <h3 className="mt-2 text-xl font-semibold text-slate-950">Importe até uma lista feita no Bloco de Notas</h3>
        <p className="mt-2 max-w-3xl text-sm leading-7 text-slate-600">
          Aceitamos TXT, PDF com texto, CSV, TSV e Excel. Só o nome é obrigatório; preço, categoria e prioridade podem ser completados depois.
        </p>
        <label className="mt-5 inline-flex cursor-pointer rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white">
          {loading ? 'Lendo lista...' : 'Escolher lista'}
          <input
            className="hidden"
            type="file"
            accept=".txt,.pdf,.csv,.tsv,.xls,.xlsx"
            disabled={loading || importing}
            onChange={(event) => event.target.files?.[0] && preview(event.target.files[0])}
          />
        </label>
        {file && <p className="mt-3 text-sm text-slate-600">{file.name}{format ? ` · ${format}` : ''}</p>}
      </div>

      {warnings.length > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          {warnings.map((warning) => <p key={warning}>{warning}</p>)}
        </div>
      )}

      {items.length > 0 && (
        <div className="overflow-x-auto rounded-[26px] border border-slate-200 bg-white">
          <table className="min-w-[1050px] w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.14em] text-slate-500">
              <tr><th className="p-3">Usar</th><th className="p-3">Desejo</th><th className="p-3">Preço opcional</th><th className="p-3">Categoria</th><th className="p-3">Prioridade</th><th className="p-3">Lista</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((item) => (
                <tr key={item.index} className={!item.enabled ? 'opacity-50' : ''}>
                  <td className="p-3"><input type="checkbox" checked={item.enabled} onChange={(event) => update(item.index, { enabled: event.target.checked })} /></td>
                  <td className="p-3"><input className="w-full min-w-[260px] rounded-xl border border-slate-200 px-3 py-2" value={item.description} onChange={(event) => update(item.index, { description: event.target.value })} /><p className="mt-1 text-xs text-slate-500">{item.rationale}</p></td>
                  <td className="p-3"><input type="number" min="0" step="0.01" placeholder="Não informado" className="w-36 rounded-xl border border-slate-200 px-3 py-2" value={item.price} onChange={(event) => update(item.index, { price: event.target.value })} /></td>
                  <td className="p-3"><select className="rounded-xl border border-slate-200 px-3 py-2" value={item.category} onChange={(event) => update(item.index, { category: event.target.value as Category })}>{Object.entries(categoryLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></td>
                  <td className="p-3"><select className="rounded-xl border border-slate-200 px-3 py-2" value={item.priority} onChange={(event) => update(item.index, { priority: event.target.value as WishlistPriority })}><option value="ALTO">Alta</option><option value="MEDIA">Média</option><option value="BAIXO">Baixa</option></select></td>
                  <td className="p-3"><select className="min-w-40 rounded-xl border border-slate-200 px-3 py-2" value={item.listId} onChange={(event) => update(item.index, { listId: event.target.value })}>{lists.map((list) => <option key={list.id} value={list.id}>{list.name}</option>)}</select></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex items-center justify-between gap-4 border-t border-slate-100 p-4">
            <p className="text-sm text-slate-600">{selected.length} de {items.length} desejo(s) selecionado(s)</p>
            <button type="button" disabled={importing || !selected.length} onClick={confirm} className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">{importing ? 'Importando...' : 'Confirmar desejos'}</button>
          </div>
        </div>
      )}

      {message && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-700">{message}</div>}
      {error && <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{error}</div>}
    </div>
  );
}
