import { useEffect, useMemo, useState } from 'react';
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
import { Field, InputConfirmationDialog, SectionCard } from '../../shared/ui';

type UserFilter = 'TODOS' | 'ATIVOS' | 'SUSPENSOS' | 'ADMINS' | 'DOIS_FATORES';
type PendingAdminAction =
  | { type: 'STATUS'; user: AdminUserResponse; active: boolean }
  | { type: 'ROLE'; user: AdminUserResponse; role: Role }
  | { type: 'PASSWORD'; user: AdminUserResponse; newPassword: string }
  | { type: 'TWO_FACTOR'; user: AdminUserResponse };

export default function AdminPage() {
  const overviewQuery = useAdminOverviewQuery(true);
  const usersQuery = useAdminUsersQuery(true);
  const updateStatusMutation = useAdminUpdateUserStatusMutation();
  const updateRoleMutation = useAdminUpdateUserRoleMutation();
  const resetPasswordMutation = useAdminResetUserPasswordMutation();
  const resetTwoFactorMutation = useAdminResetUserTwoFactorMutation();

  const [selectedUser, setSelectedUser] = useState<AdminUserResponse | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [userFilter, setUserFilter] = useState<UserFilter>('TODOS');
  const [feedbackMessage, setFeedbackMessage] = useState('');
  const [feedbackError, setFeedbackError] = useState('');
  const [pendingAdminAction, setPendingAdminAction] = useState<PendingAdminAction | null>(null);
  const [adminConfirmationEmail, setAdminConfirmationEmail] = useState('');
  const [adminConfirmationError, setAdminConfirmationError] = useState('');

  const users = usersQuery.data ?? [];
  const overview = overviewQuery.data;

  const activeUsers = useMemo(() => users.filter((user) => user.active), [users]);
  const suspendedUsers = useMemo(() => users.filter((user) => !user.active), [users]);
  const twoFactorUsers = useMemo(() => users.filter((user) => user.twoFactorEnabled), [users]);
  const adminUsers = useMemo(() => users.filter((user) => user.role === 'ADMIN'), [users]);

  const filteredUsers = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    return users.filter((user) => {
      const matchesSearch =
        normalizedSearch.length === 0
        || user.name.toLowerCase().includes(normalizedSearch)
        || user.email.toLowerCase().includes(normalizedSearch);

      if (!matchesSearch) {
        return false;
      }

      switch (userFilter) {
        case 'ATIVOS':
          return user.active;
        case 'SUSPENSOS':
          return !user.active;
        case 'ADMINS':
          return user.role === 'ADMIN';
        case 'DOIS_FATORES':
          return user.twoFactorEnabled;
        default:
          return true;
      }
    });
  }, [searchTerm, userFilter, users]);

  useEffect(() => {
    if (filteredUsers.length === 0) {
      setSelectedUser(null);
      return;
    }

    setSelectedUser((currentValue) => {
      if (!currentValue) {
        return filteredUsers[0];
      }

      const updatedSelection = filteredUsers.find((user) => user.id === currentValue.id);
      return updatedSelection ?? filteredUsers[0];
    });
  }, [filteredUsers]);

  const syncSelectedUser = (updatedUser: AdminUserResponse) => {
    setSelectedUser((currentValue) => (currentValue?.id === updatedUser.id ? updatedUser : currentValue));
  };

  const nextRole = selectedUser?.role === 'ADMIN' ? 'USER' : 'ADMIN';
  const canPromoteSelectedUser = selectedUser ? selectedUser.adminPromotionAllowed : false;
  const statusActionDisabled = !!selectedUser?.protectedAdmin && !!selectedUser?.active;
  const roleActionDisabled =
    !selectedUser
    || (!!selectedUser.protectedAdmin && selectedUser.role === 'ADMIN' && nextRole === 'USER')
    || (nextRole === 'ADMIN' && !canPromoteSelectedUser);
  const actionPending =
    updateStatusMutation.isPending
    || updateRoleMutation.isPending
    || resetPasswordMutation.isPending
    || resetTwoFactorMutation.isPending;
  const adminQueryPending = overviewQuery.isPending || usersQuery.isPending;
  const adminQueryError = overviewQuery.error ?? usersQuery.error;

  const selectUser = (user: AdminUserResponse) => {
    setSelectedUser(user);
    setNewPassword('');
    setFeedbackMessage('');
    setFeedbackError('');
  };

  const openAdminConfirmation = (action: PendingAdminAction) => {
    setAdminConfirmationEmail('');
    setAdminConfirmationError('');
    setPendingAdminAction(action);
  };

  const closeAdminConfirmation = () => {
    setPendingAdminAction(null);
    setAdminConfirmationEmail('');
    setAdminConfirmationError('');
  };

  const confirmAdminAction = () => {
    const action = pendingAdminAction;
    if (!action || adminConfirmationEmail.trim().toLowerCase() !== action.user.email.toLowerCase()) return;
    setFeedbackMessage('');
    setFeedbackError('');
    setAdminConfirmationError('');

    if (action.type === 'STATUS') {
      updateStatusMutation.mutate(
        { id: action.user.id, data: { active: action.active } },
        {
          onSuccess: (response) => {
            syncSelectedUser(response);
            setFeedbackMessage(action.active ? 'Conta reativada com sucesso.' : 'Conta suspensa com sucesso.');
            closeAdminConfirmation();
          },
          onError: (error) => setAdminConfirmationError(getApiErrorMessage(error, 'Não foi possível atualizar o status da conta agora.')),
        },
      );
      return;
    }

    if (action.type === 'ROLE') {
      updateRoleMutation.mutate(
        { id: action.user.id, data: { role: action.role } },
        {
          onSuccess: (response) => {
            syncSelectedUser(response);
            setFeedbackMessage(`Permissão atualizada para ${action.role}.`);
            closeAdminConfirmation();
          },
          onError: (error) => setAdminConfirmationError(getApiErrorMessage(error, 'Não foi possível atualizar o perfil agora.')),
        },
      );
      return;
    }

    if (action.type === 'PASSWORD') {
      resetPasswordMutation.mutate(
        { id: action.user.id, data: { newPassword: action.newPassword } },
        {
          onSuccess: (response) => {
            syncSelectedUser(response);
            setNewPassword('');
            setFeedbackMessage('Senha redefinida com sucesso.');
            closeAdminConfirmation();
          },
          onError: (error) => setAdminConfirmationError(getApiErrorMessage(error, 'Não foi possível redefinir a senha agora.')),
        },
      );
      return;
    }

    resetTwoFactorMutation.mutate(action.user.id, {
      onSuccess: (response) => {
        syncSelectedUser(response);
        setFeedbackMessage('Segundo fator removido com sucesso.');
        closeAdminConfirmation();
      },
      onError: (error) => setAdminConfirmationError(getApiErrorMessage(error, 'Não foi possível resetar o autenticador agora.')),
    });
  };

  const adminActionContent = getAdminActionContent(pendingAdminAction);

  return (
    <div className="space-y-6">
      {adminQueryPending && (
        <div className="rounded-[22px] border border-sky-100 bg-sky-50 px-5 py-4 text-sm leading-7 text-sky-800">
          Carregando contas e indicadores administrativos...
        </div>
      )}

      {adminQueryError && (
        <div className="flex flex-col gap-4 rounded-[22px] border border-rose-100 bg-rose-50 px-5 py-4 text-sm leading-7 text-rose-800 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="font-semibold">Não foi possível carregar o painel administrativo.</p>
            <p>{getApiErrorMessage(adminQueryError, 'A API recusou ou não concluiu a consulta administrativa.')}</p>
          </div>
          <button
            className="shrink-0 rounded-full bg-rose-700 px-4 py-2 font-semibold text-white transition hover:bg-rose-800"
            onClick={() => {
              overviewQuery.refetch();
              usersQuery.refetch();
            }}
            type="button"
          >
            Tentar novamente
          </button>
        </div>
      )}

      <SectionCard title="Sala de comando do admin">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <MetricCard
            label="Usuários no produto"
            value={String(overview?.totalUsuarios ?? users.length)}
            helper="Base total cadastrada."
          />
          <MetricCard
            label="Usuários ativos"
            value={String(overview?.usuariosAtivos ?? activeUsers.length)}
            helper="Contas que conseguem operar agora."
          />
          <MetricCard
            label="Administradores"
            value={String(overview?.administradores ?? adminUsers.length)}
            helper="Perfis com acesso total."
          />
          <MetricCard
            label="Com 2FA ativo"
            value={String(overview?.usuariosComDoisFatores ?? twoFactorUsers.length)}
            helper="Contas protegidas com autenticador."
          />
          <MetricCard
            label="Whitelist admin"
            value={String(overview?.emailsPermitidosParaAdmin ?? 0)}
            helper="E-mails autorizados para receber role ADMIN."
          />
          <MetricCard
            label="Status da API"
            value={overview?.statusApi === 'SAUDAVEL' ? 'Saudável' : overview?.statusApi ?? 'Indefinido'}
            helper="Leitura operacional para confirmar que o painel está estável."
          />
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <MetricCard
            label="Receitas totais"
            value={formatCurrency(overview?.totalReceitas ?? 0)}
            helper="Volume agregado de entradas."
          />
          <MetricCard
            label="Despesas totais"
            value={formatCurrency(overview?.totalDespesas ?? 0)}
            helper="Volume agregado de saídas."
          />
          <MetricCard
            label="Saldo global"
            value={formatCurrency(overview?.saldoGlobal ?? 0)}
            helper="Diferença consolidada entre entradas e saídas."
          />
        </div>

        <div className={`mt-4 rounded-[24px] border px-5 py-4 text-sm leading-7 ${
          overview?.whitelistConfigured
            ? 'border-emerald-100 bg-emerald-50/70 text-emerald-800'
            : 'border-amber-200 bg-amber-50 text-amber-900'
        }`}>
          <p className="font-semibold">Regra sensível de produção</p>
          {overview?.whitelistConfigured ? (
            <p className="mt-2">
              Só e-mails presentes em <span className="font-semibold">APP_ADMIN_ALLOWED_EMAILS</span> ou no e-mail de bootstrap
              podem receber ou manter acesso administrativo. A decisão final permanece protegida pelo backend.
            </p>
          ) : (
            <p className="mt-2">
              Modo de recuperação ativo: apenas contas que já possuem perfil ADMIN no banco podem abrir este painel. Novas
              promoções continuam bloqueadas. Configure <span className="font-semibold">APP_ADMIN_ALLOWED_EMAILS</span> no Render
              para voltar ao modo normal.
            </p>
          )}
          <div className="mt-4 flex flex-wrap gap-2">
            {(overview?.adminWhitelist ?? []).length > 0 ? (
              (overview?.adminWhitelist ?? []).map((email) => (
                <Tag key={email} tone="positive">
                  {email}
                </Tag>
              ))
            ) : (
              <Tag tone="warning">Modo de recuperação</Tag>
            )}
          </div>
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

      <div className="space-y-6">
        <SectionCard title="Gestão de contas">
          <div className="mb-4 grid gap-3 md:grid-cols-3">
            <InfoPill label="Ativos" value={String(activeUsers.length)} />
            <InfoPill label="Suspensos" value={String(suspendedUsers.length)} />
            <InfoPill label="Com 2FA" value={String(twoFactorUsers.length)} />
          </div>

          <div className="mb-4 grid gap-3 lg:grid-cols-[minmax(0,1fr)_180px]">
            <Field label="Buscar conta">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                type="text"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Nome ou e-mail"
              />
            </Field>

            <Field label="Filtro">
              <select
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                value={userFilter}
                onChange={(event) => setUserFilter(event.target.value as UserFilter)}
              >
                <option value="TODOS">Todos</option>
                <option value="ATIVOS">Ativos</option>
                <option value="SUSPENSOS">Suspensos</option>
                <option value="ADMINS">Admins</option>
                <option value="DOIS_FATORES">Com 2FA</option>
              </select>
            </Field>
          </div>

          <div className="overflow-x-auto rounded-[24px] border border-slate-100">
            <div className="min-w-[980px]">
              <div className="grid grid-cols-[2.2fr_1.25fr_1fr_1.05fr_1fr_120px] gap-4 bg-slate-50 px-5 py-4 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                <span>Conta</span>
                <span>Perfil</span>
                <span>Status</span>
                <span>Proteção</span>
                <span>Movimento</span>
                <span>Ação</span>
              </div>

              <div className="divide-y divide-slate-100">
                {filteredUsers.length === 0 && (
                  <div className="px-5 py-8 text-sm leading-7 text-slate-500">
                    Nenhuma conta combinou com a busca atual. Tente outro nome, e-mail ou filtro.
                  </div>
                )}

                {filteredUsers.map((user) => {
                  const isSelected = selectedUser?.id === user.id;

                  return (
                    <button
                      key={user.id}
                      className={`grid w-full grid-cols-[2.2fr_1.25fr_1fr_1.05fr_1fr_120px] gap-4 px-5 py-4 text-left transition ${
                        isSelected ? 'bg-emerald-50/70' : 'bg-white hover:bg-slate-50'
                      }`}
                      onClick={() => selectUser(user)}
                      type="button"
                    >
                      <div>
                        <p className="font-semibold text-slate-900">{user.name}</p>
                        <p className="mt-1 text-sm text-slate-500">{user.email}</p>
                        <div className="mt-3 flex flex-wrap gap-2">
                          {user.currentSessionUser && <Tag tone="neutral">Sessão atual</Tag>}
                          {user.protectedAdmin && <Tag tone="warning">Admin protegido</Tag>}
                          {!user.adminPromotionAllowed && user.role !== 'ADMIN' && <Tag tone="negative">Fora da whitelist</Tag>}
                        </div>
                      </div>

                      <div className="flex flex-wrap content-start items-center gap-2">
                        <Tag tone={user.role === 'ADMIN' ? 'warning' : 'neutral'}>{user.role}</Tag>
                      </div>

                      <div>
                        <span className={`text-sm font-semibold ${user.active ? 'text-emerald-700' : 'text-rose-700'}`}>
                          {user.active ? 'Liberado' : 'Bloqueado'}
                        </span>
                        <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-400">
                          {user.active ? 'Conta ativa' : 'Conta suspensa'}
                        </p>
                      </div>

                      <div>
                        <span className={`text-sm font-semibold ${user.twoFactorEnabled ? 'text-emerald-700' : 'text-slate-500'}`}>
                          {user.twoFactorEnabled ? '2FA ativo' : 'Sem 2FA'}
                        </span>
                        <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-400">
                          {user.protectedAdmin ? 'Whitelist protegida' : 'Fluxo padrão'}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-slate-900">{formatTransactionCount(user.totalTransactions)}</p>
                        <p className="mt-2 text-xs uppercase tracking-[0.14em] text-slate-400">
                          {user.lastTransactionDate ? `Última em ${formatDate(user.lastTransactionDate)}` : 'Sem histórico'}
                        </p>
                      </div>

                      <div className="flex items-start justify-end">
                        <span
                          className={`rounded-full px-3 py-2 text-xs font-semibold ${
                            isSelected ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700'
                          }`}
                        >
                          {isSelected ? 'Selecionada' : 'Selecionar'}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </SectionCard>

        <SectionCard title={selectedUser ? `Comando da conta #${selectedUser.id}` : 'Selecione uma conta'}>
          {!selectedUser ? (
            <div className="rounded-[22px] border border-dashed border-slate-200 bg-slate-50 px-5 py-6 text-sm leading-7 text-slate-500">
              Escolha uma conta na lista para revisar o perfil, alterar permissões, redefinir a senha ou resetar o autenticador.
            </div>
          ) : (
            <div className="space-y-5">
              <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900">{selectedUser.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{selectedUser.email}</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Tag tone={selectedUser.role === 'ADMIN' ? 'warning' : 'neutral'}>{selectedUser.role}</Tag>
                    <Tag tone={selectedUser.active ? 'positive' : 'negative'}>
                      {selectedUser.active ? 'Ativo' : 'Suspenso'}
                    </Tag>
                  </div>
                </div>

                <p className="mt-3 text-sm leading-7 text-slate-600">
                  Criada em {formatDateTime(selectedUser.createdAt)}
                  {selectedUser.suspendedAt ? ` • suspensa em ${formatDateTime(selectedUser.suspendedAt)}` : ''}
                </p>

                <div className="mt-3 flex flex-wrap gap-2">
                  {selectedUser.currentSessionUser && <Tag tone="neutral">Você está aqui</Tag>}
                  {selectedUser.protectedAdmin && <Tag tone="warning">E-mail admin autorizado</Tag>}
                  {!selectedUser.adminPromotionAllowed && selectedUser.role !== 'ADMIN' && (
                    <Tag tone="negative">Promoção bloqueada</Tag>
                  )}
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <MiniStat label="Histórico total" value={formatTransactionCount(selectedUser.totalTransactions)} />
                <MiniStat
                  label="Última atividade"
                  value={selectedUser.lastTransactionDate ? formatDate(selectedUser.lastTransactionDate) : 'Sem histórico'}
                />
              </div>

              <div className="rounded-[22px] border border-slate-100 bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-600">
                {selectedUser.protectedAdmin
                  ? 'Esta conta está dentro da whitelist administrativa e permanece protegida contra suspensão ou rebaixamento por este fluxo.'
                  : 'Esta conta só pode virar admin se o e-mail for adicionado explicitamente à whitelist de produção.'}
              </div>

              <div className="grid gap-3">
                <ActionButton
                  label={selectedUser.active ? 'Suspender conta' : 'Reativar conta'}
                  tone={selectedUser.active ? 'danger' : 'default'}
                  disabled={statusActionDisabled || actionPending}
                  onClick={() => {
                    openAdminConfirmation({ type: 'STATUS', user: selectedUser, active: !selectedUser.active });
                  }}
                />

                {statusActionDisabled && (
                  <p className="text-sm leading-7 text-amber-700">
                    Suspensão indisponível: esta conta está protegida pela whitelist administrativa.
                  </p>
                )}

                <ActionButton
                  label={selectedUser.role === 'ADMIN' ? 'Transformar em usuário comum' : 'Promover para admin'}
                  tone="default"
                  disabled={roleActionDisabled || actionPending}
                  onClick={() => {
                    const targetRole: Role = selectedUser.role === 'ADMIN' ? 'USER' : 'ADMIN';
                    openAdminConfirmation({ type: 'ROLE', user: selectedUser, role: targetRole });
                  }}
                />

                {roleActionDisabled && nextRole === 'ADMIN' && (
                  <p className="text-sm leading-7 text-amber-700">
                    Promoção indisponível: o e-mail desta conta ainda não está na whitelist de admins autorizados.
                  </p>
                )}

                {selectedUser.protectedAdmin && selectedUser.role === 'ADMIN' && nextRole === 'USER' && (
                  <p className="text-sm leading-7 text-amber-700">
                    Rebaixamento indisponível: esta conta é uma administradora protegida pela whitelist.
                  </p>
                )}
              </div>

              <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
                <Field label="Nova senha temporária">
                  <input
                    autoComplete="new-password"
                    className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 outline-none transition focus:border-emerald-400"
                    type="text"
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                    placeholder="Ex.: Admin@1234"
                  />
                </Field>

                <button
                  className="mt-4 rounded-full bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                  disabled={resetPasswordMutation.isPending || actionPending || !newPassword.trim()}
                  onClick={() => {
                    openAdminConfirmation({ type: 'PASSWORD', user: selectedUser, newPassword: newPassword.trim() });
                  }}
                  type="button"
                >
                  {resetPasswordMutation.isPending ? 'Salvando...' : 'Redefinir senha'}
                </button>
              </div>

              <ActionButton
                label="Resetar autenticador (2FA)"
                tone="default"
                disabled={actionPending}
                onClick={() => {
                  openAdminConfirmation({ type: 'TWO_FACTOR', user: selectedUser });
                }}
              />
            </div>
          )}
        </SectionCard>
      </div>
      <InputConfirmationDialog
        open={pendingAdminAction != null}
        title={adminActionContent.title}
        description={adminActionContent.description}
        label="E-mail da conta"
        value={adminConfirmationEmail}
        confirmLabel={adminActionContent.confirmLabel}
        placeholder={pendingAdminAction?.user.email}
        helper="Digite exatamente o e-mail exibido para liberar esta ação administrativa."
        inputMode="email"
        busy={actionPending}
        error={adminConfirmationError}
        confirmDisabled={!pendingAdminAction || adminConfirmationEmail.trim().toLowerCase() !== pendingAdminAction.user.email.toLowerCase()}
        onValueChange={(value) => { setAdminConfirmationEmail(value); setAdminConfirmationError(''); }}
        onClose={closeAdminConfirmation}
        onConfirm={confirmAdminAction}
      />
    </div>
  );
}

function getAdminActionContent(action: PendingAdminAction | null) {
  if (!action) return { title: 'Confirmar ação administrativa', description: '', confirmLabel: 'Confirmar' };
  if (action.type === 'STATUS') {
    return action.active
      ? { title: 'Reativar conta?', description: `A conta de ${action.user.email} voltará a acessar o Farol.`, confirmLabel: 'Reativar conta' }
      : { title: 'Suspender conta?', description: `A conta de ${action.user.email} perderá o acesso até ser reativada por um administrador.`, confirmLabel: 'Suspender conta' };
  }
  if (action.type === 'ROLE') {
    return action.role === 'ADMIN'
      ? { title: 'Promover para admin?', description: `${action.user.email} receberá acesso às funções administrativas permitidas pelo backend.`, confirmLabel: 'Promover para admin' }
      : { title: 'Remover acesso administrativo?', description: `${action.user.email} continuará como usuário comum, sem acesso ao painel administrativo.`, confirmLabel: 'Alterar para usuário' };
  }
  if (action.type === 'PASSWORD') {
    return { title: 'Redefinir senha?', description: `A senha de ${action.user.email} será substituída pela senha temporária informada.`, confirmLabel: 'Redefinir senha' };
  }
  return { title: 'Resetar autenticador?', description: `O segundo fator de ${action.user.email} será removido e deverá ser configurado novamente.`, confirmLabel: 'Resetar autenticador' };
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

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{label}</p>
      <p className="mt-2 text-base font-semibold text-slate-900">{value}</p>
    </div>
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
  disabled = false,
}: {
  label: string;
  onClick: () => void;
  tone?: 'default' | 'danger';
  disabled?: boolean;
}) {
  const className =
    tone === 'danger'
      ? 'border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100'
      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50';

  return (
    <button
      className={`rounded-full border px-4 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
      disabled={disabled}
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

function formatTransactionCount(value: number) {
  return `${value} ${value === 1 ? 'transação' : 'transações'}`;
}

function formatDate(value: string) {
  const [year, month, day] = value.split('-');
  return `${day}/${month}/${year}`;
}

function formatDateTime(value: string) {
  const [datePart] = value.split('T');
  return formatDate(datePart);
}
