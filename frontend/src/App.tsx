import { Dispatch, ReactNode, SetStateAction, useEffect, useMemo, useState } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import CookieConsent from './components/CookieConsent';
import {
  calculateFinalPrice,
  categoryLabels,
  Category,
  formatCurrency,
  formatMonthLabel,
  getSuggestedCategory,
  paymentMethodLabels,
  PaymentMethod,
  priorityLabels,
} from './lib/mockFinance';
import {
  useCreateTransactionMutation,
  useCreateWishlistItemMutation,
  useDashboardQuery,
  useLoginMutation,
  useMonthlyAnalysisQuery,
  usePurchaseWishlistItemMutation,
  useRegisterMutation,
  useTransactionsQuery,
  useUndoWishlistPurchaseMutation,
  useWishlistHistoryQuery,
  useWishlistItemsQuery,
  useWishlistListsQuery,
  useWishlistSummaryQuery,
} from './lib/queries';
import {
  AuthUser,
  DashboardResponse,
  MonthlyAnalysisResponse,
  TransactionResponse,
  WishlistHistoryResponse,
  WishlistItemResponse,
  WishlistListResponse,
  WishlistPriority,
} from './types';
import { useAuthStore } from './store/auth';

type ViewId = 'painel' | 'transacoes' | 'analise' | 'wishlist' | 'configuracoes';

type TransactionDraft = {
  type: 'RECEITA' | 'DESPESA';
  description: string;
  amount: string;
  paymentMethod: PaymentMethod;
  transactionDate: string;
  category: Category;
  notes: string;
};

type WishlistDraft = {
  description: string;
  notes: string;
  originalPrice: string;
  discountPercent: string;
  priority: WishlistPriority;
  category: Category;
  listId: string;
};

type PurchaseDraft = {
  purchaseDate: string;
  paymentMethod: PaymentMethod;
  installments: number;
  firstInstallmentNextMonth: boolean;
};

type ToastTone = 'success' | 'info';

type ToastMessage = {
  id: number;
  message: string;
  tone: ToastTone;
};

const onboardingKey = 'cg-demo-onboarding-dismissed';

const navItems: Array<{ id: ViewId; label: string; description: string }> = [
  { id: 'painel', label: 'Painel', description: 'Resumo do período e acumulado' },
  { id: 'transacoes', label: 'Transações', description: 'Entradas, saídas e histórico' },
  { id: 'analise', label: 'Análise mensal', description: 'Comparativos e tendência' },
  { id: 'wishlist', label: 'Lista de desejos', description: 'Desejos, compras e histórico' },
  { id: 'configuracoes', label: 'Configurações', description: 'Conta, segurança e privacidade' },
];

const today = new Date();
const currentYear = today.getFullYear();
const currentMonth = today.getMonth() + 1;

const monthOptions = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: new Date(2026, index, 1).toLocaleDateString('pt-BR', { month: 'long' }),
}));

const yearOptions = Array.from({ length: 5 }, (_, index) => currentYear - 2 + index);

