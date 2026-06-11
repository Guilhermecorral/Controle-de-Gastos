import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { AuthUser } from '../../../types';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import {
  useBeginTwoFactorSetupMutation,
  useConfirmTwoFactorMutation,
  useDeleteAccountMutation,
  useDisableTwoFactorMutation,
  useLogoutMutation,
  useTwoFactorStatusQuery,
  useUpdateProfileMutation,
} from '../../../lib/queries';
import { useAuthStore } from '../../../store/auth';
import { Field, SectionCard } from '../../shared/ui';

type SettingsPageProps = {
  onLogout: () => void;
  user: AuthUser | null;
};

const settingsSections = [
  { id: 'perfil', label: 'Perfil', helper: 'Nome, e-mail e visão geral da conta.' },
  { id: 'conta', label: 'Conta', helper: 'Sessão, autenticação extra e exclusão.' },
  { id: 'preferencias', label: 'Preferências', helper: 'Como o Farol Financeiro se comporta para você.' },
  { id: 'privacidade', label: 'Privacidade e cookies', helper: 'Explicação clara sobre dados, consentimento e analytics.' },
] as const;

export default function SettingsPage({ onLogout, user }: SettingsPageProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { updateUser } = useAuthStore();
  const logoutMutation = useLogoutMutation();
  const updateProfileMutation = useUpdateProfileMutation();
  const deleteAccountMutation = useDeleteAccountMutation();
  const twoFactorStatusQuery = useTwoFactorStatusQuery();
  const beginTwoFactorSetupMutation = useBeginTwoFactorSetupMutation();
  const confirmTwoFactorMutation = useConfirmTwoFactorMutation();
  const disableTwoFactorMutation = useDisableTwoFactorMutation();

  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [profileMessage, setProfileMessage] = useState('');
  const [profileError, setProfileError] = useState('');
  const [deletePassword, setDeletePassword] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [showDeleteAccountForm, setShowDeleteAccountForm] = useState(false);
  const [twoFactorCode, setTwoFactorCode] = useState('');
  const [disableTwoFactorCode, setDisableTwoFactorCode] = useState('');
  const [disableTwoFactorPassword, setDisableTwoFactorPassword] = useState('');
  const [twoFactorMessage, setTwoFactorMessage] = useState('');
  const [twoFactorError, setTwoFactorError] = useState('');
  const [twoFactorSecret, setTwoFactorSecret] = useState('');
  const [twoFactorOtpAuthUri, setTwoFactorOtpAuthUri] = useState('');
  const [twoFactorAccountName, setTwoFactorAccountName] = useState('');

  useEffect(() => {
    setName(user?.name ?? '');
    setEmail(user?.email ?? '');
  }, [user?.email, user?.name]);

  const twoFactorStatus = twoFactorStatusQuery.data;

  function syncLocalTwoFactor(enabled: boolean) {
    if (!user) {
      return;
    }

    updateUser({
      name: user.name,
      email: user.email,
      role: user.role,
      twoFactorEnabled: enabled,
    });
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[240px_minmax(0,1fr)]">
      <aside className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
        <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Configurações</p>
        <nav className="mt-4 grid gap-2">
          {settingsSections.map((section) => (
            <a
              key={section.id}
              href={`#${section.id}`}
              className="rounded-[22px] bg-slate-50 px-4 py-4 text-left transition hover:bg-slate-100"
            >
              <p className="font-semibold text-slate-900">{section.label}</p>
              <p className="mt-1 text-sm leading-6 text-slate-500">{section.helper}</p>
            </a>
          ))}
        </nav>
      </aside>

      <div className="space-y-6">
        <SectionCard title="Perfil">
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_280px]">
            <div id="perfil" className="grid gap-4 scroll-mt-24">
              <Field label="Nome">
                <input
                  className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                />
              </Field>
              <Field label="E-mail">
                <input
                  className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </Field>

              {profileMessage && (
                <div className="rounded-[22px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm leading-7 text-emerald-700">
                  {profileMessage}
                </div>
              )}
              {profileError && (
                <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
                  {profileError}
                </div>
              )}

              <button
                className="w-fit rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                disabled={updateProfileMutation.isPending || !name.trim() || !email.trim()}
                onClick={() => {
                  setProfileMessage('');
                  setProfileError('');
                  updateProfileMutation.mutate(
                    { name: name.trim(), email: email.trim() },
                    {
                      onSuccess: (response) => {
                        updateUser(response);
                        setProfileMessage('Dados atualizados com sucesso.');
                      },
                      onError: (error) => {
                        setProfileError(getApiErrorMessage(error, 'Não foi possível atualizar seus dados agora.'));
                      },
                    },
                  );
                }}
                type="button"
              >
                {updateProfileMutation.isPending ? 'Salvando...' : 'Salvar alterações'}
              </button>
            </div>

            <div className="rounded-[24px] border border-slate-100 bg-slate-50 p-5">
              <p className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-600">Resumo da conta</p>
              <div className="mt-4 grid gap-4">
                <InfoLine label="Perfil de acesso" value={user?.role ?? 'USER'} />
                <InfoLine label="Sessão atual" value="Ativa neste navegador" />
                <InfoLine
                  label="Dois fatores"
                  value={user?.twoFactorEnabled ? 'Ativo no autenticador' : 'Desligado'}
                />
              </div>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="Conta">
          <div id="conta" className="grid gap-5 scroll-mt-24">
            <p className="max-w-3xl text-sm leading-7 text-slate-600">
              Esta área centraliza ações sensíveis. Quando você precisa sair, proteger melhor o acesso ou encerrar a conta,
              o caminho deve ser claro e discreto.
            </p>

            <div className="flex flex-wrap gap-3">
              <button
                className="rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                onClick={() => {
                  logoutMutation.mutate(undefined, {
                    onSettled: () => onLogout(),
                  });
                }}
                type="button"
              >
                Encerrar sessão
              </button>
            </div>

            <div className="rounded-[24px] border border-slate-100 bg-slate-50 p-5">
              <p className="font-semibold text-slate-900">Autenticação em dois fatores</p>
              <p className="mt-2 text-sm leading-7 text-slate-600">
                Adicione uma segunda confirmação pelo aplicativo autenticador para reduzir o risco de invasão mesmo se a senha vazar.
              </p>

              {twoFactorMessage && (
                <div className="mt-4 rounded-[22px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm leading-7 text-emerald-700">
                  {twoFactorMessage}
                </div>
              )}
              {twoFactorError && (
                <div className="mt-4 rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
                  {twoFactorError}
                </div>
              )}

              <div className="mt-4 grid gap-4">
                <InfoLine
                  label="Status atual"
                  value={
                    twoFactorStatusQuery.isLoading
                      ? 'Carregando...'
                      : twoFactorStatus?.enabled
                        ? 'Proteção ativa'
                        : twoFactorStatus?.pendingSetup
                          ? 'Configuração pendente'
                          : 'Proteção extra desligada'
                  }
                />

                {!twoFactorStatus?.enabled && (
                  <button
                    className="w-fit rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                    disabled={beginTwoFactorSetupMutation.isPending}
                    onClick={() => {
                      setTwoFactorError('');
                      setTwoFactorMessage('');
                      beginTwoFactorSetupMutation.mutate(undefined, {
                        onSuccess: (response) => {
                          setTwoFactorSecret(response.secret);
                          setTwoFactorOtpAuthUri(response.otpAuthUri);
                          setTwoFactorAccountName(response.accountName);
                          setTwoFactorMessage('Segredo gerado. Cadastre agora no seu app autenticador e confirme com um código.');
                        },
                        onError: (error) => {
                          setTwoFactorError(getApiErrorMessage(error, 'Não foi possível iniciar o segundo fator agora.'));
                        },
                      });
                    }}
                    type="button"
                  >
                    {beginTwoFactorSetupMutation.isPending ? 'Gerando...' : 'Gerar chave do autenticador'}
                  </button>
                )}

                {(twoFactorSecret || twoFactorStatus?.pendingSetup) && !twoFactorStatus?.enabled && (
                  <div className="grid gap-4 rounded-[22px] border border-emerald-100 bg-white p-4">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">Passo 1: cadastre no autenticador</p>
                      <p className="mt-2 text-sm leading-7 text-slate-600">
                        Conta: <span className="font-semibold text-slate-900">{twoFactorAccountName || user?.email}</span>
                      </p>
                      {twoFactorSecret && (
                        <p className="mt-2 break-all rounded-2xl bg-slate-50 px-4 py-3 text-sm font-semibold tracking-[0.16em] text-slate-900">
                          {twoFactorSecret}
                        </p>
                      )}
                      {twoFactorOtpAuthUri && (
                        <p className="mt-2 text-xs leading-6 text-slate-500">
                          URI manual do autenticador: {twoFactorOtpAuthUri}
                        </p>
                      )}
                    </div>

                    <Field label="Passo 2: confirme com o código de 6 dígitos">
                      <input
                        className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 tracking-[0.24em] outline-none transition focus:border-emerald-400 focus:bg-white"
                        inputMode="numeric"
                        maxLength={6}
                        placeholder="000000"
                        value={twoFactorCode}
                        onChange={(event) => setTwoFactorCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      />
                    </Field>

                    <button
                      className="w-fit rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
                      disabled={confirmTwoFactorMutation.isPending || twoFactorCode.length !== 6}
                      onClick={() => {
                        setTwoFactorError('');
                        setTwoFactorMessage('');
                        confirmTwoFactorMutation.mutate(
                          { code: twoFactorCode },
                          {
                            onSuccess: () => {
                              setTwoFactorCode('');
                              setTwoFactorSecret('');
                              setTwoFactorOtpAuthUri('');
                              setTwoFactorAccountName('');
                              syncLocalTwoFactor(true);
                              setTwoFactorMessage('Autenticação em dois fatores ativada com sucesso.');
                            },
                            onError: (error) => {
                              setTwoFactorError(getApiErrorMessage(error, 'Não foi possível confirmar o código agora.'));
                            },
                          },
                        );
                      }}
                      type="button"
                    >
                      {confirmTwoFactorMutation.isPending ? 'Confirmando...' : 'Confirmar proteção extra'}
                    </button>
                  </div>
                )}

                {twoFactorStatus?.enabled && (
                  <div className="grid gap-4 rounded-[22px] border border-amber-100 bg-white p-4">
                    <p className="text-sm leading-7 text-slate-600">
                      Para desligar, informe sua senha atual e um código válido do autenticador.
                    </p>
                    <div className="grid gap-4 md:grid-cols-2">
                      <Field label="Senha atual">
                        <input
                          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                          type="password"
                          value={disableTwoFactorPassword}
                          onChange={(event) => setDisableTwoFactorPassword(event.target.value)}
                        />
                      </Field>
                      <Field label="Código do autenticador">
                        <input
                          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 tracking-[0.24em] outline-none transition focus:border-emerald-400 focus:bg-white"
                          inputMode="numeric"
                          maxLength={6}
                          placeholder="000000"
                          value={disableTwoFactorCode}
                          onChange={(event) => setDisableTwoFactorCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                        />
                      </Field>
                    </div>
                    <button
                      className="w-fit rounded-full border border-rose-200 bg-rose-50 px-5 py-3 text-sm font-semibold text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={
                        disableTwoFactorMutation.isPending
                        || !disableTwoFactorPassword
                        || disableTwoFactorCode.length !== 6
                      }
                      onClick={() => {
                        setTwoFactorError('');
                        setTwoFactorMessage('');
                        disableTwoFactorMutation.mutate(
                          { password: disableTwoFactorPassword, code: disableTwoFactorCode },
                          {
                            onSuccess: () => {
                              setDisableTwoFactorPassword('');
                              setDisableTwoFactorCode('');
                              syncLocalTwoFactor(false);
                              setTwoFactorMessage('Autenticação em dois fatores desligada.');
                            },
                            onError: (error) => {
                              setTwoFactorError(getApiErrorMessage(error, 'Não foi possível desligar o segundo fator agora.'));
                            },
                          },
                        );
                      }}
                      type="button"
                    >
                      {disableTwoFactorMutation.isPending ? 'Desligando...' : 'Desligar autenticação extra'}
                    </button>
                  </div>
                )}
              </div>
            </div>

            <div className="rounded-[24px] border border-rose-100 bg-rose-50 p-5">
              <p className="font-semibold text-slate-900">Excluir conta</p>
              <p className="mt-2 text-sm leading-7 text-slate-600">
                Se você decidir encerrar sua conta, pedimos sua senha atual para evitar exclusões indevidas.
              </p>

              {!showDeleteAccountForm ? (
                <button
                  className="mt-4 text-sm font-semibold text-rose-700 underline underline-offset-4 transition hover:text-rose-800"
                  onClick={() => setShowDeleteAccountForm(true)}
                  type="button"
                >
                  Excluir minha conta
                </button>
              ) : (
                <div className="mt-4 grid gap-3">
                  <Field label="Senha atual">
                    <input
                      className="h-12 w-full rounded-2xl border border-rose-100 bg-white px-4 outline-none transition focus:border-rose-300"
                      type="password"
                      value={deletePassword}
                      onChange={(event) => setDeletePassword(event.target.value)}
                    />
                  </Field>

                  {deleteError && (
                    <div className="rounded-[18px] border border-rose-200 bg-white px-4 py-3 text-sm leading-7 text-rose-700">
                      {deleteError}
                    </div>
                  )}

                  <div className="flex flex-wrap gap-3">
                    <button
                      className="rounded-full bg-rose-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-rose-300"
                      disabled={deleteAccountMutation.isPending || !deletePassword}
                      onClick={() => {
                        setDeleteError('');
                        deleteAccountMutation.mutate(
                          { password: deletePassword },
                          {
                            onSuccess: () => {
                              queryClient.clear();
                              onLogout();
                              navigate('/');
                            },
                            onError: (error) => {
                              setDeleteError(getApiErrorMessage(error, 'Não foi possível excluir a conta agora.'));
                            },
                          },
                        );
                      }}
                      type="button"
                    >
                      {deleteAccountMutation.isPending ? 'Excluindo...' : 'Confirmar exclusão'}
                    </button>
                    <button
                      className="rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                      onClick={() => {
                        setShowDeleteAccountForm(false);
                        setDeletePassword('');
                        setDeleteError('');
                      }}
                      type="button"
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </SectionCard>

        <SectionCard title="Preferências">
          <div id="preferencias" className="grid gap-4 scroll-mt-24 md:grid-cols-2">
            <PreferenceCard
              title="Resumo inicial"
              body="Abra o app já olhando o mês atual e os principais números que importam para sua rotina."
            />
            <PreferenceCard
              title="Lembretes futuros"
              body="Quando essa camada evoluir, poderá avisar sobre compras da lista de desejos e movimentos importantes."
            />
          </div>
        </SectionCard>

        <section
          id="privacidade"
          className="scroll-mt-24 rounded-[28px] bg-slate-900 p-8 text-slate-100 shadow-[0_24px_70px_rgba(15,23,42,0.16)]"
        >
          <div className="flex flex-wrap items-start justify-between gap-4 border-b border-white/10 pb-5">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Privacidade e cookies</p>
              <h3 className="mt-2 text-2xl font-semibold text-white">Transparência no uso de dados</h3>
              <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-300">
                O Farol Financeiro informa, em linguagem direta, quais dados são usados para manter sua conta ativa, proteger seu acesso e melhorar o produto quando você autoriza medições opcionais.
              </p>
            </div>
          </div>

          <div className="mt-8 grid gap-8 md:grid-cols-2 xl:grid-cols-4">
            <PrivacyColumn
              title="Sua conta"
              items={[
                'Nome, e-mail e senha protegida são usados para criar e manter sua conta.',
                'Segredos do autenticador são criptografados antes de serem persistidos no banco.',
                'A exclusão da conta exige confirmação por senha para evitar abuso.',
              ]}
            />
            <PrivacyColumn
              title="Cookies"
              items={[
                'Cookies essenciais mantêm sua sessão ativa e guardam suas escolhas de privacidade.',
                'Cookies opcionais só devem ser ativados com o seu consentimento.',
                'Você pode revisar a qualquer momento o que fica ligado no navegador.',
              ]}
            />
            <PrivacyColumn
              title="Medição e analytics"
              items={[
                'Com seu consentimento, o produto pode usar Google Analytics para entender páginas mais usadas e estabilidade de navegação.',
                'Esses dados ajudam a priorizar melhorias e corrigir gargalos de uso.',
                'Sem autorização, a medição opcional permanece desligada.',
              ]}
            />
            <PrivacyColumn
              title="Proteção e direitos"
              items={[
                'Login, recuperação de senha, rate limit e segundo fator ajudam a bloquear abuso.',
                'Tentativas excessivas podem ser bloqueadas por um período curto para proteger sua conta.',
                'Você pode corrigir dados, pedir exclusão e revisar preferências de privacidade.',
              ]}
            />
          </div>

          <div className="mt-8 border-t border-white/10 pt-5 text-sm leading-7 text-slate-400">
            O Farol Financeiro usa o mínimo necessário para autenticação, segurança, funcionamento do produto e medições opcionais autorizadas por você.
          </div>
        </section>
      </div>
    </div>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[18px] border border-white/40 bg-white px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-900">{value}</p>
    </div>
  );
}

function PreferenceCard({ title, body }: { title: string; body: string }) {
  return (
    <article className="rounded-[22px] border border-slate-100 bg-slate-50 p-5">
      <p className="font-semibold text-slate-900">{title}</p>
      <p className="mt-2 text-sm leading-7 text-slate-600">{body}</p>
    </article>
  );
}

function PrivacyColumn({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <p className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-300">{title}</p>
      <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </div>
  );
}
