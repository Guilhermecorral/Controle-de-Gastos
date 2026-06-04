type AuthSessionNoticeProps = {
  email: string;
  onContinue: () => void;
  onSwitchAccount: () => void;
};

export default function AuthSessionNotice({
  email,
  onContinue,
  onSwitchAccount,
}: AuthSessionNoticeProps) {
  return (
    <div className="rounded-[24px] border border-amber-100 bg-amber-50 px-4 py-4 text-sm leading-7 text-amber-900">
      <p className="font-semibold text-slate-900">Você já está com uma sessão ativa</p>
      <p className="mt-2">
        A sessão atual está aberta com <span className="font-semibold">{email}</span>. Se quiser entrar com outra conta,
        encerre a sessão atual antes de continuar.
      </p>
      <div className="mt-4 flex flex-wrap gap-3">
        <button
          className="rounded-full bg-slate-900 px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-slate-800"
          onClick={onContinue}
          type="button"
        >
          Continuar no app
        </button>
        <button
          className="rounded-full border border-amber-200 bg-white px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-amber-900 transition hover:border-amber-300 hover:bg-amber-100"
          onClick={onSwitchAccount}
          type="button"
        >
          Encerrar sessão e trocar conta
        </button>
      </div>
    </div>
  );
}