function App() {
  const { isAuthenticated, hydrated, hydrate, logout } = useAuthStore();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  if (!hydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4">
        <div className="rounded-[28px] border border-emerald-100 bg-white px-6 py-5 text-sm font-semibold text-slate-600 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
          Preparando seu ambiente com segurança...
        </div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage isLoggedIn={isAuthenticated} />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route path="/esqueci-a-senha" element={<ForgotPasswordPage />} />
        <Route
          path="/app"
          element={
            isAuthenticated ? (
              <Workspace onLogout={logout} />
            ) : (
              <Navigate replace to="/login" />
            )
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <CookieConsent />
    </BrowserRouter>
  );
}

function LandingPage({ isLoggedIn }: { isLoggedIn: boolean }) {
  return (
    <div className="min-h-screen bg-[#f4f6f1] text-slate-900">
      <header className="border-b border-emerald-100 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 lg:px-8">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-lg font-bold text-white shadow-lg shadow-emerald-500/20">
              CG
            </div>
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Controle de Gastos</p>
              <h1 className="text-xl font-semibold text-slate-900">Seu dinheiro com mais clareza e menos ansiedade.</h1>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link
              className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
              to="/login"
            >
              Entrar
            </Link>
            <Link
              className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
              to={isLoggedIn ? '/app' : '/cadastro'}
            >
              {isLoggedIn ? 'Ir para o app' : 'Começar agora'}
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section className="mx-auto grid max-w-7xl gap-10 px-4 py-16 lg:grid-cols-[1.1fr_0.9fr] lg:px-8">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Produto em construção séria</p>
            <h2 className="mt-4 text-5xl font-semibold leading-tight text-balance text-slate-900">
              Um espaço para entender o mês, planejar compras e decidir melhor.
            </h2>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
              O Controle de Gastos nasceu de um problema real e está sendo pensado em fases: primeiro clareza financeira, depois profundidade, segurança e experiência de SaaS.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                className="rounded-full bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                to={isLoggedIn ? '/app' : '/cadastro'}
              >
                {isLoggedIn ? 'Abrir meu ambiente' : 'Criar conta de demonstração'}
              </Link>
              <Link
                className="rounded-full border border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                to="/login"
              >
                Ver área logada
              </Link>
            </div>

            <div className="mt-10 grid gap-4 sm:grid-cols-3">
              <FeatureChip label="Painel objetivo" helper="Entradas, saídas e acumulado." />
              <FeatureChip label="Análise mensal" helper="Comparação com meses e anos." />
              <FeatureChip label="Lista de desejos" helper="Compra planejada ligada ao financeiro." />
            </div>
          </div>

          <div className="rounded-[36px] bg-slate-900 p-6 text-white shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">Visão do produto</p>
            <div className="mt-5 grid gap-4">
              <LandingStat title="Receitas do mês" value={formatCurrency(4200)} />
              <LandingStat title="Despesas do mês" value={formatCurrency(1462)} tone="negative" />
              <LandingStat title="Resultado do mês" value={formatCurrency(2738)} />
              <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
                <p className="text-sm font-semibold text-white">O que o sistema já faz hoje</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
                  <li>Mostra o mês atual e o acumulado até o período escolhido.</li>
                  <li>Compara mês anterior, mesmo mês do ano passado e acumulado anual.</li>
                  <li>Planeja desejos e envia compras para o financeiro.</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="bg-white/70 py-16">
          <div className="mx-auto max-w-7xl px-4 lg:px-8">
            <div className="max-w-3xl">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Por que isso importa</p>
              <h3 className="mt-3 text-3xl font-semibold text-slate-900">O produto precisa ajudar, não só parecer um sistema bonito.</h3>
              <p className="mt-4 text-base leading-8 text-slate-600">
                A proposta aqui é simples: facilitar a leitura do mês, evitar sustos com o dinheiro e transformar vontade de compra em decisão consciente.
              </p>
            </div>

            <div className="mt-10 grid gap-5 lg:grid-cols-3">
              <LandingCard
                title="Painel"
                body="Mostra o que entrou, o que saiu, quanto sobrou e as últimas movimentações sem afogar o usuário em ruído."
              />
              <LandingCard
                title="Análise mensal"
                body="Traz contexto histórico para o usuário descobrir se está melhorando, piorando ou só repetindo padrões."
              />
              <LandingCard
                title="Lista de desejos"
                body="Ajuda a decidir compras com prioridade, desconto, histórico e impacto financeiro no momento em que o item é comprado."
              />
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-7xl px-4 py-16 lg:px-8">
          <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
            <div className="rounded-[32px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Camadas de SaaS que já estão no radar</p>
              <ul className="mt-5 grid gap-3 text-sm leading-7 text-slate-600">
                <li>Cadastro com força de senha e mensagens claras.</li>
                <li>Fluxo de recuperação de senha.</li>
                <li>Cookies com escolha simples entre essenciais e opcionais.</li>
                <li>Base para anti-bot em cadastro, recuperação e comportamentos suspeitos.</li>
                <li>Espaço para privacidade, termos e confiança do usuário.</li>
              </ul>
            </div>

            <div className="rounded-[32px] bg-emerald-500 px-7 py-8 text-white shadow-[0_24px_70px_rgba(16,185,129,0.18)]">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-100">Próximo passo de quem está conhecendo o projeto</p>
              <h3 className="mt-4 text-3xl font-semibold text-balance">Entrar no app, sentir os fluxos e descobrir o que ainda precisa nascer.</h3>
              <p className="mt-5 max-w-2xl text-base leading-8 text-emerald-50">
                Você ainda vai refinar muita coisa, e isso é ótimo. A função desta fase não é ser perfeita. É te dar um terreno visual firme para pensar com mais profundidade.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Link
                  className="rounded-full bg-white px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-50"
                  to={isLoggedIn ? '/app' : '/login'}
                >
                  Abrir área logada
                </Link>
                <Link
                  className="rounded-full border border-white/30 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
                  to="/cadastro"
                >
                  Ver cadastro
                </Link>
              </div>
            </div>
          </div>
        </section>

        <section className="bg-slate-950 py-16 text-white">
          <div className="mx-auto max-w-7xl px-4 lg:px-8">
            <div className="max-w-3xl">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">Como funciona na prática</p>
              <h3 className="mt-3 text-3xl font-semibold text-white">O fluxo principal já está claro antes mesmo da integração real.</h3>
              <p className="mt-4 text-base leading-8 text-slate-300">
                Primeiro o usuário entende o mês, depois compara períodos e por fim conecta desejos ao financeiro quando realmente compra algo.
              </p>
            </div>

            <div className="mt-10 grid gap-5 lg:grid-cols-3">
              <LandingCard
                title="1. Ler o momento"
                body="O Painel mostra receitas, despesas, resultado do mês e acumulado até o período de referência."
              />
              <LandingCard
                title="2. Comparar com contexto"
                body="A Análise Mensal responde se o mês foi melhor, pior ou igual ao mês anterior e ao mesmo mês do ano passado."
              />
              <LandingCard
                title="3. Comprar com intenção"
                body="A Lista de Desejos deixa o usuário planejar prioridade, desconto e impacto financeiro antes da compra acontecer."
              />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [email, setEmail] = useState('jorge@email.com');
  const [password, setPassword] = useState('Senha@123');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const loginMutation = useLoginMutation();

  return (
    <AuthLayout
      eyebrow="Entrar"
      title="Acesse sua conta"
      description="Esta etapa continua simples de propósito: clareza, foco e uma entrada sem distração para você testar o produto."
      sideTitle="Login limpo e honesto"
      sideText="Aqui a ideia é reproduzir a sensação de um acesso real sem depender do backend ainda."
    >
      <Field label="E-mail">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          placeholder="jorge@email.com"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </Field>

      <Field label="Senha">
        <div className="flex gap-3">
          <input
            className="h-12 flex-1 rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Senha@123"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
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

      <div className="rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
        <p className="font-semibold text-slate-900">Microtexto de apoio</p>
        <p className="mt-2">No produto real, essa tela vai continuar enxuta. O suporte maior ficará no cadastro e na recuperação de senha.</p>
      </div>

      {errorMessage && (
        <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
          {errorMessage}
        </div>
      )}

      <button
        className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
        disabled={loginMutation.isPending || !email || !password}
        onClick={() => {
          setErrorMessage('');
          loginMutation.mutate(
            { email, password },
            {
              onSuccess: (response) => {
                login(response);
                navigate('/app');
              },
              onError: () => {
                setErrorMessage('Não foi possível entrar. Confira o e-mail, a senha ou o estado do backend.');
              },
            },
          );
        }}
        type="button"
      >
        {loginMutation.isPending ? 'Entrando...' : 'Entrar no app'}
      </button>

      <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
        <Link className="font-semibold text-slate-600 transition hover:text-emerald-600" to="/esqueci-a-senha">
          Esqueci minha senha
        </Link>
        <Link className="font-semibold text-emerald-600 transition hover:text-emerald-700" to="/cadastro">
          Ainda não tenho conta
        </Link>
      </div>
    </AuthLayout>
  );
}

function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [name, setName] = useState('Jorge Corral');
  const [email, setEmail] = useState('jorge@email.com');
  const [confirmEmail, setConfirmEmail] = useState('jorge@email.com');
  const [password, setPassword] = useState('Senha@123');
  const [confirmPassword, setConfirmPassword] = useState('Senha@123');
  const [errorMessage, setErrorMessage] = useState('');
  const registerMutation = useRegisterMutation();

  const passwordChecks = [
    { label: 'Pelo menos 8 caracteres', valid: password.length >= 8 },
    { label: 'Pelo menos 1 número', valid: /\d/.test(password) },
    { label: 'Pelo menos 1 caractere especial', valid: /[^A-Za-z0-9]/.test(password) },
    { label: 'Pelo menos 1 letra maiúscula', valid: /[A-Z]/.test(password) },
  ];

  const passwordScore = passwordChecks.filter((rule) => rule.valid).length;
  const passwordStrength =
    passwordScore <= 1 ? 'Fraca' : passwordScore <= 3 ? 'Média' : 'Forte';

  return (
    <AuthLayout
      eyebrow="Cadastro"
      title="Crie sua conta"
      description="A ideia aqui é ser claro, ajudar quem está começando e não deixar o usuário se perder no meio do caminho."
      sideTitle="Cadastro com segurança visível"
      sideText="Seu produto vai ganhar muito quando o usuário sentir que está sendo guiado, não testado."
    >
      <div className="grid gap-4 md:grid-cols-2">
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
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>
        <Field label="Confirmar e-mail">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            value={confirmEmail}
            onChange={(event) => setConfirmEmail(event.target.value)}
          />
        </Field>
        <Field label="Senha">
          <input
            className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>
      </div>

      <Field label="Confirmar senha">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
        />
      </Field>

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

      <div className="rounded-[24px] border border-amber-100 bg-amber-50 p-4 text-sm leading-7 text-slate-600">
        <p className="font-semibold text-slate-900">Base para anti-bot</p>
        <p className="mt-2">
          Aqui vai entrar um fluxo silencioso para verificar comportamento suspeito sem atrapalhar o cadastro de quem está agindo normalmente.
        </p>
      </div>

      {errorMessage && (
        <div className="rounded-[22px] border border-rose-100 bg-rose-50 px-4 py-3 text-sm leading-7 text-rose-700">
          {errorMessage}
        </div>
      )}

      <button
        className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
        disabled={!name || email !== confirmEmail || password !== confirmPassword || registerMutation.isPending}
        onClick={() => {
          setErrorMessage('');
          registerMutation.mutate(
            { name, email, password },
            {
              onSuccess: (response) => {
                login(response);
                navigate('/app');
              },
              onError: () => {
                setErrorMessage('Não foi possível concluir o cadastro agora. Revise os dados ou tente novamente.');
              },
            },
          );
        }}
        type="button"
      >
        {registerMutation.isPending ? 'Criando conta...' : 'Criar conta e entrar'}
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

function ForgotPasswordPage() {
  const [email, setEmail] = useState('jorge@email.com');

  return (
    <AuthLayout
      eyebrow="Recuperação"
      title="Esqueci minha senha"
      description="Mesmo em modo demonstração, essa tela já existe porque ela é parte da sensação de SaaS real e confiável."
      sideTitle="Mensagem clara e neutra"
      sideText="No fluxo real, o sistema não deve confirmar se o e-mail existe. Ele só orienta o próximo passo com segurança."
    >
      <Field label="E-mail da conta">
        <input
          className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </Field>

      <div className="rounded-[24px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
        <p className="font-semibold text-slate-900">Como o produto deve se comportar</p>
        <p className="mt-2">
          “Se existir uma conta vinculada a este e-mail, enviaremos as orientações para redefinição de senha.”
        </p>
      </div>

      <button className="rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800" type="button">
        Enviar instruções
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

function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4">
      <div className="w-full max-w-xl rounded-[32px] border border-emerald-100 bg-white p-8 text-center shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">404</p>
        <h1 className="mt-4 text-4xl font-semibold text-slate-900">Essa página não foi encontrada.</h1>
        <p className="mt-5 text-base leading-8 text-slate-600">
          O link pode estar quebrado ou a rota ainda não existir nesta fase do produto. Vamos te colocar de volta no caminho certo.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Link
            className="rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            to="/"
          >
            Ir para a landing page
          </Link>
          <Link
            className="rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
            to="/login"
          >
            Abrir login
          </Link>
        </div>
      </div>
    </div>
  );
}

function AuthLayout({
  eyebrow,
  title,
  description,
  sideTitle,
  sideText,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
  sideTitle: string;
  sideText: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f4f6f1] px-4 py-12">
      <div className="grid w-full max-w-5xl gap-8 lg:grid-cols-[0.92fr_1.08fr]">
        <section className="rounded-[32px] bg-slate-900 px-7 py-8 text-white shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">{eyebrow}</p>
          <h1 className="mt-4 text-4xl font-semibold leading-tight text-balance">{sideTitle}</h1>
          <p className="mt-5 text-sm leading-7 text-slate-300">{sideText}</p>

          <div className="mt-8 grid gap-3">
            <FeatureChip label="Clareza" helper="Microtextos curtos e humanos." dark />
            <FeatureChip label="Segurança" helper="Força da senha e base para anti-bot." dark />
            <FeatureChip label="Confiança" helper="Privacidade e recuperação já pensadas." dark />
          </div>
        </section>

        <section className="rounded-[32px] border border-emerald-100 bg-white p-7 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">{eyebrow}</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">{title}</h2>
          <p className="mt-3 text-sm leading-7 text-slate-600">{description}</p>
          <div className="mt-6 grid gap-4">{children}</div>
        </section>
      </div>
    </div>
  );
}

function Workspace({ onLogout }: { onLogout: () => void }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [currentView, setCurrentView] = useState<ViewId>('painel');
  const [dashboardYear, setDashboardYear] = useState(currentYear);
  const [dashboardMonth, setDashboardMonth] = useState(currentMonth);
  const [analysisYear, setAnalysisYear] = useState(currentYear);
  const [analysisMonth, setAnalysisMonth] = useState(currentMonth);
  const [transactionSearch, setTransactionSearch] = useState('');
  const [transactionTypeFilter, setTransactionTypeFilter] = useState<'TODOS' | 'RECEITA' | 'DESPESA'>('TODOS');
  const [transactionCategoryFilter, setTransactionCategoryFilter] = useState<'TODAS' | Category>('TODAS');
  const [wishlistStatusFilter, setWishlistStatusFilter] = useState<'TODOS' | 'PENDENTE' | 'COMPRADO'>('TODOS');
  const [wishlistListFilter, setWishlistListFilter] = useState<'TODAS' | string>('TODAS');
  const [transactionModalOpen, setTransactionModalOpen] = useState(false);
  const [purchaseModalItemId, setPurchaseModalItemId] = useState<number | null>(null);
  const [historyItemId, setHistoryItemId] = useState<number | null>(null);
  const [onboardingDismissed, setOnboardingDismissed] = useState(false);
  const [transactionCategoryTouched, setTransactionCategoryTouched] = useState(false);
  const [wishlistCategoryTouched, setWishlistCategoryTouched] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [transactionDraft, setTransactionDraft] = useState<TransactionDraft>(buildTransactionDraft('DESPESA'));
  const [wishlistDraft, setWishlistDraft] = useState<WishlistDraft>({
    description: '',
    notes: '',
    originalPrice: '',
    discountPercent: '0',
    priority: 'MEDIA',
    category: 'OUTROS',
    listId: '1',
  });
  const [purchaseDraft, setPurchaseDraft] = useState<PurchaseDraft>({
    purchaseDate: new Date().toISOString().slice(0, 10),
    paymentMethod: 'PIX',
    installments: 1,
    firstInstallmentNextMonth: false,
  });
  const dashboardQuery = useDashboardQuery(dashboardYear, dashboardMonth);
  const transactionsQuery = useTransactionsQuery({
    type: transactionTypeFilter,
    category: transactionCategoryFilter,
    enabled: true,
  });
  const monthlyAnalysisQuery = useMonthlyAnalysisQuery(analysisYear, analysisMonth);
  const wishlistListsQuery = useWishlistListsQuery();
  const wishlistItemsQuery = useWishlistItemsQuery({
    status: wishlistStatusFilter,
    listId: wishlistListFilter,
    enabled: true,
  });
  const wishlistSummaryQuery = useWishlistSummaryQuery();
  const wishlistHistoryQuery = useWishlistHistoryQuery(historyItemId);
  const purchaseHistoryQuery = useWishlistHistoryQuery(purchaseModalItemId);
  const createTransactionMutation = useCreateTransactionMutation();
  const createWishlistItemMutation = useCreateWishlistItemMutation();
  const purchaseWishlistItemMutation = usePurchaseWishlistItemMutation();
  const undoWishlistPurchaseMutation = useUndoWishlistPurchaseMutation();

  useEffect(() => {
    if (location.pathname === '/app') {
      return;
    }

    navigate('/app', { replace: true });
  }, [location.pathname, navigate]);

  useEffect(() => {
    setOnboardingDismissed(localStorage.getItem(onboardingKey) === 'true');
  }, []);

  useEffect(() => {
    if (toasts.length === 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setToasts((currentValue) => currentValue.slice(1));
    }, 3200);

    return () => window.clearTimeout(timer);
  }, [toasts]);

  const transactionSuggestion = useMemo(
    () => getSuggestedCategory(transactionDraft.description),
    [transactionDraft.description],
  );
  const wishlistSuggestion = useMemo(
    () => getSuggestedCategory(wishlistDraft.description),
    [wishlistDraft.description],
  );

  const dashboardSnapshot = dashboardQuery.data ?? null;
  const monthlySnapshot = monthlyAnalysisQuery.data ?? null;
  const transactions = transactionsQuery.data ?? [];
  const wishlistItems = wishlistItemsQuery.data ?? [];
  const wishlistLists = wishlistListsQuery.data ?? [];

  useEffect(() => {
    if (wishlistLists.length === 0) {
      return;
    }

    const listExists = wishlistLists.some((list) => String(list.id) === wishlistDraft.listId);

    if (listExists) {
      return;
    }

    const defaultList = wishlistLists.find((list) => list.isDefault) ?? wishlistLists[0];

    setWishlistDraft((currentValue) => ({
      ...currentValue,
      listId: String(defaultList.id),
    }));
  }, [wishlistDraft.listId, wishlistLists]);

  const filteredTransactions = useMemo(() => {
    return transactions
      .filter((transaction) => {
        if (!transactionSearch.trim()) {
          return true;
        }
        const normalizedSearch = transactionSearch.toLowerCase();
        return (
          transaction.description.toLowerCase().includes(normalizedSearch) ||
          categoryLabels[transaction.category].toLowerCase().includes(normalizedSearch)
        );
      })
      .slice()
      .sort((left, right) => right.transactionDate.localeCompare(left.transactionDate));
  }, [transactionSearch, transactions]);

  const wishlistSummary = useMemo(() => {
    const summary = wishlistSummaryQuery.data;

    if (!summary) {
      return {
        desiredCount: 0,
        purchasedCount: 0,
        desiredValue: 0,
        purchasedValue: 0,
      };
    }

    return {
      desiredCount: summary.quantidadeItensDesejados,
      purchasedCount: summary.quantidadeItensComprados,
      desiredValue: summary.valorTotalDesejados,
      purchasedValue: summary.valorTotalComprados,
    };
  }, [wishlistSummaryQuery.data]);

  const purchaseModalItem = useMemo(
    () => wishlistItems.find((item) => item.id === purchaseModalItemId) ?? null,
    [purchaseModalItemId, wishlistItems],
  );

  const purchaseModalHistory = purchaseHistoryQuery.data ?? [];
  const filteredWishlistItems = wishlistItems;
  const selectedWishlistHistory = wishlistHistoryQuery.data ?? [];

  const pushToast = (message: string, tone: ToastTone = 'success') => {
    setToasts((currentValue) => [...currentValue, { id: Date.now() + Math.random(), message, tone }]);
  };

  const dismissOnboarding = () => {
    setOnboardingDismissed(true);
    localStorage.setItem(onboardingKey, 'true');
  };

  const handleTransactionDescriptionChange = (value: string) => {
    const suggestion = getSuggestedCategory(value);

    setTransactionDraft((currentValue) => ({
      ...currentValue,
      description: value,
      category: !transactionCategoryTouched && suggestion ? suggestion : currentValue.category,
    }));
  };

  const handleCreateTransaction = () => {
    const parsedAmount = Number(transactionDraft.amount);

    if (!transactionDraft.description || !parsedAmount) {
      return;
    }

    createTransactionMutation.mutate(
      {
        type: transactionDraft.type,
        description: transactionDraft.description.trim(),
        category: transactionDraft.category,
        amount: parsedAmount,
        paymentMethod: transactionDraft.paymentMethod,
        transactionDate: transactionDraft.transactionDate,
      },
      {
        onSuccess: () => {
          setTransactionDraft(buildTransactionDraft(transactionDraft.type));
          setTransactionCategoryTouched(false);
          setTransactionModalOpen(false);
          pushToast(
            transactionDraft.type === 'RECEITA'
              ? 'Receita adicionada ao histórico.'
              : 'Despesa adicionada ao histórico.',
          );
        },
        onError: () => {
          pushToast('Não foi possível salvar a transação agora.', 'info');
        },
      },
    );
  };

  const handleCreateWishlistItem = () => {
    const originalPrice = Number(wishlistDraft.originalPrice);
    const discountPercent = Number(wishlistDraft.discountPercent || 0);

    if (!wishlistDraft.description || !originalPrice) {
      return;
    }

    createWishlistItemMutation.mutate(
      {
        description: wishlistDraft.description.trim(),
        notes: wishlistDraft.notes.trim(),
        originalPrice,
        discountPercent,
        priority: wishlistDraft.priority,
        category: wishlistDraft.category,
        listId: Number(wishlistDraft.listId),
      },
      {
        onSuccess: (createdItem) => {
          setHistoryItemId(createdItem.id);
          setWishlistDraft((currentValue) => ({
            ...currentValue,
            description: '',
            notes: '',
            originalPrice: '',
            discountPercent: '0',
            priority: 'MEDIA',
            category: 'OUTROS',
          }));
          setWishlistCategoryTouched(false);
          pushToast('Item adicionado à lista de desejos.');
        },
        onError: () => {
          pushToast('Não foi possível criar o item agora.', 'info');
        },
      },
    );
  };

  const handlePurchaseWishlistItem = () => {
    if (!purchaseModalItem) {
      return;
    }

    purchaseWishlistItemMutation.mutate(
      {
        id: purchaseModalItem.id,
        data: {
          purchaseDate: purchaseDraft.purchaseDate,
          paymentMethod: purchaseDraft.paymentMethod,
          installments: purchaseDraft.installments,
          firstInstallmentNextMonth: purchaseDraft.firstInstallmentNextMonth,
        },
      },
      {
        onSuccess: () => {
          setHistoryItemId(purchaseModalItem.id);
          setPurchaseDraft({
            purchaseDate: new Date().toISOString().slice(0, 10),
            paymentMethod: 'PIX',
            installments: 1,
            firstInstallmentNextMonth: false,
          });
          setPurchaseModalItemId(null);
          pushToast('Compra concluída e lançamentos gerados.');
        },
        onError: () => {
          pushToast('Não foi possível concluir a compra agora.', 'info');
        },
      },
    );
  };

  const handleUndoPurchase = (itemId: number) => {
    undoWishlistPurchaseMutation.mutate(itemId, {
      onSuccess: () => {
        setHistoryItemId(itemId);
        pushToast('Compra desfeita e lançamentos removidos.', 'info');
      },
      onError: () => {
        pushToast('Não foi possível desfazer a compra agora.', 'info');
      },
    });
  };

  const currentSection = navItems.find((item) => item.id === currentView) ?? navItems[0];

  return (
    <div className="min-h-screen bg-[#f4f6f1] text-slate-900">
      <div className="border-b border-emerald-100 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-lg font-bold text-white shadow-lg shadow-emerald-500/20">
              CG
            </div>
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Controle de Gastos</p>
              <h1 className="text-xl font-semibold text-slate-900">Ambiente logado para explorar o produto</h1>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              className="rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
              onClick={() => {
                setTransactionDraft(buildTransactionDraft('RECEITA'));
                setTransactionCategoryTouched(false);
                setTransactionModalOpen(true);
              }}
              type="button"
            >
              Nova receita
            </button>
            <button
              className="rounded-full border border-rose-200 bg-rose-50 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-100"
              onClick={() => {
                setTransactionDraft(buildTransactionDraft('DESPESA'));
                setTransactionCategoryTouched(false);
                setTransactionModalOpen(true);
              }}
              type="button"
            >
              Nova despesa
            </button>
            <div className="flex items-center gap-3 rounded-full border border-slate-200 bg-white px-3 py-2">
              <div className="h-9 w-9 rounded-full bg-slate-100" />
              <div className="text-left">
                <p className="text-sm font-semibold text-slate-900">{user?.name ?? 'Usuário'}</p>
                <p className="text-xs text-slate-500">{user?.email ?? 'sem e-mail carregado'}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-8 lg:grid-cols-[260px_minmax(0,1fr)] lg:px-8">
        <aside className="rounded-[28px] border border-emerald-100 bg-white p-5 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Navegação</p>
          <nav className="mt-4 grid gap-2">
            {navItems.map((item) => (
              <button
                key={item.id}
                className={`rounded-[22px] px-4 py-4 text-left transition ${
                  item.id === currentView
                    ? 'bg-slate-900 text-white shadow-lg shadow-slate-900/10'
                    : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                }`}
                onClick={() => setCurrentView(item.id)}
                type="button"
              >
                <p className="font-semibold">{item.label}</p>
                <p className={`mt-1 text-sm ${item.id === currentView ? 'text-slate-300' : 'text-slate-500'}`}>
                  {item.description}
                </p>
              </button>
            ))}
          </nav>

          <div className="mt-6 rounded-[22px] border border-emerald-100 bg-emerald-50 p-4">
            <p className="text-sm font-semibold text-emerald-700">Decisão importante</p>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              Este front já conversa com a lógica do backend, mas continua sem integração real para te dar espaço para pensar com calma.
            </p>
          </div>
        </aside>

        <main className="space-y-6">
          <section className="rounded-[32px] bg-slate-900 px-6 py-7 text-white shadow-[0_24px_70px_rgba(15,23,42,0.18)] lg:px-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">Visão atual</p>
                <h2 className="mt-2 text-3xl font-semibold">{currentSection.label}</h2>
                <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
                  {currentSection.id === 'painel' &&
                    'O painel resume o mês atual, mostra o acumulado até o período de referência e conecta o usuário às ações mais importantes.'}
                  {currentSection.id === 'transacoes' &&
                    'A área de transações já nasce preparada para modal, sugestão automática de categoria e filtros que façam sentido no uso real.'}
                  {currentSection.id === 'analise' &&
                    'A análise mensal foi desenhada para ler o mês atual, comparar com períodos anteriores e deixar a tendência explícita.'}
                  {currentSection.id === 'wishlist' &&
                    'A lista de desejos funciona como um bloco forte do produto: desejo, prioridade, desconto, compra e impacto financeiro visualizados juntos.'}
                  {currentSection.id === 'configuracoes' &&
                    'Configurações foi organizada como seção de SaaS real: perfil, conta, segurança, privacidade e preferências.'}
                </p>
              </div>

              <div className="grid gap-3 rounded-[24px] border border-white/10 bg-white/5 p-4 text-sm text-slate-300 sm:grid-cols-2">
                <div>
                  <p className="font-semibold text-white">Período do painel</p>
                  <p className="mt-1">{formatMonthLabel(dashboardYear, dashboardMonth)}</p>
                </div>
                <div>
                  <p className="font-semibold text-white">Período da análise</p>
                  <p className="mt-1">{formatMonthLabel(analysisYear, analysisMonth)}</p>
                </div>
              </div>
            </div>
          </section>

          {currentView === 'painel' && (
            <>
              {!onboardingDismissed && (
                <WelcomePanel
                  onDismiss={dismissOnboarding}
                  onOpenTransactions={() => setCurrentView('transacoes')}
                  onOpenWishlist={() => setCurrentView('wishlist')}
                  sessionName={user?.name ?? 'Usuário'}
                />
              )}
              <DashboardView
                hasError={dashboardQuery.isError}
                isLoading={dashboardQuery.isLoading}
                month={dashboardMonth}
                onOpenWishlist={() => setCurrentView('wishlist')}
                onReferenceMonthChange={setDashboardMonth}
                onReferenceYearChange={setDashboardYear}
                snapshot={dashboardSnapshot}
                wishlistSummary={wishlistSummary}
                year={dashboardYear}
              />
            </>
          )}

          {currentView === 'transacoes' && (
            <TransactionsView
              categoryFilter={transactionCategoryFilter}
              hasError={transactionsQuery.isError}
              isLoading={transactionsQuery.isLoading}
              onCategoryFilterChange={setTransactionCategoryFilter}
              onOpenModal={() => setTransactionModalOpen(true)}
              onSearchChange={setTransactionSearch}
              onTypeFilterChange={setTransactionTypeFilter}
              search={transactionSearch}
              transactions={filteredTransactions}
              typeFilter={transactionTypeFilter}
            />
          )}

          {currentView === 'analise' && (
            <MonthlyAnalysisView
              hasError={monthlyAnalysisQuery.isError}
              isLoading={monthlyAnalysisQuery.isLoading}
              month={analysisMonth}
              monthOptions={monthOptions}
              onMonthChange={setAnalysisMonth}
              onYearChange={setAnalysisYear}
              snapshot={monthlySnapshot}
              year={analysisYear}
              yearOptions={yearOptions}
            />
          )}

          {currentView === 'wishlist' && (
            <WishlistView
              categoryTouched={wishlistCategoryTouched}
              currentListFilter={wishlistListFilter}
              currentStatusFilter={wishlistStatusFilter}
              draft={wishlistDraft}
              filteredItems={filteredWishlistItems}
              hasError={wishlistItemsQuery.isError || wishlistListsQuery.isError || wishlistSummaryQuery.isError}
              history={selectedWishlistHistory}
              isLoading={wishlistItemsQuery.isLoading || wishlistListsQuery.isLoading || wishlistSummaryQuery.isLoading}
              lists={wishlistLists}
              onCreate={handleCreateWishlistItem}
              onDraftChange={setWishlistDraft}
              onListFilterChange={setWishlistListFilter}
              onMarkPurchased={setPurchaseModalItemId}
              onOpenHistory={setHistoryItemId}
              onStatusFilterChange={setWishlistStatusFilter}
              onUndoPurchase={handleUndoPurchase}
              setCategoryTouched={setWishlistCategoryTouched}
              summary={wishlistSummary}
              suggestion={wishlistSuggestion}
            />
          )}

          {currentView === 'configuracoes' && <SettingsView onLogout={onLogout} user={user} />}
        </main>
      </div>

      <TransactionModal
        draft={transactionDraft}
        isOpen={transactionModalOpen}
        onCategoryTouched={setTransactionCategoryTouched}
        onClose={() => setTransactionModalOpen(false)}
        onDescriptionChange={handleTransactionDescriptionChange}
        onDraftChange={setTransactionDraft}
        onSubmit={handleCreateTransaction}
        suggestion={transactionSuggestion}
      />

      <PurchaseModal
        draft={purchaseDraft}
        history={purchaseModalHistory}
        isOpen={!!purchaseModalItem}
        item={purchaseModalItem}
        onClose={() => setPurchaseModalItemId(null)}
        onDraftChange={setPurchaseDraft}
        onSubmit={handlePurchaseWishlistItem}
      />

      <ToastStack toasts={toasts} />
    </div>
  );
}

function DashboardView({
  snapshot,
  wishlistSummary,
  hasError,
  isLoading,
  year,
  month,
  onReferenceYearChange,
  onReferenceMonthChange,
  onOpenWishlist,
}: {
  snapshot: DashboardResponse | null;
  wishlistSummary: {
    desiredCount: number;
    purchasedCount: number;
    desiredValue: number;
    purchasedValue: number;
  };
  hasError: boolean;
  isLoading: boolean;
  year: number;
  month: number;
  onReferenceYearChange: (value: number) => void;
  onReferenceMonthChange: (value: number) => void;
  onOpenWishlist: () => void;
}) {
  if (isLoading) {
    return <LoadingCard label="Carregando painel real..." />;
  }

  if (hasError || !snapshot) {
    return <UnavailableCard label="Não foi possível carregar o painel agora." />;
  }

  return (
    <>
      <SectionCard>
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Resumo do período</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">{formatMonthLabel(year, month)}</h3>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              O painel mostra o mês escolhido com mais clareza e preserva o acumulado até esse ponto para contar a história financeira certa.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <SelectField
              label="Ano"
              options={yearOptions.map((option) => ({ value: String(option), label: String(option) }))}
              value={String(year)}
              onChange={(value) => onReferenceYearChange(Number(value))}
            />
            <SelectField
              label="Mês"
              options={monthOptions.map((option) => ({ value: String(option.value), label: option.label }))}
              value={String(month)}
              onChange={(value) => onReferenceMonthChange(Number(value))}
            />
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Receitas do mês" tone="positive" value={formatCurrency(snapshot.receitasMesAtual)} />
          <MetricCard label="Despesas do mês" tone="negative" value={formatCurrency(snapshot.despesasMesAtual)} />
          <MetricCard label="Resultado do mês" tone={snapshot.resultadoMesAtual >= 0 ? 'neutral' : 'warning'} value={formatCurrency(snapshot.resultadoMesAtual)} />
          <MetricCard label="Saldo acumulado" tone="neutral" value={formatCurrency(snapshot.saldoAcumulado)} />
        </div>
      </SectionCard>

      <section className="grid gap-5 xl:grid-cols-[1.3fr_0.7fr]">
        <SectionCard title="Fluxo do período">
          <div className="grid gap-4 sm:grid-cols-3">
            <InfoStrip helper="Tudo que entrou até o mês de referência." label="Receitas do ano" value={formatCurrency(snapshot.receitasAnoReferencia)} />
            <InfoStrip helper="Tudo que saiu até o mês de referência." label="Despesas do ano" value={formatCurrency(snapshot.despesasAnoReferencia)} />
            <InfoStrip helper="Resultado acumulado do ano até aqui." label="Resultado do ano" value={formatCurrency(snapshot.resultadoAnoReferencia)} />
          </div>

          <div className="mt-6 rounded-[24px] bg-slate-50 p-5">
            <div className="flex items-center justify-between text-sm text-slate-500">
              <span>Entrada x saída do mês</span>
              <span>{snapshot.resultadoMesAtual >= 0 ? 'Mês positivo' : 'Mês apertado'}</span>
            </div>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <ProgressBar label="Receitas" tone="positive" total={Math.max(snapshot.receitasMesAtual, snapshot.despesasMesAtual, 1)} value={snapshot.receitasMesAtual} />
              <ProgressBar label="Despesas" tone="negative" total={Math.max(snapshot.receitasMesAtual, snapshot.despesasMesAtual, 1)} value={snapshot.despesasMesAtual} />
            </div>
          </div>
        </SectionCard>

        <SectionCard title="Lista de desejos em destaque">
          <p className="text-sm leading-7 text-slate-600">
            Esse bloco mostra o peso dos desejos sem roubar a atenção da parte financeira principal.
          </p>
          <div className="mt-5 grid gap-3">
            <InfoStrip helper="Itens aguardando decisão." label="Pendentes" value={`${wishlistSummary.desiredCount} item(ns)`} />
            <InfoStrip helper="Total ainda desejado." label="Valor desejado" value={formatCurrency(wishlistSummary.desiredValue)} />
            <InfoStrip helper="Compra concluída já enviada ao financeiro." label="Comprados" value={`${wishlistSummary.purchasedCount} item(ns)`} />
          </div>
          <button
            className="mt-5 rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
            onClick={onOpenWishlist}
            type="button"
          >
            Abrir lista de desejos
          </button>
        </SectionCard>
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <SectionCard title="Últimas 5 transações">
          <div className="grid max-h-[320px] gap-3 overflow-y-auto pr-1">
            {snapshot.ultimasTransacoes.map((transaction) => (
              <div key={transaction.id} className="rounded-[20px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-slate-900">{transaction.description}</p>
                    <p className="mt-1 text-sm text-slate-500">
                      {categoryLabels[transaction.category]} · {new Date(`${transaction.transactionDate}T12:00:00`).toLocaleDateString('pt-BR')}
                    </p>
                  </div>
                  <span className={`text-sm font-semibold ${transaction.type === 'RECEITA' ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {transaction.type === 'RECEITA' ? '+' : '-'} {formatCurrency(transaction.amount)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="Gastos por categoria">
          <div className="grid max-h-[320px] gap-4 overflow-y-auto pr-1">
            {snapshot.gastosPorCategoria.length === 0 && <EmptyState label="Sem despesas categorizadas no período." />}
            {snapshot.gastosPorCategoria.map((entry) => (
              <div key={entry.category}>
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-semibold text-slate-700">{categoryLabels[entry.category]}</span>
                  <span className="text-slate-500">{formatCurrency(entry.totalAmount)}</span>
                </div>
                <div className="h-3 rounded-full bg-slate-100">
                  <div
                    className="h-3 rounded-full bg-gradient-to-r from-emerald-400 to-emerald-500"
                    style={{
                      width: `${Math.max((entry.totalAmount / Math.max(...snapshot.gastosPorCategoria.map((item) => item.totalAmount), 1)) * 100, 8)}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>
    </>
  );
}

function WelcomePanel({
  sessionName,
  onDismiss,
  onOpenTransactions,
  onOpenWishlist,
}: {
  sessionName: string;
  onDismiss: () => void;
  onOpenTransactions: () => void;
  onOpenWishlist: () => void;
}) {
  return (
    <section className="rounded-[32px] border border-emerald-200 bg-gradient-to-r from-emerald-500 to-emerald-400 px-6 py-6 text-white shadow-[0_24px_70px_rgba(16,185,129,0.18)] lg:px-8">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-100">Primeiros passos</p>
          <h3 className="mt-2 text-3xl font-semibold">Bom te ver por aqui, {sessionName.split(' ')[0]}.</h3>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-emerald-50">
            Este bloco deixa a entrada mais humana. Primeiro você entende o ambiente, depois registra algo e por fim testa a lista de desejos sem se perder.
          </p>
        </div>

        <button
          className="rounded-full border border-white/30 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
          onClick={onDismiss}
          type="button"
        >
          Dispensar
        </button>
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-3">
        <FeatureChip dark helper="Veja o mês, o acumulado e a lista de desejos em destaque." label="1. Ler o Painel" />
        <FeatureChip dark helper="Crie uma receita ou despesa e sinta o fluxo do modal." label="2. Fazer um lançamento" />
        <FeatureChip dark helper="Marque um item como comprado e observe o impacto no financeiro." label="3. Testar a lista de desejos" />
      </div>

      <div className="mt-6 flex flex-wrap gap-3">
        <button
          className="rounded-full bg-white px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-50"
          onClick={onOpenTransactions}
          type="button"
        >
          Ir para transações
        </button>
        <button
          className="rounded-full border border-white/30 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
          onClick={onOpenWishlist}
          type="button"
        >
          Abrir lista de desejos
        </button>
      </div>
    </section>
  );
}

function TransactionsView({
  transactions,
  search,
  typeFilter,
  categoryFilter,
  hasError,
  isLoading,
  onSearchChange,
  onTypeFilterChange,
  onCategoryFilterChange,
  onOpenModal,
}: {
  transactions: TransactionResponse[];
  search: string;
  typeFilter: 'TODOS' | 'RECEITA' | 'DESPESA';
  categoryFilter: 'TODAS' | Category;
  hasError: boolean;
  isLoading: boolean;
  onSearchChange: (value: string) => void;
  onTypeFilterChange: (value: 'TODOS' | 'RECEITA' | 'DESPESA') => void;
  onCategoryFilterChange: (value: 'TODAS' | Category) => void;
  onOpenModal: () => void;
}) {
  if (isLoading) {
    return <LoadingCard label="Carregando transações reais..." />;
  }

  if (hasError) {
    return <UnavailableCard label="Não foi possível carregar as transações agora." />;
  }

  return (
    <>
      <section className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <SectionCard title="Filtros">
          <div className="grid gap-4 md:grid-cols-3">
            <Field label="Buscar">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                placeholder="Descrição ou categoria"
                value={search}
                onChange={(event) => onSearchChange(event.target.value)}
              />
            </Field>
            <SelectField
              label="Tipo"
              options={[
                { value: 'TODOS', label: 'Todos' },
                { value: 'RECEITA', label: 'Receitas' },
                { value: 'DESPESA', label: 'Despesas' },
              ]}
              value={typeFilter}
              onChange={(value) => onTypeFilterChange(value as 'TODOS' | 'RECEITA' | 'DESPESA')}
            />
            <SelectField
              label="Categoria"
              options={[
                { value: 'TODAS', label: 'Todas' },
                ...Object.entries(categoryLabels).map(([value, label]) => ({ value, label })),
              ]}
              value={categoryFilter}
              onChange={(value) => onCategoryFilterChange(value as 'TODAS' | Category)}
            />
          </div>
        </SectionCard>

        <SectionCard title="Lançamento rápido">
          <p className="text-sm leading-7 text-slate-600">
            O fluxo principal continua no modal, para o usuário registrar algo sem sair da tela de histórico.
          </p>
          <button
            className="mt-5 rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            onClick={onOpenModal}
            type="button"
          >
            Abrir modal de lançamento
          </button>
        </SectionCard>
      </section>

      <section className="rounded-[28px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Histórico</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">Entradas e saídas</h3>
          </div>
          <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-600">
            {transactions.length} registro(s)
          </span>
        </div>

        <div className="mt-6 overflow-hidden rounded-[24px] border border-slate-100">
          <div className="hidden grid-cols-[1.1fr_0.7fr_0.7fr_0.7fr_0.8fr_0.7fr] gap-3 bg-slate-50 px-5 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500 md:grid">
            <span>Descrição</span>
            <span>Tipo</span>
            <span>Categoria</span>
            <span>Pagamento</span>
            <span>Data</span>
            <span className="text-right">Valor</span>
          </div>

          <div className="grid gap-px bg-slate-100">
            {transactions.length === 0 && (
              <div className="bg-white px-5 py-12">
                <EmptyState label="Nenhuma transação encontrada com os filtros atuais." />
              </div>
            )}

            {transactions.map((transaction) => (
              <div key={transaction.id} className="grid gap-4 bg-white px-5 py-4 md:grid-cols-[1.1fr_0.7fr_0.7fr_0.7fr_0.8fr_0.7fr] md:items-center">
                <div>
                  <p className="font-semibold text-slate-900">{transaction.description}</p>
                </div>
                <Tag tone={transaction.type === 'RECEITA' ? 'positive' : 'negative'}>{transaction.type === 'RECEITA' ? 'Receita' : 'Despesa'}</Tag>
                <span className="text-sm text-slate-600">{categoryLabels[transaction.category]}</span>
                <span className="text-sm text-slate-600">{paymentMethodLabels[transaction.paymentMethod]}</span>
                <span className="text-sm text-slate-600">{new Date(`${transaction.transactionDate}T12:00:00`).toLocaleDateString('pt-BR')}</span>
                <span className={`text-right text-sm font-semibold ${transaction.type === 'RECEITA' ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {transaction.type === 'RECEITA' ? '+' : '-'} {formatCurrency(transaction.amount)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function MonthlyAnalysisView({
  snapshot,
  hasError,
  isLoading,
  year,
  month,
  yearOptions,
  monthOptions,
  onYearChange,
  onMonthChange,
}: {
  snapshot: MonthlyAnalysisResponse | null;
  hasError: boolean;
  isLoading: boolean;
  year: number;
  month: number;
  yearOptions: number[];
  monthOptions: Array<{ value: number; label: string }>;
  onYearChange: (value: number) => void;
  onMonthChange: (value: number) => void;
}) {
  if (isLoading) {
    return <LoadingCard label="Carregando análise mensal real..." />;
  }

  if (hasError || !snapshot) {
    return <UnavailableCard label="Não foi possível carregar a análise mensal agora." />;
  }

  return (
    <>
      <SectionCard>
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Leitura do mês</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">{formatMonthLabel(year, month)}</h3>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              Esta área deixa o período bem visível e cria o contexto certo antes das comparações.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <SelectField
              label="Ano"
              options={yearOptions.map((option) => ({ value: String(option), label: String(option) }))}
              value={String(year)}
              onChange={(value) => onYearChange(Number(value))}
            />
            <SelectField
              label="Mês"
              options={monthOptions.map((option) => ({ value: String(option.value), label: option.label }))}
              value={String(month)}
              onChange={(value) => onMonthChange(Number(value))}
            />
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          <MetricCard label="Receitas do mês" tone="positive" value={formatCurrency(snapshot.totalReceitas)} />
          <MetricCard label="Despesas do mês" tone="negative" value={formatCurrency(snapshot.totalDespesas)} />
          <MetricCard label="Saldo do mês" tone={snapshot.saldo >= 0 ? 'neutral' : 'warning'} value={formatCurrency(snapshot.saldo)} />
        </div>
      </SectionCard>

      <section className="grid gap-5 lg:grid-cols-2">
        <SectionCard title="Maior gasto">
          {snapshot.maiorGasto ? <ExpenseHighlight transaction={snapshot.maiorGasto} /> : <EmptyState label="Sem despesas registradas no período." />}
        </SectionCard>

        <SectionCard title="Tendência do período">
          <div className="grid gap-4">
            <TrendRow label="Versus mês anterior" tone={snapshot.comparativoMesAnterior.tendenciaGeral} value={snapshot.comparativoMesAnterior.diferencaSaldo} />
            <TrendRow
              label="Versus ano passado"
              tone={snapshot.comparativoMesmoMesAnoAnterior.tendenciaGeral}
              value={snapshot.comparativoMesmoMesAnoAnterior.diferencaSaldo}
            />
          </div>
          <div className="mt-5 rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
            O mês está <span className="font-semibold text-slate-900">{snapshot.comparativoMesAnterior.tendenciaGeral.toLowerCase()}</span> em relação ao anterior e
            {' '}
            <span className="font-semibold text-slate-900">{snapshot.comparativoMesmoMesAnoAnterior.tendenciaGeral.toLowerCase()}</span> contra o mesmo mês do ano passado.
          </div>
        </SectionCard>
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <ComparisonCard
          title="Comparação com o mês anterior"
          subtitle={`Referência: ${formatMonthLabel(snapshot.comparativoMesAnterior.year, snapshot.comparativoMesAnterior.month)}`}
          summary={snapshot.comparativoMesAnterior}
        />
        <ComparisonCard
          title="Mesmo mês do ano passado"
          subtitle={`Referência: ${formatMonthLabel(
            snapshot.comparativoMesmoMesAnoAnterior.year,
            snapshot.comparativoMesmoMesAnoAnterior.month,
          )}`}
          summary={snapshot.comparativoMesmoMesAnoAnterior}
        />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard title="Acumulado do ano">
          <div className="grid gap-4 sm:grid-cols-3">
            <InfoStrip helper="Receitas até o mês selecionado." label="Receitas" value={formatCurrency(snapshot.acumuladoAnoAtual.totalReceitas)} />
            <InfoStrip helper="Despesas até o mês selecionado." label="Despesas" value={formatCurrency(snapshot.acumuladoAnoAtual.totalDespesas)} />
            <InfoStrip helper="Saldo do ano até aqui." label="Saldo" value={formatCurrency(snapshot.acumuladoAnoAtual.saldo)} />
          </div>
        </SectionCard>

        <SectionCard title="Ano atual x ano passado">
          <div className="grid gap-4">
            <TrendRow label="Receitas" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaReceitas} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaReceitas} />
            <TrendRow label="Despesas" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaDespesas} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaDespesas} inverse />
            <TrendRow label="Saldo" tone={snapshot.comparativoAcumuladoAnoAnterior.tendenciaSaldo} value={snapshot.comparativoAcumuladoAnoAnterior.diferencaSaldo} />
          </div>
          <div className="mt-5 rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
            Tendência geral do período: <span className="font-semibold text-slate-900">{snapshot.comparativoAcumuladoAnoAnterior.tendenciaGeral.toLowerCase()}</span>.
          </div>
        </SectionCard>
      </section>

      <SectionCard title="Categoria com maior peso no mês">
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="grid gap-4">
            {snapshot.gastosPorCategoria.length === 0 && <EmptyState label="Sem despesas categorizadas neste mês." />}
            {snapshot.gastosPorCategoria.map((entry) => (
              <div key={entry.category}>
                <div className="mb-2 flex items-center justify-between text-sm">
                  <span className="font-semibold text-slate-700">{categoryLabels[entry.category]}</span>
                  <span className="text-slate-500">{formatCurrency(entry.totalAmount)}</span>
                </div>
                <div className="h-3 rounded-full bg-slate-100">
                  <div
                    className="h-3 rounded-full bg-gradient-to-r from-slate-900 to-emerald-500"
                    style={{
                      width: `${Math.max((entry.totalAmount / Math.max(...snapshot.gastosPorCategoria.map((item) => item.totalAmount), 1)) * 100, 8)}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>

          <div className="rounded-[24px] bg-slate-50 p-5">
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Leitura sugerida</p>
            <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-600">
              <li>O produto pode resumir se o mês foi melhor, pior ou igual com base no saldo.</li>
              <li>Também pode destacar a categoria mais pesada para facilitar a tomada de decisão.</li>
              <li>Esse espaço será um bom lugar para insights textuais futuros.</li>
            </ul>
          </div>
        </div>
      </SectionCard>
    </>
  );
}

function WishlistView({
  lists,
  filteredItems,
  summary,
  draft,
  suggestion,
  categoryTouched,
  currentStatusFilter,
  currentListFilter,
  hasError,
  isLoading,
  onStatusFilterChange,
  onListFilterChange,
  onDraftChange,
  onCreate,
  onMarkPurchased,
  onUndoPurchase,
  onOpenHistory,
  history,
  setCategoryTouched,
}: {
  lists: WishlistListResponse[];
  filteredItems: WishlistItemResponse[];
  summary: {
    desiredCount: number;
    purchasedCount: number;
    desiredValue: number;
    purchasedValue: number;
  };
  draft: WishlistDraft;
  suggestion: Category | null;
  categoryTouched: boolean;
  currentStatusFilter: 'TODOS' | 'PENDENTE' | 'COMPRADO';
  currentListFilter: 'TODAS' | string;
  hasError: boolean;
  isLoading: boolean;
  onStatusFilterChange: (value: 'TODOS' | 'PENDENTE' | 'COMPRADO') => void;
  onListFilterChange: (value: 'TODAS' | string) => void;
  onDraftChange: Dispatch<SetStateAction<WishlistDraft>>;
  onCreate: () => void;
  onMarkPurchased: (itemId: number) => void;
  onUndoPurchase: (itemId: number) => void;
  onOpenHistory: (itemId: number) => void;
  history: WishlistHistoryResponse[];
  setCategoryTouched: (value: boolean) => void;
}) {
  if (isLoading) {
    return <LoadingCard label="Carregando lista de desejos real..." />;
  }

  if (hasError) {
    return <UnavailableCard label="Não foi possível carregar a lista de desejos agora." />;
  }

  return (
    <>
      <section className="grid gap-5 xl:grid-cols-[0.95fr_1.05fr]">
        <SectionCard title="Resumo da lista de desejos">
          <div className="grid gap-4 sm:grid-cols-2">
            <MetricCard label="Itens pendentes" tone="neutral" value={String(summary.desiredCount)} />
            <MetricCard label="Itens comprados" tone="positive" value={String(summary.purchasedCount)} />
            <MetricCard label="Valor desejado" tone="warning" value={formatCurrency(summary.desiredValue)} />
            <MetricCard label="Valor comprado" tone="positive" value={formatCurrency(summary.purchasedValue)} />
          </div>

          <div className="mt-6 grid gap-3">
            {lists.map((list) => (
              <div key={list.id} className="rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900">{list.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{list.description}</p>
                  </div>
                  {list.isDefault && <Tag tone="neutral">Padrão</Tag>}
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="Novo item desejado">
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Descrição">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                placeholder="Ex.: notebook, consulta médica, lancheira"
                value={draft.description}
                onChange={(event) =>
                  onDraftChange((currentValue) => {
                    const nextDescription = event.target.value;
                    const nextSuggestion = getSuggestedCategory(nextDescription);

                    return {
                      ...currentValue,
                      description: nextDescription,
                      category: !categoryTouched && nextSuggestion ? nextSuggestion : currentValue.category,
                    };
                  })
                }
              />
            </Field>
            <SelectField
              label="Lista"
              options={lists.map((list) => ({ value: String(list.id), label: list.name }))}
              value={draft.listId}
              onChange={(value) => onDraftChange((currentValue) => ({ ...currentValue, listId: value }))}
            />
            <Field label="Preço original">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                placeholder="0,00"
                type="number"
                value={draft.originalPrice}
                onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, originalPrice: event.target.value }))}
              />
            </Field>
            <Field label="Desconto (%)">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                placeholder="0"
                type="number"
                value={draft.discountPercent}
                onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, discountPercent: event.target.value }))}
              />
            </Field>
            <SelectField
              label="Prioridade"
              options={Object.entries(priorityLabels).map(([value, label]) => ({ value, label }))}
              value={draft.priority}
              onChange={(value) => onDraftChange((currentValue) => ({ ...currentValue, priority: value as WishlistPriority }))}
            />
            <SelectField
              label="Categoria"
              options={Object.entries(categoryLabels).map(([value, label]) => ({ value, label }))}
              value={draft.category}
              onChange={(value) => {
                setCategoryTouched(true);
                onDraftChange((currentValue) => ({ ...currentValue, category: value as Category }));
              }}
            />
          </div>

          <Field label="Observação">
            <textarea
              className="min-h-[110px] w-full rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="O que faz esse item ser importante agora?"
              value={draft.notes}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, notes: event.target.value }))}
            />
          </Field>

          <div className="mt-4 flex flex-col gap-4 rounded-[24px] bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
            <div className="text-sm leading-7 text-slate-600">
              <p>
                Sugestão automática de categoria:{' '}
                <span className="font-semibold text-emerald-700">{suggestion ? categoryLabels[suggestion] : 'sem sugestão por enquanto'}</span>
              </p>
              <p>
                Preço final estimado:{' '}
                <span className="font-semibold text-slate-900">
                  {formatCurrency(calculateFinalPrice(Number(draft.originalPrice || 0), Number(draft.discountPercent || 0)))}
                </span>
              </p>
            </div>
            <button
              className="rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
              onClick={onCreate}
              type="button"
            >
              Adicionar item
            </button>
          </div>
        </SectionCard>
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard title="Itens da lista">
          <div className="grid gap-4 md:grid-cols-2">
            <SelectField
              label="Filtrar status"
              options={[
                { value: 'TODOS', label: 'Todos' },
                { value: 'PENDENTE', label: 'Pendentes' },
                { value: 'COMPRADO', label: 'Comprados' },
              ]}
              value={currentStatusFilter}
              onChange={(value) => onStatusFilterChange(value as 'TODOS' | 'PENDENTE' | 'COMPRADO')}
            />
            <SelectField
              label="Filtrar lista"
              options={[
                { value: 'TODAS', label: 'Todas as listas' },
                ...lists.map((list) => ({ value: String(list.id), label: list.name })),
              ]}
              value={currentListFilter}
              onChange={(value) => onListFilterChange(value)}
            />
          </div>

          <div className="mt-5 grid gap-4">
            {filteredItems.length === 0 && <EmptyState label="Nenhum item encontrado com os filtros atuais." />}

            {filteredItems.map((item) => (
              <div key={item.id} className="rounded-[24px] border border-slate-100 bg-slate-50 p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-lg font-semibold text-slate-900">{item.description}</p>
                      <Tag tone={item.status === 'COMPRADO' ? 'positive' : 'warning'}>
                        {item.status === 'COMPRADO' ? 'Comprado' : 'Pendente'}
                      </Tag>
                      <Tag tone="neutral">{priorityLabels[item.priority]}</Tag>
                    </div>
                    <p className="mt-2 text-sm leading-7 text-slate-600">{item.notes}</p>
                    <p className="mt-3 text-sm text-slate-500">
                      {item.listName} · {categoryLabels[item.category]}
                    </p>
                  </div>

                  <div className="rounded-[22px] border border-slate-200 bg-white px-4 py-3 text-right">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Preço final</p>
                    <p className="mt-2 text-xl font-semibold text-slate-900">{formatCurrency(item.finalPrice)}</p>
                    {item.discountPercent > 0 && (
                      <p className="mt-2 text-sm text-emerald-600">
                        De {formatCurrency(item.originalPrice)} para {formatCurrency(item.finalPrice)}
                      </p>
                    )}
                  </div>
                </div>

                <div className="mt-5 flex flex-wrap gap-3">
                  {item.status === 'PENDENTE' ? (
                    <button
                      className="rounded-full bg-emerald-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-600"
                      onClick={() => onMarkPurchased(item.id)}
                      type="button"
                    >
                      Marcar como comprado
                    </button>
                  ) : (
                    <button
                      className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                      onClick={() => onUndoPurchase(item.id)}
                      type="button"
                    >
                      Desfazer compra
                    </button>
                  )}
                  <button
                    className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                    onClick={() => onOpenHistory(item.id)}
                    type="button"
                  >
                    Ver histórico
                  </button>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="Histórico da lista">
          {history.length === 0 && (
            <div className="rounded-[24px] bg-slate-50 p-5">
              <EmptyState label="Abra o histórico de um item ou use o modal de compra para ver a trilha de alterações." />
            </div>
          )}

          <div className="grid gap-3">
            {history.map((entry) => (
              <div key={entry.id} className="rounded-[20px] border border-slate-100 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <Tag tone={historyTone(entry.actionType)}>{entry.actionType.replace('_', ' ')}</Tag>
                  <span className="text-xs text-slate-500">
                    {new Date(entry.createdAt).toLocaleDateString('pt-BR', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                <p className="mt-3 text-sm leading-7 text-slate-600">{entry.description}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>
    </>
  );
}

function SettingsView({ onLogout, user }: { onLogout: () => void; user: AuthUser | null }) {
  return (
    <div className="grid gap-5 xl:grid-cols-2">
      <SectionCard title="Perfil">
        <div className="grid gap-4">
          <InfoStrip helper="Nome que aparece nas áreas logadas." label="Nome" value={user?.name ?? 'Usuário'} />
          <InfoStrip helper="E-mail principal da conta." label="E-mail" value={user?.email ?? 'E-mail indisponível'} />
          <InfoStrip helper="Papel carregado a partir da autenticação real." label="Perfil de acesso" value={user?.role ?? 'USER'} />
        </div>
      </SectionCard>

      <SectionCard title="Conta">
        <div className="grid gap-4">
          <InfoStrip helper="Reset de senha com token temporário e mensagem neutra." label="Recuperação de senha" value="Planejado" />
          <InfoStrip helper="Troca de e-mail com confirmação." label="Alteração de e-mail" value="Planejada" />
          <InfoStrip helper="Sessão simulada para testar o app." label="Sessão" value="Ativa" />
        </div>
      </SectionCard>

      <SectionCard title="Segurança">
        <div className="grid gap-4">
          <ChecklistItem checked label="Força de senha visível no cadastro." />
          <ChecklistItem checked label="Captcha previsto para cadastro, recuperação e ações suspeitas." />
          <ChecklistItem checked label="Cookies previstos com escolha simples no primeiro acesso." />
          <ChecklistItem checked label="Base para cookies necessários e opcionais." />
        </div>
      </SectionCard>

      <SectionCard title="Preferências">
        <div className="grid gap-4">
          <PreferenceItem description="Primeiro dia útil para olhar a vida financeira do mês." title="Mês padrão de abertura" />
          <PreferenceItem description="Notificações futuras para compras e tendência ruim." title="Alertas de produto" />
          <PreferenceItem description="Tema visual, densidade de cards e leitura mobile." title="Experiência do usuário" />
        </div>
      </SectionCard>

      <SectionCard title="Privacidade e cookies">
        <p className="text-sm leading-7 text-slate-600">
          A base do frontend já prevê banner de cookies, preferência essencial ou opcional e espaço para política de privacidade real quando o produto entrar em produção.
        </p>
        <div className="mt-5 grid gap-3">
          <ChecklistItem checked label="Cookies essenciais para sessão." />
          <ChecklistItem checked label="Cookies opcionais para analytics e experiência." />
          <ChecklistItem checked label="Base pronta para política de privacidade." />
        </div>
        <button
          className="mt-5 rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
          onClick={onLogout}
          type="button"
        >
          Sair da demonstração
        </button>
      </SectionCard>
    </div>
  );
}

function TransactionModal({
  isOpen,
  draft,
  suggestion,
  onDraftChange,
  onDescriptionChange,
  onCategoryTouched,
  onSubmit,
  onClose,
}: {
  isOpen: boolean;
  draft: TransactionDraft;
  suggestion: Category | null;
  onDraftChange: Dispatch<SetStateAction<TransactionDraft>>;
  onDescriptionChange: (value: string) => void;
  onCategoryTouched: (value: boolean) => void;
  onSubmit: () => void;
  onClose: () => void;
}) {
  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="w-full max-w-2xl rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Lançamento rápido</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">
              {draft.type === 'RECEITA' ? 'Nova receita' : 'Nova despesa'}
            </h3>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              Modal pensado para o usuário registrar algo sem sair do contexto, com sugestão automática de categoria e chance de editar tudo.
            </p>
          </div>
          <button
            className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
            onClick={onClose}
            type="button"
          >
            Fechar
          </button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <SelectField
            label="Tipo"
            options={[
              { value: 'DESPESA', label: 'Despesa' },
              { value: 'RECEITA', label: 'Receita' },
            ]}
            value={draft.type}
            onChange={(value) => onDraftChange((currentValue) => ({ ...currentValue, type: value as 'RECEITA' | 'DESPESA' }))}
          />
          <Field label="Data">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              type="date"
              value={draft.transactionDate}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, transactionDate: event.target.value }))}
            />
          </Field>
          <Field label="Descrição">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="Ex.: consulta médica, lanche, aluguel"
              value={draft.description}
              onChange={(event) => onDescriptionChange(event.target.value)}
            />
          </Field>
          <Field label="Valor">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="0,00"
              type="number"
              value={draft.amount}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, amount: event.target.value }))}
            />
          </Field>
          <SelectField
            label="Categoria"
            options={Object.entries(categoryLabels).map(([value, label]) => ({ value, label }))}
            value={draft.category}
            onChange={(value) => {
              onCategoryTouched(true);
              onDraftChange((currentValue) => ({ ...currentValue, category: value as Category }));
            }}
          />
          <SelectField
            label="Pagamento"
            options={Object.entries(paymentMethodLabels).map(([value, label]) => ({ value, label }))}
            value={draft.paymentMethod}
            onChange={(value) => onDraftChange((currentValue) => ({ ...currentValue, paymentMethod: value as PaymentMethod }))}
          />
        </div>

        <Field label="Observação">
          <textarea
            className="min-h-[110px] w-full rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Notas rápidas para lembrar do contexto."
            value={draft.notes}
            onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, notes: event.target.value }))}
          />
        </Field>

        <div className="mt-5 flex flex-col gap-4 rounded-[24px] bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
          <div className="text-sm leading-7 text-slate-600">
            <p>
              Sugestão automática:{' '}
              <span className="font-semibold text-emerald-700">{suggestion ? categoryLabels[suggestion] : 'sem sugestão suficiente'}</span>
            </p>
            <p>Se a sugestão errar, o usuário continua livre para trocar manualmente.</p>
          </div>
          <button
            className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
            onClick={onSubmit}
            type="button"
          >
            Salvar lançamento
          </button>
        </div>
      </div>
    </div>
  );
}

