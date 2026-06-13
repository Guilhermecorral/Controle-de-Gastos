import { useEffect, useState } from 'react';
import { CookiePreferences, readCookiePreferences, saveCookiePreferences } from '../lib/cookiePreferences';

// Controla a experiência inicial de cookies do app e já prepara o terreno para uma política mais madura.
export default function CookieConsent() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const existingPreferences = readCookiePreferences();

    if (existingPreferences) {
      return;
    }

    setIsVisible(true);
  }, []);

  const acceptAll = () => {
    const nextPreferences = { necessary: true, analytics: true, preferences: true } as CookiePreferences;
    saveCookiePreferences(nextPreferences);
    setIsVisible(false);
  };

  const keepEssentialOnly = () => {
    const nextPreferences = { necessary: true, analytics: false, preferences: false } as CookiePreferences;
    saveCookiePreferences(nextPreferences);
    setIsVisible(false);
  };

  if (!isVisible) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 w-[min(360px,calc(100vw-2rem))]">
      <div className="rounded-[24px] border border-emerald-100 bg-white/95 p-4 shadow-[0_24px_60px_rgba(15,23,42,0.16)] backdrop-blur">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-600">Cookies</p>
        <h3 className="mt-2 text-lg font-semibold text-slate-900">Privacidade sem atrapalhar.</h3>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          Mantemos só os essenciais ou liberamos opcionais para melhorar a experiência.
        </p>

        <div className="mt-4 grid gap-2 sm:grid-cols-2">
          <button
            className="rounded-full border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
            onClick={keepEssentialOnly}
            type="button"
          >
            Só essenciais
          </button>
          <button
            className="rounded-full bg-emerald-500 px-4 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
            onClick={acceptAll}
            type="button"
          >
            Aceitar opcionais
          </button>
        </div>
      </div>
    </div>
  );
}
