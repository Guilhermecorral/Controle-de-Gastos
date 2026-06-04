import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import CaptchaField from '../../../components/CaptchaField';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { useForgotPasswordMutation, useLogoutMutation } from '../../../lib/queries';
import { useAuthStore } from '../../../store/auth';
import { Field } from '../../shared/ui';
import AuthLayout from '../components/AuthLayout';
import AuthSessionNotice from '../components/AuthSessionNotice';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, logout, user } = useAuthStore();
  const [email, setEmail] = useState('');
  const [captchaToken, setCaptchaToken] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const forgotPasswordMutation = useForgotPasswordMutation();
  const logoutMutation = useLogoutMutation();

  return (
    <AuthLayout
      eyebrow="Recuperação"
      title="Esqueci minha senha"
      description="Informe o e-mail da sua conta. Se ele existir no sistema, você receberá as instruções para criar uma nova senha."
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

      <Field label="E-mail da conta">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          placeholder="voce@email.com"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </Field>

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
        disabled={forgotPasswordMutation.isPending || !email}
        onClick={() => {
          setFeedbackMessage('');
          setErrorMessage('');
          forgotPasswordMutation.mutate(
            { email, captchaToken: captchaToken || undefined },
            {
              onSuccess: (response) => {
                setFeedbackMessage(response.message);
              },
              onError: (error) => {
                setErrorMessage(getApiErrorMessage(error, 'Não foi possível iniciar a recuperação agora. Tente novamente em instantes.'));
              },
            },
          );
        }}
        type="button"
      >
        {forgotPasswordMutation.isPending ? 'Enviando...' : 'Enviar instruções'}
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
