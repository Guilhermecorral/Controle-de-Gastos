import { useState } from 'react';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import TaxObligationCard from '../components/TaxObligationCard';
import TaxObligationForm from '../components/TaxObligationForm';
import PaymentConfirmDialog from '../components/PaymentConfirmDialog';
import { useTaxProfile, useSaveTaxProfile } from '../hooks/useTaxProfile';
import { useConfirmTaxPayment, useCreateTaxObligation, useDeleteTaxObligation, useTaxObligations, useUpdateTaxObligation } from '../hooks/useTaxObligations';
import type { PaymentConfirmationInput, TaxObligation, TaxObligationInput, TaxStatus } from '../types';
import TaxOnboardingModal from './TaxOnboardingModal';

const filters: Array<{ id: TaxStatus | 'TODOS'; label: string }> = [
  { id: 'TODOS', label: 'Todos' }, { id: 'A_PAGAR', label: 'A pagar' },
  { id: 'ATRASADA', label: 'Atrasados' }, { id: 'PAGA', label: 'Pagos' },
  { id: 'EM_REVISAO', label: 'Em revisão' }, { id: 'ISENTA', label: 'Isentos' },
];

export default function TaxPage() {
  const profile = useTaxProfile();
  const saveProfile = useSaveTaxProfile();
  const obligations = useTaxObligations(profile.data === 'PF');
  const create = useCreateTaxObligation();
  const update = useUpdateTaxObligation();
  const remove = useDeleteTaxObligation();
  const confirm = useConfirmTaxPayment();
  const [filter, setFilter] = useState<TaxStatus | 'TODOS'>('TODOS');
  const [editing, setEditing] = useState<TaxObligation | null | 'new'>(null);
  const [paying, setPaying] = useState<TaxObligation | null>(null);
  const [deleting, setDeleting] = useState<TaxObligation | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function chooseProfile(type: 'PF' | 'PJ') {
    setError('');
    try { await saveProfile.mutateAsync(type); } catch (cause) { setError(getApiErrorMessage(cause, 'Não foi possível salvar o perfil.')); }
  }
  async function save(input: TaxObligationInput) {
    setError('');
    try {
      if (editing && editing !== 'new') await update.mutateAsync({ id: editing.id, input });
      else await create.mutateAsync(input);
      setEditing(null);
      setMessage('Obrigação salva. Nenhuma despesa foi criada.');
    } catch (cause) { setError(getApiErrorMessage(cause, 'Não foi possível salvar a obrigação.')); }
  }
  async function pay(input: PaymentConfirmationInput) {
    if (!paying) return;
    setError('');
    try {
      await confirm.mutateAsync({ id: paying.id, input });
      setPaying(null);
      setMessage('Pagamento confirmado. A despesa vinculada foi registrada no Histórico Financeiro.');
    } catch (cause) { setError(getApiErrorMessage(cause, 'Não foi possível confirmar o pagamento.')); }
  }
  async function deleteSelected() {
    if (!deleting) return;
    setError('');
    try {
      await remove.mutateAsync(deleting.id);
      setDeleting(null);
      setMessage('Obrigação excluída.');
    } catch (cause) { setError(getApiErrorMessage(cause, 'Não foi possível excluir a obrigação.')); }
  }

  if (profile.isLoading) return <div className="glass-panel rounded-[28px] p-8 text-slate-600">Carregando seu perfil tributário...</div>;
  if (profile.isError) return <div className="glass-panel rounded-[28px] p-8 text-rose-700" role="alert">Não foi possível carregar o perfil tributário. <button className="underline" onClick={() => profile.refetch()} type="button">Tentar novamente</button></div>;
  if (!profile.data) return <TaxOnboardingModal error={error} onChoose={chooseProfile} pending={saveProfile.isPending} />;
  if (profile.data === 'PJ') return <section className="glass-panel rounded-[28px] p-8"><p className="text-4xl" aria-hidden="true">▣</p><h2 className="mt-3 text-2xl font-semibold">Central de Tributos PJ</h2><p className="mt-3 text-slate-600">Funcionalidade em desenvolvimento. Planejada para uma versão futura do Farol Financeiro.</p><button className="mt-5 rounded-full bg-slate-900 px-5 py-3 font-semibold text-white" disabled={saveProfile.isPending} onClick={() => chooseProfile('PF')} type="button">Alterar perfil para PF</button>{error && <p className="mt-3 text-rose-700" role="alert">{error}</p>}</section>;

  const visible = (obligations.data ?? []).filter((item) => filter === 'TODOS' || item.status === filter);
  return (
    <section className="space-y-6">
      <div className="glass-panel rounded-[28px] border border-emerald-100 bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-[0.17em] text-emerald-700">Pessoa Física</p><h2 className="mt-2 text-2xl font-semibold text-slate-900">Central de Tributos</h2><p className="mt-2 text-sm text-slate-600">Acompanhe estimativas, guias e pagamentos sem antecipar despesas.</p></div><button className="rounded-full bg-slate-900 px-5 py-3 font-semibold text-white hover:bg-slate-800" onClick={() => { setError(''); setEditing('new'); }} type="button">Nova obrigação</button></div>
      </div>
      {message && <p className="rounded-2xl border border-emerald-100 bg-emerald-50 p-4 text-sm text-emerald-800" role="status">{message}</p>}
      {error && !editing && !paying && !deleting && <p className="rounded-2xl bg-rose-50 p-4 text-sm text-rose-700" role="alert">{error}</p>}
      <div aria-label="Filtrar obrigações por status" className="flex flex-wrap gap-2">{filters.map((item) => <button aria-pressed={filter === item.id} className={`rounded-full px-4 py-2 text-sm font-semibold ${filter === item.id ? 'bg-emerald-700 text-white' : 'border border-slate-200 bg-white text-slate-600'}`} key={item.id} onClick={() => setFilter(item.id)} type="button">{item.label}</button>)}</div>
      {obligations.isLoading && <p className="glass-panel rounded-[24px] p-6 text-slate-600">Carregando obrigações...</p>}
      {obligations.isError && <p className="glass-panel rounded-[24px] p-6 text-rose-700" role="alert">Não foi possível carregar as obrigações. <button className="underline" onClick={() => obligations.refetch()} type="button">Tentar novamente</button></p>}
      {!obligations.isLoading && !obligations.isError && visible.length === 0 && <p className="glass-panel rounded-[24px] p-8 text-slate-600">{filter === 'TODOS' ? 'Nenhuma obrigação cadastrada. Comece por Nova obrigação.' : 'Nenhuma obrigação neste filtro.'}</p>}
      <div className="grid gap-4">{visible.map((item) => <TaxObligationCard key={item.id} obligation={item} onDelete={() => { setError(''); setDeleting(item); }} onEdit={() => { setError(''); setEditing(item); }} onPay={() => { setError(''); setPaying(item); }} />)}</div>
      {editing && <TaxObligationForm error={error} key={editing === 'new' ? 'new' : editing.id} obligation={editing === 'new' ? null : editing} onClose={() => { setEditing(null); setError(''); }} onSave={save} pending={create.isPending || update.isPending} />}
      {paying && <PaymentConfirmDialog error={error} key={paying.id} obligation={paying} onClose={() => { setPaying(null); setError(''); }} onConfirm={pay} pending={confirm.isPending} />}
      {deleting && <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4"><section aria-modal="true" className="w-full max-w-md rounded-[28px] bg-white p-6 shadow-2xl" role="dialog"><h2 className="text-xl font-semibold">Excluir {deleting.name}?</h2><p className="mt-2 text-sm text-slate-600">Esta ação remove a obrigação não paga. Nenhuma despesa será criada.</p>{error && <p className="mt-3 text-rose-700" role="alert">{error}</p>}<div className="mt-5 flex justify-end gap-2"><button className="rounded-full border border-slate-200 px-4 py-2" disabled={remove.isPending} onClick={() => { setDeleting(null); setError(''); }} type="button">Cancelar</button><button className="rounded-full bg-rose-700 px-4 py-2 text-white disabled:opacity-50" disabled={remove.isPending} onClick={deleteSelected} type="button">{remove.isPending ? 'Excluindo...' : 'Excluir'}</button></div></section></div>}
    </section>
  );
}
