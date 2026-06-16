import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import CaptchaField from '../../../components/CaptchaField';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { useLoginMutation, useLogoutMutation } from '../../../lib/queries';
import { useAuthStore } from '../../../store/auth';
import { Field } from '../../shared/ui';
import AuthLayout from '../components/AuthLayout';
import AuthSessionNotice from '../components/AuthSessionNotice';

function isTwoFactorChallengeResponse(response: unknown): response is { requiresTwoFactor: boolean; message: string } {
  return typeof response === 'object' && response !== null && 'requiresTwoFactor' in response;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, hydrate, logout, user } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [captchaToken, setCaptchaToken] = useState('');
  const [twoFactorCode, setTwoFactorCode] = useState('');
  const [requiresTwoFactor, setRequiresTwoFactor] = useState(false);
  const [twoFactorMessage, setTwoFactorMessage] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const loginMutation = useLoginMutation();
  const logoutMutation = useLogoutMutation();

  const handleSubmit = () => {
    setErrorMessage('');
    setTwoFactorMessage('');

    loginMutation.mutate(
      {
        email,
        password,
        captchaToken: captchaToken || undefined,
        twoFactorCode: twoFactorCode || undefined,
      },
      {
        onSuccess: async (response) => {
          if (isTwoFactorChallengeResponse(response) && response.requiresTwoFactor) {
            setRequiresTwoFactor(true);
            setTwoFactorMessage(response.message);
            return;
          }

          if (isTwoFactorChallengeResponse(response)) {
            setErrorMessage('Não foi possível concluir a etapa de autenticação adicional agora.');
            return;
          }

          queryClient.clear();
          const authenticated = await hydrate();

          if (!authenticated) {
            setErrorMessage('Sua sessão não conseguiu ser confirmada no navegador. Revise os cookies do ambiente e tente novamente.');
            return;
          }

          navigate('/app', { replace: true });
        },
        onError: (error) => {
          setErrorMessage(
            getApiErrorMessage(error, 'Não foi possível entrar. Confira seu e-mail, sua senha e o código do autenticador.'),
          );
        },
      },
    );
  };

  return (
    <AuthLayout
      eyebrow="Entrar"
      title="Acesse sua conta"
      description="Entre com seu e-mail e sua senha para acompanhar seu mês, suas compras planejadas e seu histórico financeiro."
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

      <form
        className="space-y-6"
        onSubmit={(event) => {
          event.preventDefault();
          handleSubmit();
        }}
      >
        <Field label="E-mail">
          <input
            autoComplete="email"
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="voce@email.com"
            type="email"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value);
              setRequiresTwoFactor(false);
              setTwoFactorCode('');
              setTwoFactorMessage('');
            }}
          />
        </Field>

        <Field label="Senha">
          <div className="flex gap-3">
            <input
              autoComplete={showPassword ? 'current-password' : 'current-password'}
              className="h-12 flex-1 rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="Digite sua senha"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
                setRequiresTwoFactor(false);
                setTwoFactorCode('');
                setTwoFactorMessage('');
              }}
            />
            <button
              className="rounded-2xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
              onClick={() => setShowPassword((currentValue) => !currentValue)}
              type="button"
            >
              {showPassword ? 'Ocultar' : 'Ver'}
            </button>
          </div>
        </Field>

        <CaptchaField value={captchaToken} onChange={setCaptchaToken} />

        {requiresTwoFactor && (
          <Field label="Código do autenticador">
            <input
              autoComplete="one-time-code"
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 tracking-[0.24em] outline-none transition focus:border-emerald-400 focus:bg-white"
              inputMode="numeric"
              maxLength={6}
              placeholder="000000"
              value={twoFactorCode}
              onChange={(event) => setTwoFactorCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
            />
          </Field>
        )}

        {twoFactorMessage && (
          <div className="rounded-[22px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm leading-7 text-emerald-700">
            {twoFactorMessage}
          </div>
        )}

        {errorMessage && (
          <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
            {errorMessage}
          </div>
        )}

        <button
          className="w-full rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
          disabled={loginMutation.isPending || !email || !password || (requiresTwoFactor && twoFactorCode.length !== 6)}
          type="submit"
        >
          {loginMutation.isPending ? 'Entrando...' : requiresTwoFactor ? 'Validar e entrar' : 'Entrar no app'}
        </button>

        <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
          <Link className="font-semibold text-slate-600 transition hover:text-emerald-600" to="/esqueci-a-senha">
            Esqueci minha senha
          </Link>
          <Link className="font-semibold text-emerald-600 transition hover:text-emerald-700" to="/cadastro">
            Ainda não tenho conta
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
}