function PurchaseModal({
  isOpen,
  item,
  draft,
  history,
  onDraftChange,
  onSubmit,
  onClose,
}: {
  isOpen: boolean;
  item: WishlistItemResponse | null;
  draft: PurchaseDraft;
  history: WishlistHistoryResponse[];
  onDraftChange: Dispatch<SetStateAction<PurchaseDraft>>;
  onSubmit: () => void;
  onClose: () => void;
}) {
  if (!isOpen || !item) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="grid w-full max-w-4xl gap-5 rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)] xl:grid-cols-[1.1fr_0.9fr]">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Compra da lista</p>
          <h3 className="mt-2 text-2xl font-semibold text-slate-900">{item.description}</h3>
          <p className="mt-2 text-sm leading-7 text-slate-600">
            Ao confirmar, o item sai dos desejados, vai para comprados e gera lançamentos financeiros automáticos.
          </p>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <InfoStrip helper="Preço final com desconto aplicado." label="Valor final" value={formatCurrency(item.finalPrice)} />
            <InfoStrip helper="Categoria que entrará no financeiro." label="Categoria" value={categoryLabels[item.category]} />
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <Field label="Data da compra">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                type="date"
                value={draft.purchaseDate}
                onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, purchaseDate: event.target.value }))}
              />
            </Field>
            <SelectField
              label="Forma de pagamento"
              options={Object.entries(paymentMethodLabels).map(([value, label]) => ({ value, label }))}
              value={draft.paymentMethod}
              onChange={(value) =>
                onDraftChange((currentValue) => ({
                  ...currentValue,
                  paymentMethod: value as PaymentMethod,
                  installments: value === 'CARTAO_CREDITO_PARCELADO' ? Math.max(currentValue.installments, 2) : 1,
                }))
              }
            />
            <Field label="Parcelas">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                max={24}
                min={1}
                type="number"
                value={draft.installments}
                onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, installments: Number(event.target.value) }))}
              />
            </Field>
            <label className="flex items-center gap-3 rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              <input
                checked={draft.firstInstallmentNextMonth}
                className="h-5 w-5 rounded border-slate-300 text-emerald-500 focus:ring-emerald-400"
                type="checkbox"
                onChange={(event) =>
                  onDraftChange((currentValue) => ({
                    ...currentValue,
                    firstInstallmentNextMonth: event.target.checked,
                  }))
                }
              />
              Primeira parcela cai no mês seguinte
            </label>
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <button
              className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
              onClick={onSubmit}
              type="button"
            >
              Confirmar compra
            </button>
            <button
              className="rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
              onClick={onClose}
              type="button"
            >
              Cancelar
            </button>
          </div>
        </div>

        <div className="rounded-[28px] bg-slate-50 p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Histórico recente</p>
          <div className="mt-4 grid gap-3">
            {history.length === 0 && <EmptyState label="Esse item ainda não tem histórico relevante." />}
            {history.map((entry) => (
              <div key={entry.id} className="rounded-[20px] border border-white bg-white p-4">
                <div className="flex items-center justify-between gap-3">
                  <Tag tone={historyTone(entry.actionType)}>{entry.actionType.replace('_', ' ')}</Tag>
                  <span className="text-xs text-slate-500">
                    {new Date(entry.createdAt).toLocaleDateString('pt-BR', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                    })}
                  </span>
                </div>
                <p className="mt-3 text-sm leading-7 text-slate-600">{entry.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function SectionCard({ children, title }: { children: ReactNode; title?: string }) {
  return (
    <section className="rounded-[28px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
      {title && <h3 className="text-2xl font-semibold text-slate-900">{title}</h3>}
      {title && <div className="mt-5" />}
      {children}
    </section>
  );
}

function LoadingCard({ label }: { label: string }) {
  return (
    <SectionCard>
      <div className="flex min-h-[220px] items-center justify-center rounded-[24px] bg-slate-50 px-6 text-center text-sm font-semibold text-slate-500">
        {label}
      </div>
    </SectionCard>
  );
}

function UnavailableCard({ label }: { label: string }) {
  return (
    <SectionCard>
      <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 rounded-[24px] bg-slate-50 px-6 text-center">
        <p className="text-base font-semibold text-slate-900">Não conseguimos carregar este bloco.</p>
        <p className="max-w-md text-sm leading-7 text-slate-500">{label}</p>
      </div>
    </SectionCard>
  );
}

function MetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: 'positive' | 'negative' | 'neutral' | 'warning';
}) {
  const tones = {
    positive: 'border-emerald-100 bg-emerald-50 text-emerald-700',
    negative: 'border-rose-100 bg-rose-50 text-rose-700',
    neutral: 'border-slate-100 bg-slate-50 text-slate-900',
    warning: 'border-amber-100 bg-amber-50 text-amber-700',
  };

  return (
    <article className={`rounded-[24px] border p-5 ${tones[tone]}`}>
      <p className="text-sm font-semibold uppercase tracking-[0.16em]">{label}</p>
      <p className="mt-4 text-3xl font-semibold">{value}</p>
    </article>
  );
}

function LandingCard({ title, body }: { title: string; body: string }) {
  return (
    <article className="rounded-[28px] border border-emerald-100 bg-white p-6 shadow-[0_18px_60px_rgba(15,23,42,0.06)]">
      <h4 className="text-xl font-semibold text-slate-900">{title}</h4>
      <p className="mt-4 text-sm leading-7 text-slate-600">{body}</p>
    </article>
  );
}

function FeatureChip({
  label,
  helper,
  dark = false,
}: {
  label: string;
  helper: string;
  dark?: boolean;
}) {
  return (
    <div className={`rounded-[22px] border px-4 py-4 ${dark ? 'border-white/10 bg-white/5 text-white' : 'border-emerald-100 bg-white text-slate-900'}`}>
      <p className={`font-semibold ${dark ? 'text-white' : 'text-slate-900'}`}>{label}</p>
      <p className={`mt-2 text-sm ${dark ? 'text-slate-300' : 'text-slate-600'}`}>{helper}</p>
    </div>
  );
}

function LandingStat({
  title,
  value,
  tone = 'positive',
}: {
  title: string;
  value: string;
  tone?: 'positive' | 'negative';
}) {
  return (
    <div className="rounded-[22px] border border-white/10 bg-white/5 p-4">
      <p className="text-sm text-slate-300">{title}</p>
      <p className={`mt-3 text-2xl font-semibold ${tone === 'positive' ? 'text-emerald-300' : 'text-rose-300'}`}>{value}</p>
    </div>
  );
}

function InfoStrip({
  label,
  helper,
  value,
}: {
  label: string;
  helper: string;
  value: string;
}) {
  return (
    <div className="rounded-[22px] border border-slate-100 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-emerald-600">{label}</p>
      <p className="mt-2 text-xl font-semibold text-slate-900">{value}</p>
      <p className="mt-2 text-sm leading-7 text-slate-500">{helper}</p>
    </div>
  );
}

function ProgressBar({
  label,
  value,
  total,
  tone,
}: {
  label: string;
  value: number;
  total: number;
  tone: 'positive' | 'negative';
}) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-sm">
        <span className="font-semibold text-slate-700">{label}</span>
        <span className={tone === 'positive' ? 'text-emerald-600' : 'text-rose-600'}>{formatCurrency(value)}</span>
      </div>
      <div className="h-3 rounded-full bg-white">
        <div
          className={`h-3 rounded-full ${tone === 'positive' ? 'bg-emerald-500' : 'bg-rose-500'}`}
          style={{ width: `${Math.max((value / total) * 100, 8)}%` }}
        />
      </div>
    </div>
  );
}

