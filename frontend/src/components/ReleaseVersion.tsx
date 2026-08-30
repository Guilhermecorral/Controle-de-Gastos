type ReleaseVersionProps = {
  className?: string;
};

export default function ReleaseVersion({ className = '' }: ReleaseVersionProps) {
  const shortCommit = __APP_BUILD_COMMIT__ === 'local' ? 'local' : __APP_BUILD_COMMIT__.slice(0, 7);
  const buildDate = new Date(__APP_BUILD_TIME__);
  const buildLabel = Number.isNaN(buildDate.getTime())
    ? __APP_BUILD_TIME__
    : buildDate.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

  return (
    <span
      aria-label={`Versão ${__APP_VERSION__}`}
      className={`inline-flex text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400 ${className}`}
      title={`Farol Financeiro v${__APP_VERSION__} · commit ${shortCommit} · build ${buildLabel}`}
    >
      v{__APP_VERSION__}
    </span>
  );
}
