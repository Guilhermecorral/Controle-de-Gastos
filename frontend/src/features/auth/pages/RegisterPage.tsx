import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import CaptchaField from '../../../components/CaptchaField';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { useLoginMutation, useLogoutMutation, useRegisterMutation } from '../../../lib/queries';
import { useAuthStore } from '../../../store/auth';
import { Field } from '../../shared/ui';
import AuthLayout from '../components/AuthLayout';
import AuthSessionNotice from '../components/AuthSessionNotice';
import PasswordStrengthCard from '../components/PasswordStrengthCard';

export default function RegisterPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, login, logout, user } = useAuthStore();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [captchaToken, setCaptchaToken] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const submitLockRef = useRef(false);
  const registerMutation = useRegisterMutation();
  const loginMutation = useLoginMutation();
  const logoutMutation = useLogoutMutation();

  function finishAuthentication(nameValue: string, emailValue: string, roleValue: string, twoFactorEnabledValue: boolean) {
    queryClient.clear();
    login({ name: nameValue, email: emailValue, role: roleValue, twoFactorEnabled: twoFactorEnabledValue });
    navigate('/app', { replace: true });
  }

  function handleRegisterFailure(error: unknown) {
    loginMutation.mutate(
      {
        email,
        password,
        captchaToken: captchaToken || undefined,
        },
      {
        onSuccess: (response) => {
          if ('requiresTwoFactor' in response) {
            submitLockRef.current = false;
            setErrorMessage('Sua conta exige autenticação em dois fatores. Entre pela tela de login para concluir com o código do autenticador.');
            navigate('/login', { replace: true });
            return;
          }

          finishAuthentication(response.name, response.email, response.role, response.twoFactorEnabled);
        },
        onError: () => {
          submitLockRef.current = false;
          setErrorMessage(
            getApiErrorMessage(error, 'Não foi possível concluir o cadastro agora. Revise os dados ou tente novamente.'),
          );
        },
      },
    );
  }

  return (
    <AuthLayout
      eyebrow="Cadastro"
      title="Crie sua conta"
      description="Preencha seus dados, escolha uma senha segura e comece a acompanhar sua vida financeira em poucos minutos."
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

      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Nome">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Seu nome"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </Field>
        <Field label="E-mail">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="voce@email.com"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>
        <Field label="Confirmar e-mail">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Repita seu e-mail"
            type="email"
            value={confirmEmail}
            onChange={(event) => setConfirmEmail(event.target.value)}
          />
        </Field>
        <Field label="Senha">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Crie uma senha forte"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>
      </div>

      <Field label="Confirmar senha">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          placeholder="Repita sua senha"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
        />
      </Field>

      <PasswordStrengthCard password={password} />

      <CaptchaField value={captchaToken} onChange={setCaptchaToken} />

      {errorMessage && (
        <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
          {errorMessage}
        </div>
      )}

      <button
        className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
        disabled={
          !name
          || email !== confirmEmail
          || password !== confirmPassword
          || registerMutation.isPending
          || loginMutation.isPending
          || submitLockRef.current
        }
        onClick={() => {
          if (submitLockRef.current) {
            return;
          }

          submitLockRef.current = true;
          setErrorMessage('');
          registerMutation.mutate(
            { name, email, password, captchaToken: captchaToken || undefined },
            {
              onSuccess: (response) => {
                finishAuthentication(response.name, response.email, response.role, response.twoFactorEnabled);
              },
              onError: (error) => {
                handleRegisterFailure(error);
              },
            },
          );
        }}
        type="button"
      >
        {registerMutation.isPending || loginMutation.isPending ? 'Criando conta...' : 'Criar conta e entrar'}
      </button>

      <p className="text-sm leading-7 text-slate-600">
        Já tem conta?{' '}
        <Link className="font-semibold text-emerald-600 transition hover:text-emerald-700" to="/login">
          Voltar para o login
        </Link>
      </p>
    </AuthLayout>
  );
}
