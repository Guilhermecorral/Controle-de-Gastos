import { ReactNode } from 'react';
import { FeatureChip } from '../../shared/ui';

type AuthLayoutProps = {
  eyebrow: string;
  title: string;
  description: string;
  sideTitle?: string;
  sideText?: string;
  showSidePanel?: boolean;
  children: ReactNode;
};

export default function AuthLayout({
  eyebrow,
  title,
  description,
  sideTitle = '',
  sideText = '',
  showSidePanel = true,
  children,
}: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4 py-12">
      <div className={`grid w-full gap-8 ${showSidePanel ? 'max-w-5xl lg:grid-cols-[0.92fr_1.08fr]' : 'max-w-2xl'}`}>
        {showSidePanel && (
          <section className="rounded-[32px] bg-slate-900 px-7 py-8 text-white shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">{eyebrow}</p>
            <h1 className="mt-4 text-4xl font-semibold leading-tight text-balance">{sideTitle}</h1>
            <p className="mt-5 text-sm leading-7 text-slate-300">{sideText}</p>

            <div className="mt-8 grid gap-3">
              <FeatureChip label="Clareza" helper="Microtextos curtos e humanos." dark />
              <FeatureChip label="Segurança" helper="Força da senha e base para anti-bot." dark />
              <FeatureChip label="Confiança" helper="Privacidade e recuperação já pensadas." dark />
            </div>
          </section>
        )}

        <section className="rounded-[32px] border border-emerald-100 bg-white p-7 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">{eyebrow}</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">{title}</h2>
          <p className="mt-3 text-sm leading-7 text-slate-600">{description}</p>
          <div className="mt-6 grid gap-4">{children}</div>
        </section>
      </div>
    </div>
  );
}
