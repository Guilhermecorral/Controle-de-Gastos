import { useEffect, useRef } from 'react';

declare global {
  interface Window {
    turnstile?: {
      render: (
        target: string | HTMLElement,
        options: {
          sitekey: string;
          callback?: (token: string) => void;
          'expired-callback'?: () => void;
          'error-callback'?: () => void;
          theme?: 'light' | 'dark';
        },
      ) => string;
      remove: (widgetId: string) => void;
    };
  }
}

type CaptchaFieldProps = {
  value: string;
  onChange: (token: string) => void;
};

const turnstileSiteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY?.trim() || '';
const turnstileAllowedHosts = new Set(
  (import.meta.env.VITE_TURNSTILE_ALLOWED_HOSTS || 'farolfinanceiro.online,www.farolfinanceiro.online')
    .split(',')
    .map((hostname: string) => hostname.trim().toLowerCase())
    .filter(Boolean),
);
let turnstileScriptPromise: Promise<void> | null = null;

// Renderiza o desafio anti-bot quando o ambiente possuir uma chave pública configurada.
export default function CaptchaField({ value, onChange }: CaptchaFieldProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const widgetIdRef = useRef<string | null>(null);
  const currentHostname = window.location.hostname.toLowerCase();
  const isAuthorizedHostname = import.meta.env.DEV || turnstileAllowedHosts.has(currentHostname);

  useEffect(() => {
    if (!turnstileSiteKey || !isAuthorizedHostname || !containerRef.current) {
      return;
    }

    let cancelled = false;

    loadTurnstileScript()
      .then(() => {
        if (cancelled || !containerRef.current || !window.turnstile) {
          return;
        }

        widgetIdRef.current = window.turnstile.render(containerRef.current, {
          sitekey: turnstileSiteKey,
          theme: 'light',
          callback: (token) => onChange(token),
          'expired-callback': () => onChange(''),
          'error-callback': () => onChange(''),
        });
      })
      .catch(() => onChange(''));

    return () => {
      cancelled = true;

      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.remove(widgetIdRef.current);
        widgetIdRef.current = null;
      }
    };
  }, [isAuthorizedHostname, onChange]);

  if (!turnstileSiteKey) {
    return (
      <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 px-4 py-4 text-sm leading-7 text-slate-500">
        <p className="font-semibold text-slate-900">Proteção contra abuso</p>
        <p className="mt-2">Esta etapa será ativada quando a proteção automática estiver ligada neste ambiente.</p>
      </div>
    );
  }

  if (!isAuthorizedHostname) {
    const officialUrl = `https://farolfinanceiro.online${window.location.pathname}`;

    return (
      <div className="rounded-[22px] border border-amber-200 bg-amber-50 px-4 py-4 text-sm leading-7 text-amber-900">
        <p className="font-semibold">Ambiente de preview não homologado</p>
        <p className="mt-2">
          A verificação anti-bot só funciona no domínio oficial. Isso evita o uso indevido da chave de segurança em endereços temporários.
        </p>
        <a className="mt-3 inline-flex font-semibold underline underline-offset-4" href={officialUrl}>
          Continuar em farolfinanceiro.online
        </a>
      </div>
    );
  }

  return (
    <div className="rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-sm font-semibold text-slate-900">Verificação anti-bot</p>
      <p className="mt-2 text-sm leading-7 text-slate-500">
        Essa etapa ajuda a reduzir abuso automatizado em cadastro, login e recuperação de senha.
      </p>
      <div className="mt-4 min-h-[70px]" ref={containerRef} />
      {value ? (
        <p className="mt-2 text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">Verificação pronta</p>
      ) : null}
    </div>
  );
}

function loadTurnstileScript() {
  if (window.turnstile) {
    return Promise.resolve();
  }

  if (turnstileScriptPromise) {
    return turnstileScriptPromise;
  }

  turnstileScriptPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Não foi possível carregar o script do captcha'));
    document.head.appendChild(script);
  });

  return turnstileScriptPromise;
}
