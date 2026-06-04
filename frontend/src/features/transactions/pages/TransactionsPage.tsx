import { Category, TransactionResponse } from '../../../types';
import { categoryLabels, formatCurrency, paymentMethodLabels } from '../../../lib/mockFinance';
import { EmptyState, Field, LoadingCard, SectionCard, SelectField, Tag, UnavailableCard } from '../../shared/ui';

type TransactionsPageProps = {
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
};

export default function TransactionsPage({
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
}: TransactionsPageProps) {
  const getPaymentLabel = (
    paymentMethod: TransactionResponse['paymentMethod'],
    installments: TransactionResponse['installments'],
  ) => {
    const baseLabel = paymentMethodLabels[paymentMethod];
    if (paymentMethod === 'CARTAO_CREDITO_PARCELADO' && installments && installments > 1) {
      return `${baseLabel} · ${installments}x`;
    }

    return baseLabel;
  };

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
                <Tag tone={transaction.type === 'RECEITA' ? 'positive' : 'negative'}>
                  {transaction.type === 'RECEITA' ? 'Receita' : 'Despesa'}
                </Tag>
                <span className="text-sm text-slate-600">{categoryLabels[transaction.category]}</span>
                <span className="text-sm text-slate-600">{getPaymentLabel(transaction.paymentMethod, transaction.installments)}</span>
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
