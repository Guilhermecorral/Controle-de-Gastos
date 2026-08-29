import { useEffect } from 'react';
import AppRouter from './app/router';
import AppProviders from './providers/AppProviders';
import { useAuthStore } from './store/auth';

export default function App() {
  const { bootStatus, hydrated, hydrate } = useAuthStore();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  if (!hydrated) {
    const isWakingBackend = bootStatus === 'waking';

    return (
      <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#f4f6f1] px-4">
        <div className="absolute -left-24 top-12 h-72 w-72 rounded-full bg-emerald-200/30 blur-3xl" />
        <div className="absolute -bottom-28 right-0 h-80 w-80 rounded-full bg-teal-200/25 blur-3xl" />
        <div className="relative max-w-md rounded-[32px] border border-emerald-100 bg-white/95 px-8 py-8 text-center shadow-[0_22px_70px_rgba(15,23,42,0.10)] backdrop-blur">
          <div className="mx-auto mb-5 h-11 w-11 animate-pulse rounded-2xl bg-emerald-500 shadow-[0_0_28px_rgba(16,185,129,0.45)]" />
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-emerald-600">Farol Financeiro</p>
          <h1 className="mt-3 text-xl font-bold text-slate-950">
            {isWakingBackend ? 'Acordando seu ambiente...' : 'Conferindo sua sessão...'}
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            {isWakingBackend
              ? 'O servidor estava em repouso e já está sendo preparado. Vamos continuar automaticamente assim que ele responder.'
              : 'Estamos verificando sua sessão com segurança.'}
          </p>
          {isWakingBackend && (
            <p className="mt-4 text-xs font-semibold text-emerald-700">
              Na hospedagem gratuita, a primeira abertura pode levar até um minuto.
            </p>
          )}
        </div>
      </div>
    );
  }

  return (
    <AppProviders>
      <AppRouter />
    </AppProviders>
  );
}
