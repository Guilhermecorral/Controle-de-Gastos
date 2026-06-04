import { Link } from 'react-router-dom';

export default function NotFoundPage() {
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