function ComparisonCard({
  title,
  subtitle,
  summary,
}: {
  title: string;
  subtitle: string;
  summary: MonthlyAnalysisResponse['comparativoMesAnterior'];
}) {
  return (
    <SectionCard title={title}>
      <p className="text-sm text-slate-500">{subtitle}</p>
      <div className="mt-5 grid gap-3">
        <TrendRow label="Receitas" tone={summary.tendenciaReceitas} value={summary.diferencaReceitas} />
        <TrendRow label="Despesas" tone={summary.tendenciaDespesas} value={summary.diferencaDespesas} inverse />
        <TrendRow label="Saldo" tone={summary.tendenciaSaldo} value={summary.diferencaSaldo} />
      </div>
      <div className="mt-5 rounded-[22px] bg-slate-50 p-4 text-sm leading-7 text-slate-600">
        Tendência geral do período: <span className="font-semibold text-slate-900">{summary.tendenciaGeral.toLowerCase()}</span>.
      </div>
    </SectionCard>
  );
}

function TrendRow({
  label,
  value,
  tone,
  inverse = false,
}: {
  label: string;
  value: number;
  tone: 'MELHOR' | 'PIOR' | 'IGUAL';
  inverse?: boolean;
}) {
  const color = tone === 'MELHOR' ? 'text-emerald-600' : tone === 'PIOR' ? 'text-rose-600' : 'text-slate-500';
  const trendLabel = inverse && tone === 'MELHOR' ? 'Menos gasto' : tone;

  return (
    <div className="flex items-center justify-between rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4">
      <div>
        <p className="font-semibold text-slate-900">{label}</p>
        <p className="mt-1 text-sm text-slate-500">Comparação automática com a referência</p>
      </div>
      <div className="text-right">
        <p className={`font-semibold ${color}`}>{value >= 0 ? '+' : ''}{formatCurrency(value)}</p>
        <p className="mt-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{trendLabel}</p>
      </div>
    </div>
  );
}

