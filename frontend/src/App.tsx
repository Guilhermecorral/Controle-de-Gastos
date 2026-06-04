import { useEffect } from 'react';
import AppRouter from './app/router';
import AppProviders from './providers/AppProviders';
import { useAuthStore } from './store/auth';

export default function App() {
  const { hydrated, hydrate } = useAuthStore();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  if (!hydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4">
        <div className="rounded-[28px] border border-emerald-100 bg-white px-6 py-5 text-sm font-semibold text-slate-600 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
          Preparando seu ambiente com segurança...
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
