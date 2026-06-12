import { Link } from 'react-router-dom';
import { formatCurrency } from '../../../lib/mockFinance';
import { FeatureChip, LandingCard, LandingStat } from '../../shared/ui';

type LandingPageProps = {
  isLoggedIn: boolean;
};

export default function LandingPage({ isLoggedIn }: LandingPageProps) {
  return (
    <div className="ambient-grid ambient-noise min-h-screen overflow-hidden bg-[#f4f6f1] text-slate-900">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[440px] bg-[radial-gradient(circle_at_12%_16%,rgba(16,185,129,0.1),transparent_30%),radial-gradient(circle_at_85%_12%,rgba(45,212,191,0.08),transparent_24%)]" />

      <header className="relative z-10 border-b border-emerald-100/70 bg-white/82 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 lg:px-8">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-lg font-bold text-white shadow-lg shadow-emerald-500/30">
              FF
            </div>
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Farol Financeiro</p>
              <h1 className="text-xl font-semibold text-slate-900">Seu dinheiro com mais clareza e menos ansiedade.</h1>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link
              className="button-pop rounded-full border border-slate-200 bg-white/92 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
              to="/login"
            >
              Entrar
            </Link>
            <Link
              className="button-pop button-glow rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
              to={isLoggedIn ? '/app' : '/cadastro'}
            >
              {isLoggedIn ? 'Ir para o app' : 'Começar agora'}
            </Link>
          </div>
        </div>
      </header>

      <main className="relative z-10">
        <section className="mx-auto grid max-w-7xl gap-10 px-4 py-16 lg:grid-cols-[1.06fr_0.94fr] lg:px-8">
          <div className="hero-glow fade-up">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Organize sua vida financeira</p>
            <h2 className="title-gradient mt-4 text-5xl font-semibold leading-tight text-balance md:text-6xl">
              Um espaço para entender o mês, planejar compras e decidir melhor.
            </h2>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
              Tenha uma leitura clara do que entrou, do que saiu e do que faz sentido comprar agora, sem se perder em telas confusas.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                className="button-pop rounded-full bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                to={isLoggedIn ? '/app' : '/cadastro'}
              >
                {isLoggedIn ? 'Abrir meu ambiente' : 'Criar conta agora'}
              </Link>
              <Link
                className="button-pop rounded-full border border-slate-200 bg-white/92 px-6 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                to="/login"
              >
                Ver área logada
              </Link>
            </div>

            <div className="mt-10 grid gap-4 sm:grid-cols-3">
              <FeatureChip label="Painel objetivo" helper="Entradas, saídas e acumulado do período." />
              <FeatureChip label="Análise mensal" helper="Comparações para perceber melhora e tendência." />
              <FeatureChip label="Lista de desejos" helper="Compra planejada ligada ao financeiro real." />
            </div>
          </div>

          <div className="hero-glow fade-up rounded-[36px] border border-slate-800 bg-slate-950 p-6 text-white shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-300">Visão do produto</p>
            <div className="mt-5 grid gap-4">
              <LandingStat title="Receitas do mês" value={formatCurrency(4200)} />
              <LandingStat title="Despesas do mês" value={formatCurrency(1462)} tone="negative" />
              <LandingStat title="Saldo do mês" value={formatCurrency(2738)} />
              <div className="rounded-[24px] border border-white/12 bg-white/8 p-5 backdrop-blur-sm">
                <p className="text-sm font-semibold text-white">O que você consegue acompanhar</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-200">
                  <li>O mês atual com leitura rápida das entradas, saídas e saldo.</li>
                  <li>Comparações para perceber se o momento está melhorando ou apertando.</li>
                  <li>Uma lista de desejos que ajuda a comprar com mais intenção.</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="bg-white/60 py-16 backdrop-blur-sm">
          <div className="mx-auto max-w-7xl px-4 lg:px-8">
            <div className="max-w-3xl">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Por que isso importa</p>
              <h3 className="mt-3 text-3xl font-semibold text-slate-900">Tudo foi pensado para ser claro, útil e fácil de acompanhar.</h3>
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
                body="Traz contexto histórico para descobrir se o momento está melhorando, piorando ou apenas repetindo padrões."
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
            <div className="glass-panel card-lift rounded-[32px] p-6">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Recursos pensados para o dia a dia</p>
              <ul className="mt-5 grid gap-3 text-sm leading-7 text-slate-600">
                <li>Cadastro com orientação de senha forte e mensagens simples.</li>
                <li>Recuperação de senha para quando você precisar retomar o acesso.</li>
                <li>Escolha de cookies entre essenciais e opcionais.</li>
                <li>Proteções contra abuso em cadastro, login e recuperação.</li>
                <li>Privacidade explicada de forma direta, sem juridiquês desnecessário.</li>
              </ul>
            </div>

            <div className="hero-glow rounded-[32px] border border-emerald-300/40 bg-emerald-500 px-7 py-8 text-white shadow-[0_24px_70px_rgba(16,185,129,0.18)]">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-100">Comece pelo essencial</p>
              <h3 className="mt-4 text-3xl font-semibold text-balance">Entre, acompanhe seu momento e ajuste o que fizer sentido para a sua rotina.</h3>
              <p className="mt-5 max-w-2xl text-base leading-8 text-emerald-50">
                O objetivo é te ajudar a tomar boas decisões financeiras desde o primeiro acesso, sem excesso de informação nem passos desnecessários.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Link
                  className="button-pop rounded-full bg-white px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-50"
                  to={isLoggedIn ? '/app' : '/login'}
                >
                  Abrir área logada
                </Link>
                <Link
                  className="button-pop rounded-full border border-white/30 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
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
              <h3 className="mt-3 text-3xl font-semibold text-white">Você acompanha o presente, compara períodos e planeja compras com mais calma.</h3>
              <p className="mt-4 text-base leading-8 text-slate-300">
                Primeiro você entende o mês, depois compara períodos e por fim conecta desejos ao financeiro quando realmente compra algo.
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

        <footer className="bg-slate-900 text-slate-200">
          <div className="mx-auto max-w-7xl px-4 py-14 lg:px-8">
            <div className="grid gap-10 lg:grid-cols-4">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Sua conta</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
                  <li>Entrar e criar conta</li>
                  <li>Alterar nome, e-mail e senha</li>
                  <li>Recuperar acesso com segurança</li>
                  <li>Excluir conta quando quiser</li>
                </ul>
              </div>
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Privacidade</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
                  <li>Cookies essenciais para a sessão</li>
                  <li>Cookies opcionais com consentimento</li>
                  <li>Registros de proteção contra abuso</li>
                  <li>Transparência sobre dados tratados</li>
                </ul>
              </div>
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Produto</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
                  <li>Painel com visão do mês</li>
                  <li>Análise mensal comparativa</li>
                  <li>Lista de desejos com impacto financeiro</li>
                  <li>Categorias e histórico organizados</li>
                </ul>
              </div>
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-300">Ajuda</p>
                <ul className="mt-4 grid gap-3 text-sm leading-7 text-slate-300">
                  <li>Recuperação de senha</li>
                  <li>Dúvidas sobre privacidade</li>
                  <li>Configuração de cookies</li>
                  <li>Suporte para acesso e conta</li>
                </ul>
              </div>
            </div>

            <div className="mt-12 border-t border-white/10 pt-6 text-sm leading-7 text-slate-400">
              <p>Este produto trata dados necessários para autenticação, proteção da conta, organização financeira e preferências de uso.</p>
              <p className="mt-2">Cookies opcionais e medições de uso só devem ser ativados com o seu consentimento.</p>
            </div>
          </div>
        </footer>
      </main>
    </div>
  );
}