function ExpenseHighlight({
  transaction,
}: {
  transaction: Pick<TransactionResponse, 'description' | 'amount' | 'category' | 'transactionDate'>;
}) {
  return (
    <div className="rounded-[22px] bg-slate-50 p-5">
      <p className="text-lg font-semibold text-slate-900">{transaction.description}</p>
      <p className="mt-2 text-sm text-slate-500">{categoryLabels[transaction.category]}</p>
      <p className="mt-4 text-2xl font-semibold text-rose-600">{formatCurrency(transaction.amount)}</p>
      <p className="mt-3 text-sm text-slate-500">
        {new Date(`${transaction.transactionDate}T12:00:00`).toLocaleDateString('pt-BR')}
      </p>
    </div>
  );
}

function Tag({
  children,
  tone,
}: {
  children: ReactNode;
  tone: 'positive' | 'negative' | 'warning' | 'neutral';
}) {
  const styles = {
    positive: 'bg-emerald-100 text-emerald-700',
    negative: 'bg-rose-100 text-rose-700',
    warning: 'bg-amber-100 text-amber-700',
    neutral: 'bg-slate-100 text-slate-700',
  };

  return <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] ${styles[tone]}`}>{children}</span>;
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-slate-700">{label}</span>
      {children}
    </label>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<{ value: string; label: string }>;
  onChange: (value: string) => void;
}) {
  return (
    <Field label={label}>
      <select
        className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </Field>
  );
}

function EmptyState({ label }: { label: string }) {
  return (
    <div className="rounded-[24px] border border-dashed border-slate-200 bg-white p-6 text-center text-sm leading-7 text-slate-500">
      {label}
    </div>
  );
}

function ChecklistItem({ checked, label }: { checked: boolean; label: string }) {
  return (
    <div className="flex items-start gap-3 rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4">
      <div className={`mt-1 h-5 w-5 rounded-full ${checked ? 'bg-emerald-500' : 'bg-slate-300'}`} />
      <p className="text-sm leading-7 text-slate-600">{label}</p>
    </div>
  );
}

function PreferenceItem({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4">
      <p className="font-semibold text-slate-900">{title}</p>
      <p className="mt-2 text-sm leading-7 text-slate-600">{description}</p>
    </div>
  );
}

function ToastStack({ toasts }: { toasts: ToastMessage[] }) {
  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="fixed bottom-24 right-4 z-50 grid w-[min(380px,calc(100vw-2rem))] gap-3">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`rounded-[22px] border px-4 py-4 shadow-[0_20px_60px_rgba(15,23,42,0.14)] ${
            toast.tone === 'success'
              ? 'border-emerald-100 bg-white text-slate-900'
              : 'border-sky-100 bg-white text-slate-900'
          }`}
        >
          <p className={`text-xs font-semibold uppercase tracking-[0.16em] ${toast.tone === 'success' ? 'text-emerald-600' : 'text-sky-600'}`}>
            {toast.tone === 'success' ? 'Tudo certo' : 'Aviso'}
          </p>
          <p className="mt-2 text-sm leading-7 text-slate-600">{toast.message}</p>
        </div>
      ))}
    </div>
  );
}

function buildTransactionDraft(type: 'RECEITA' | 'DESPESA'): TransactionDraft {
  return {
    type,
    description: '',
    amount: '',
    paymentMethod: 'PIX',
    transactionDate: new Date().toISOString().slice(0, 10),
    category: 'OUTROS',
    notes: '',
  };
}

function historyTone(actionType: WishlistHistoryResponse['actionType']): 'positive' | 'negative' | 'warning' | 'neutral' {
  if (actionType === 'COMPRADO') {
    return 'positive';
  }

  if (actionType === 'COMPRA_DESFEITA') {
    return 'warning';
  }

  if (actionType === 'ATUALIZADO') {
    return 'neutral';
  }

  return 'warning';
}

export default App;
