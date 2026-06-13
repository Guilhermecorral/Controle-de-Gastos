import { useMemo, useState } from 'react';
import {
  useAdminOverviewQuery,
  useAdminResetUserPasswordMutation,
  useAdminResetUserTwoFactorMutation,
  useAdminUpdateUserRoleMutation,
  useAdminUpdateUserStatusMutation,
  useAdminUsersQuery,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { AdminUserResponse, Role } from '../../../types';
import { Field, SectionCard } from '../../shared/ui';

export default function AdminPage() {
  const overviewQuery = useAdminOverviewQuery(true);
  const usersQuery = useAdminUsersQuery(true);
  const updateStatusMutation = useAdminUpdateUserStatusMutation();
  const updateRoleMutation = useAdminUpdateUserRoleMutation();
  const resetPasswordMutation = useAdminResetUserPasswordMutation();
  const resetTwoFactorMutation = useAdminResetUserTwoFactorMutation();

  const [selectedUser, setSelectedUser] = useState<AdminUserResponse | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState('');
  const [feedbackError, setFeedbackError] = useState('');

  const users = usersQuery.data ?? [];
  const overview = overviewQuery.data;

  const activeUsers = useMemo(() => users.filter((user) => user.active), [users]);
  const suspendedUsers = useMemo(() => users.filter((user) => !user.active), [users]);

  const syncSelectedUser = (updatedUser: AdminUserResponse) => {
    setSelectedUser((currentValue) => (currentValue?.id === updatedUser.id ? updatedUser : currentValue));
  };

  return (
    <div className="space-y-6">
      <SectionCard title="Painel administrativo">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Usuarios no produto" value={String(overview?.totalUsuarios ?? 0)} helper="Base total cadastrada." />
          <MetricCard label="Usuarios ativos" value={String(overview?.usuariosAtivos ?? activeUsers.length)} helper="Contas prontas para operar." />
          <MetricCard label="Administradores" value={String(overview?.administradores ?? 0)} helper="Contas com acesso total." />
          <MetricCard label="Saldo global" value={formatCurrency(overview?.saldoGlobal ?? 0)} helper="Receitas menos despesas do sistema." />
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <MetricCard label="Receitas totais" value={formatCurrency(overview?.totalReceitas ?? 0)} helper="Volume agregado de entradas." />
          <MetricCard label="Despesas totais" value={formatCurrency(overview?.totalDespesas ?? 0)} helper="Volume agregado de saidas." />
        </div>
      </SectionCard>

      {feedbackMessage && (
        <div className="rounded-[22px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm leading-7 text-emerald-700">
          {feedbackMessage}
        </div>
      )}
      {feedbackError && (
        <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
          {feedbackError}
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_380px]">
        <SectionCard title="Gestao de contas">
          <div className="mb-4 grid gap-3 md:grid-cols-3">
            <InfoPill label="Ativos" value={String(activeUsers.length)} />
            <InfoPill label="Suspensos" value={String(suspendedUsers.length)} />
            <InfoPill label="Com 2FA" value={String(users.filter((user) => user.twoFactorEnabled).length)} />
          </div>

          <div className="overflow-hidden rounded-[24px] border border-slate-100">
            <div className="hidden grid-cols-[1.4fr_1.15fr_0.8fr_0.9fr_0.8fr] gap-4 bg-slate-50 px-5 py-4 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 lg:grid">
              <span>Conta</span>
              <span>Perfil</span>
              <span>Status</span>
              <span>2FA</span>
              <span>Acoes</span>
            </div>

            <div className="divide-y divide-slate-100">
              {users.map((user) => (
                <article key={user.id} className="grid gap-3 px-5 py-4 lg:grid-cols-[1.4fr_1.15fr_0.8fr_0.9fr_0.8fr] lg:items-center">
                  <div>
                    <p className="font-semibold text-slate-900">{user.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{user.email}</p>
                    <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-400">
                      {user.totalTransactions} transacao(oes)
                      {user.lastTransactionDate ? ` • ultima em ${formatDate(user.lastTransactionDate)}` : ' • sem historico'}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <Tag tone={user.role === 'ADMIN' ? 'warning' : 'neutral'}>{user.role}</Tag>
                    <Tag tone={user.active ? 'positive' : 'negative'}>{user.active ? 'ATIVO' : 'SUSPENSO'}</Tag>
                  </div>
                  <div>
                    <span className={`text-sm font-semibold ${user.active ? 'text-emerald-700' : 'text-rose-700'}`}>
                      {user.active ? 'Liberado' : 'Bloqueado'}
                    </span>
                  </div>
                  <div>
                    <span className={`text-sm font-semibold ${user.twoFactorEnabled ? 'text-emerald-700' : 'text-slate-500'}`}>
                      {user.twoFactorEnabled ? 'Ativo' : 'Desligado'}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      className="rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                      onClick={() => {
                        setSelectedUser(user);
                        setNewPassword('');
                        setFeedbackMessage('');
                        setFeedbackError('');
                      }}
                      type="button"
                    >
                      Gerenciar
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </SectionCard>

        <SectionCard title={selectedUser ? `Comando da conta #${selectedUser.id}` : 'Selecione uma conta'}>
          {!selectedUser ? (
            <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 px-5 py-6 text-sm leading-7 text-slate-500">
              Escolha uma conta na lista para revisar perfil, alterar permissoes, redefinir senha ou resetar o autenticador.
            </div>
          ) : (
            <div className="space-y-5">
              <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <p className="font-semibold text-slate-900">{selectedUser.name}</p>
                <p className="mt-1 text-sm text-slate-500">{selectedUser.email}</p>
                <p className="mt-3 text-sm leading-7 text-slate-600">
                  Criada em {formatDateTime(selectedUser.createdAt)}
                  {selectedUser.suspendedAt ? ` • suspensa em ${formatDateTime(selectedUser.suspendedAt)}` : ''}
                </p>
              </div>

              <div className="grid gap-3">
                <ActionButton
                  label={selectedUser.active ? 'Suspender conta' : 'Reativar conta'}
                  tone={selectedUser.active ? 'danger' : 'default'}
                  onClick={() => {
                    const actionLabel = selectedUser.active ? 'suspender' : 'reativar';
                    const confirmation = window.prompt(`Digite o e-mail da conta para confirmar ${actionLabel}:`);
                    if (confirmation?.trim().toLowerCase() !== selectedUser.email.toLowerCase()) {
                      return;
                    }

                    setFeedbackMessage('');
                    setFeedbackError('');
                    updateStatusMutation.mutate(
                      { id: selectedUser.id, data: { active: !selectedUser.active } },
                      {
                        onSuccess: (response) => {
                          syncSelectedUser(response);
                          setFeedbackMessage(selectedUser.active ? 'Conta suspensa com sucesso.' : 'Conta reativada com sucesso.');
                        },
                        onError: (error) => {
                          setFeedbackError(getApiErrorMessage(error, 'Nao foi possivel atualizar o status da conta agora.'));
                        },
                      },
                    );
                  }}
                />

                <ActionButton
                  label={selectedUser.role === 'ADMIN' ? 'Transformar em usuario comum' : 'Promover para admin'}
                  tone="default"
                  onClick={() => {
                    const nextRole: Role = selectedUser.role === 'ADMIN' ? 'USER' : 'ADMIN';
                    const confirmation = window.prompt(`Digite o e-mail da conta para confirmar a role ${nextRole}:`);
                    if (confirmation?.trim().toLowerCase() !== selectedUser.email.toLowerCase()) {
                      return;
                    }

                    setFeedbackMessage('');
                    setFeedbackError('');
                    updateRoleMutation.mutate(
                      { id: selectedUser.id, data: { role: nextRole } },
                      {
                        onSuccess: (response) => {
                          syncSelectedUser(response);
                          setFeedbackMessage(`Permissao atualizada para ${nextRole}.`);
                        },
                        onError: (error) => {
                          setFeedbackError(getApiErrorMessage(error, 'Nao foi possivel atualizar a role agora.'));
                        },
                      },
                    );
                  }}
                />
              </div>

              <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <Field label="Nova senha temporaria">
                  <input
                    className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 outline-none transition focus:border-emerald-400"
                    type="text"
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                    placeholder="Ex.: Admin@1234"
                  />
                </Field>

                <button
                  className="mt-4 rounded-full bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                  disabled={resetPasswordMutation.isPending || !newPassword.trim()}
                  onClick={() => {
                    const confirmation = window.prompt('Digite o e-mail da conta para confirmar a redefinicao da senha:');
                    if (confirmation?.trim().toLowerCase() !== selectedUser.email.toLowerCase()) {
                      return;
                    }

                    setFeedbackMessage('');
                    setFeedbackError('');
                    resetPasswordMutation.mutate(
                      { id: selectedUser.id, data: { newPassword: newPassword.trim() } },
                      {
                        onSuccess: (response) => {
                          syncSelectedUser(response);
                          setNewPassword('');
                          setFeedbackMessage('Senha redefinida com sucesso.');
                        },
                        onError: (error) => {
                          setFeedbackError(getApiErrorMessage(error, 'Nao foi possivel redefinir a senha agora.'));
                        },
                      },
                    );
                  }}
                  type="button"
                >
                  {resetPasswordMutation.isPending ? 'Salvando...' : 'Redefinir senha'}
                </button>
              </div>

              <ActionButton
                label="Resetar autenticador (2FA)"
                tone="default"
                onClick={() => {
                  const confirmation = window.prompt('Digite o e-mail da conta para confirmar o reset do autenticador:');
                  if (confirmation?.trim().toLowerCase() !== selectedUser.email.toLowerCase()) {
                    return;
                  }

                  setFeedbackMessage('');
                  setFeedbackError('');
                  resetTwoFactorMutation.mutate(selectedUser.id, {
                    onSuccess: (response) => {
                      syncSelectedUser(response);
                      setFeedbackMessage('Segundo fator removido com sucesso.');
                    },
                    onError: (error) => {
                      setFeedbackError(getApiErrorMessage(error, 'Nao foi possivel resetar o autenticador agora.'));
                    },
                  });
                }}
              />
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
}

function MetricCard({ label, value, helper }: { label: string; value: string; helper: string }) {
  return (
    <article className="rounded-[24px] border border-slate-100 bg-slate-50 p-5">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-600">{label}</p>
      <p className="mt-4 text-3xl font-semibold text-slate-900">{value}</p>
      <p className="mt-2 text-sm leading-7 text-slate-500">{helper}</p>
    </article>
  );
}

function InfoPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{label}</p>
      <p className="mt-2 text-xl font-semibold text-slate-900">{value}</p>
    </div>
  );
}

function Tag({ children, tone }: { children: string; tone: 'positive' | 'negative' | 'warning' | 'neutral' }) {
  const className = {
    positive: 'bg-emerald-100 text-emerald-700',
    negative: 'bg-rose-100 text-rose-700',
    warning: 'bg-amber-100 text-amber-700',
    neutral: 'bg-slate-200 text-slate-700',
  }[tone];

  return <span className={`rounded-full px-3 py-1 text-xs font-semibold tracking-[0.14em] ${className}`}>{children}</span>;
}

function ActionButton({
  label,
  onClick,
  tone = 'default',
}: {
  label: string;
  onClick: () => void;
  tone?: 'default' | 'danger';
}) {
  const className =
    tone === 'danger'
      ? 'border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100'
      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50';

  return (
    <button
      className={`rounded-full border px-4 py-3 text-sm font-semibold transition ${className}`}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
  }).format(value);
}

function formatDate(value: string) {
  const [year, month, day] = value.split('-');
  return `${day}/${month}/${year}`;
}

function formatDateTime(value: string) {
  const [datePart] = value.split('T');
  return formatDate(datePart);
}
