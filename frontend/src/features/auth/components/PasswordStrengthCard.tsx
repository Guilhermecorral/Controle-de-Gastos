type PasswordStrengthCardProps = {
  password: string;
};

export default function PasswordStrengthCard({ password }: PasswordStrengthCardProps) {
  const passwordChecks = [
    { label: 'Pelo menos 8 caracteres', valid: password.length >= 8 },
    { label: 'Pelo menos 1 número', valid: /\d/.test(password) },
    { label: 'Pelo menos 1 caractere especial', valid: /[^A-Za-z0-9]/.test(password) },
    { label: 'Pelo menos 1 letra maiúscula', valid: /[A-Z]/.test(password) },
  ];

  const passwordScore = passwordChecks.filter((rule) => rule.valid).length;
  const passwordStrength = passwordScore <= 1 ? 'Fraca' : passwordScore <= 3 ? 'Média' : 'Forte';

  return (
    <div className="rounded-[24px] bg-slate-50 p-4">
      <div className="flex items-center justify-between gap-4">
        <p className="font-semibold text-slate-900">Força da senha</p>
        <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">
          {passwordStrength}
        </span>
      </div>

      <div className="mt-4 h-3 rounded-full bg-white">
        <div
          className={`h-3 rounded-full ${
            passwordScore <= 1
              ? 'bg-rose-500'
              : passwordScore <= 3
                ? 'bg-amber-500'
                : 'bg-emerald-500'
          }`}
          style={{ width: `${(passwordScore / passwordChecks.length) * 100}%` }}
        />
      </div>

      <div className="mt-4 grid gap-2 text-sm text-slate-600">
        {passwordChecks.map((rule) => (
          <div key={rule.label} className="flex items-center gap-3">
            <div className={`h-3 w-3 rounded-full ${rule.valid ? 'bg-emerald-500' : 'bg-slate-300'}`} />
            <span>{rule.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
