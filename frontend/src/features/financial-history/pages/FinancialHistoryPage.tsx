import { useState } from 'react';
import type { TransactionResponse, WishlistListResponse } from '../../../types';
import OFXUploader from '../../ofx-upload/components/OFXUploader';
import ReceiptBatchUploader from '../components/ReceiptBatchUploader';
import WishlistImportUploader from '../components/WishlistImportUploader';

type Mode = 'transactions' | 'wishlist' | 'receipts';

const modes: Array<{ id: Mode; label: string; helper: string }> = [
  { id: 'transactions', label: 'Transações', helper: 'OFX, CSV, TSV e Excel' },
  { id: 'wishlist', label: 'Lista de desejos', helper: 'TXT, PDF, CSV e Excel' },
  { id: 'receipts', label: 'Notas fiscais', helper: 'PDF, JPG, PNG ou pasta' },
];

export default function FinancialHistoryPage({
  transactions,
  wishlistLists,
}: {
  transactions: TransactionResponse[];
  wishlistLists: WishlistListResponse[];
}) {
  const [mode, setMode] = useState<Mode>('transactions');

  return (
    <section className="space-y-6">
      <div className="rounded-[32px] border border-white/70 bg-white/92 p-6 shadow-[0_24px_70px_rgba(15,23,42,0.12)] backdrop-blur-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.16em] text-cyan-600">Central de importação</p>
        <h3 className="mt-2 text-2xl font-semibold text-slate-900">Três caminhos, uma revisão segura</h3>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">
          Escolha o que deseja trazer para o Farol Financeiro. Nenhuma transação, desejo ou nota é gravada antes da sua confirmação.
        </p>
        <div className="mt-6 grid gap-3 md:grid-cols-3">
          {modes.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setMode(item.id)}
              className={`rounded-[22px] border p-4 text-left transition ${mode === item.id ? 'border-cyan-300 bg-slate-950 text-white shadow-lg' : 'border-slate-200 bg-slate-50 text-slate-900 hover:bg-white'}`}
            >
              <span className="block font-semibold">{item.label}</span>
              <span className={`mt-1 block text-xs ${mode === item.id ? 'text-slate-300' : 'text-slate-500'}`}>{item.helper}</span>
            </button>
          ))}
        </div>
      </div>

      {mode === 'transactions' && <OFXUploader />}
      {mode === 'wishlist' && <WishlistImportUploader lists={wishlistLists} />}
      {mode === 'receipts' && <ReceiptBatchUploader transactions={transactions} />}
    </section>
  );
}
