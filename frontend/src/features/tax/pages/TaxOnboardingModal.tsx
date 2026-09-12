import type { TaxProfileType } from '../types';

export default function TaxOnboardingModal({ onChoose, pending, error }: {
  onChoose: (profile: TaxProfileType) => void;
  pending: boolean;
  error: string;
}) {
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
      <section aria-labelledby="tax-onboarding-title" aria-modal="true" className="w-full max-w-2xl rounded-[30px] border border-white/40 bg-white p-6 shadow-2xl md:p-9" role="dialog">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-emerald-700">Primeiro acesso</p>
        <h2 className="mt-3 text-2xl font-semibold text-slate-900" id="tax-onboarding-title">Como você gostaria de utilizar a Central de Tributos?</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">Sua escolha pode ser alterada depois nas configurações da conta.</p>
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <button className="rounded-[22px] border border-emerald-200 bg-emerald-50 p-5 text-left transition hover:border-emerald-500 hover:bg-emerald-100 disabled:opacity-50" disabled={pending} onClick={() => onChoose('PF')} type="button">
            <span className="text-lg font-semibold text-slate-900">Pessoa Física (PF)</span>
            <span className="mt-2 block text-sm leading-6 text-slate-600">Gerencie IRPF, IPVA, IPTU e outros tributos pessoais.</span>
          </button>
          <button className="rounded-[22px] border border-slate-200 bg-slate-50 p-5 text-left transition hover:border-slate-400 hover:bg-slate-100 disabled:opacity-50" disabled={pending} onClick={() => onChoose('PJ')} type="button">
            <span className="text-lg font-semibold text-slate-900">Pessoa Jurídica (PJ)</span>
            <span className="mt-2 block text-xs font-bold uppercase tracking-widest text-amber-700">Em breve</span>
            <span className="mt-2 block text-sm leading-6 text-slate-600">Funcionalidade em desenvolvimento, sem recursos PJ nesta versão.</span>
          </button>
        </div>
        {error && <p className="mt-4 text-sm text-rose-700" role="alert">{error}</p>}
      </section>
    </div>
  );
}
