import { Component, ErrorInfo, ReactNode } from 'react'

type AppErrorBoundaryProps = {
  children: ReactNode
}

type AppErrorBoundaryState = {
  hasError: boolean
}

// Protege a aplicacao contra tela branca total quando um componente quebra em producao.
export default class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  constructor(props: AppErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[AppErrorBoundary]', error, info)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <div className="min-h-screen bg-slate-950 px-6 py-16 text-slate-50">
        <div className="mx-auto flex min-h-[70vh] max-w-3xl items-center justify-center">
          <div className="glass-panel card-lift w-full max-w-xl rounded-[2rem] border border-emerald-300/20 bg-slate-950/90 p-10 text-center shadow-[0_32px_120px_rgba(15,23,42,0.55)]">
            <p className="text-xs font-semibold uppercase tracking-[0.4em] text-emerald-300">
              Farol Financeiro
            </p>
            <h1 className="mt-4 text-4xl font-semibold text-white">
              Algo saiu do esperado nesta tela.
            </h1>
            <p className="mt-4 text-base leading-8 text-slate-300">
              A sessao continua protegida, mas esta area precisa ser carregada novamente para voltar ao fluxo normal.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
              <button
                type="button"
                onClick={this.handleReload}
                className="button-pop rounded-full bg-emerald-400 px-6 py-3 text-sm font-semibold text-slate-950 shadow-[0_18px_45px_rgba(16,185,129,0.28)] transition hover:-translate-y-0.5"
              >
                Recarregar app
              </button>
              <a
                href="/app"
                className="rounded-full border border-white/15 px-6 py-3 text-sm font-semibold text-white transition hover:border-emerald-300/40 hover:text-emerald-200"
              >
                Voltar ao painel
              </a>
            </div>
          </div>
        </div>
      </div>
    )
  }
}
