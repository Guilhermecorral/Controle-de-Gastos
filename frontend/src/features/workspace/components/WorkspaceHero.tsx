import { ReactNode } from 'react';
import { ViewId } from '../types';
import { viewAccentMap, viewMeta } from '../constants';

type WorkspaceHeroProps = {
  currentView: ViewId;
  children: ReactNode;
};

export default function WorkspaceHero({ currentView, children }: WorkspaceHeroProps) {
  const accent = viewAccentMap[currentView];

  return (
    <section className={`hero-glow relative overflow-hidden rounded-[32px] border px-6 py-6 shadow-[0_24px_70px_rgba(15,23,42,0.18)] lg:px-8 ${accent.panelClass}`}>
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.08),transparent_28%),linear-gradient(180deg,rgba(255,255,255,0.03),transparent_58%)]" />
      <div className="relative z-10 grid gap-5">
        <div className="max-w-3xl">
          <p className={`text-sm font-semibold uppercase tracking-[0.18em] ${accent.accentClass}`}>{accent.eyebrow}</p>
          <h2 className="mt-2 text-3xl font-semibold">{viewMeta[currentView].label}</h2>
          <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-200">{accent.description}</p>
        </div>

        {children}
      </div>
    </section>
  );
}
