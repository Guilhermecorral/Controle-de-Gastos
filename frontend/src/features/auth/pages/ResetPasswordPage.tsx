import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import CaptchaField from '../../../components/CaptchaField';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { useLogoutMutation, useResetPasswordMutation } from '../../../lib/queries';
import { useAuthStore } from '../../../store/auth';
import { Field } from '../../shared/ui';
import AuthLayout from '../components/AuthLayout';
import AuthSessionNotice from '../components/AuthSessionNotice';
import PasswordStrengthCard from '../components/PasswordStrengthCard';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, logout, user } = useAuthStore();
  const searchParams = new URLSearchParams(window.location.search);
  const [token] = useState(searchParams.get('token') ?? '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [captchaToken, setCaptchaToken] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const resetPasswordMutation = useResetPasswordMutation();
  const logoutMutation = useLogoutMutation();

  return (
    <AuthLayout
      eyebrow="Nova senha"
      title="Redefinir senha"
      description="Escolha uma nova senha para voltar a acessar sua conta com segurança."
      showSidePanel={false}
    >
      {isAuthenticated && (
        <AuthSessionNotice
          email={user?.email ?? ''}
          onContinue={() => navigate('/app')}
          onSwitchAccount={() => {
            logoutMutation.mutate(undefined, {
              onSettled: () => {
                queryClient.clear();
                logout();
              },
            });
          }}
        />
      )}

      {!token && (
        <div className="rounded-[22px] border border-amber-100 bg-amber-50 px-4 py-3 text-sm leading-7 text-amber-700">
          O link de redefinição chegou incompleto. Volte ao e-mail e abra novamente o botão enviado pelo Farol Financeiro.
        </div>
      )}

      <Field label="Nova senha">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </Field>

      <Field label="Confirmar nova senha">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
        />
      </Field>

      <PasswordStrengthCard password={password} />

      <CaptchaField value={captchaToken} onChange={setCaptchaToken} />

      {feedbackMessage && (
        <div className="rounded-[22px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm leading-7 text-emerald-700">
          {feedbackMessage}
        </div>
      )}

      {errorMessage && (
        <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
          {errorMessage}
        </div>
      )}

      <button
        className="rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        disabled={resetPasswordMutation.isPending || !password || !confirmPassword || !token}
        onClick={() => {
          setFeedbackMessage('');
          setErrorMessage('');
          resetPasswordMutation.mutate(
            {
              token,
              password,
              confirmPassword,
              captchaToken: captchaToken || undefined,
            },
            {
              onSuccess: (response) => {
                setFeedbackMessage(response.message);
                window.setTimeout(() => navigate('/login'), 1500);
              },
              onError: (error) => {
                setErrorMessage(
                  getApiErrorMessage(
                    error,
                    'Não foi possível redefinir a senha. Confira o link, a nova senha e tente novamente.',
                  ),
                );
              },
            },
          );
        }}
        type="button"
      >
        {resetPasswordMutation.isPending ? 'Salvando...' : 'Salvar nova senha'}
      </button>

      <div className="flex flex-wrap gap-4 text-sm">
        <Link className="font-semibold text-slate-600 transition hover:text-emerald-600" to="/login">
          Voltar ao login
        </Link>
        <Link className="font-semibold text-emerald-600 transition hover:text-emerald-700" to="/cadastro">
          Criar conta
        </Link>
      </div>
    </AuthLayout>
  );
}
