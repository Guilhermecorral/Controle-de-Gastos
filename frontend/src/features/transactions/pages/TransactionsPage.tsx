import { Category, TransactionResponse } from '../../../types';
import { categoryLabels, formatCurrency, formatIsoDate, paymentMethodLabels } from '../../../lib/mockFinance';
import { EmptyState, Field, LoadingCard, SectionCard, SelectField, Tag, UnavailableCard } from '../../shared/ui';

type TransactionsPageProps = {
  transactions: TransactionResponse[];
  search: string;
  typeFilter: 'TODOS' | 'RECEITA' | 'DESPESA';
  categoryFilter: 'TODAS' | Category;
  hasError: boolean;
  isLoading: boolean;
  isDeleting: boolean;
  onSearchChange: (value: string) => void;
  onTypeFilterChange: (value: 'TODOS' | 'RECEITA' | 'DESPESA') => void;
  onCategoryFilterChange: (value: 'TODAS' | Category) => void;
  onOpenModal: () => void;
  onOpenReceipts: () => void;
  onUploadReceipt: (transaction: TransactionResponse) => void;
  onDeleteTransaction: (transaction: TransactionResponse) => void;
};

export default function TransactionsPage({
  transactions,
  search,
  typeFilter,
  categoryFilter,
  hasError,
  isLoading,
  isDeleting,
  onSearchChange,
  onTypeFilterChange,
  onCategoryFilterChange,
  onOpenModal,
  onOpenReceipts,
  onUploadReceipt,
  onDeleteTransaction,
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
      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
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
          <div className="mt-5 flex flex-wrap gap-3">
            <button
              className="button-pop rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
              onClick={onOpenModal}
              type="button"
            >
              Abrir modal de lançamento
            </button>
            <button
              className="button-pop rounded-full border border-emerald-200 bg-emerald-50 px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
              onClick={onOpenReceipts}
              type="button"
            >
              Ver notas fiscais
            </button>
          </div>
        </SectionCard>
      </section>

      <section className="fade-up rounded-[30px] border border-emerald-100/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.96),rgba(245,248,245,0.96))] p-6 shadow-[0_22px_70px_rgba(15,23,42,0.08)]">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Histórico</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">Entradas e saídas</h3>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">
              Aqui ficam as movimentações reais do período, com leitura direta de categoria, pagamento, valor e vínculo fiscal.
            </p>
          </div>
          <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-600">
            {transactions.length} registro(s)
          </span>
        </div>

        <div className="mt-6 overflow-hidden rounded-[24px] border border-slate-100">
          <div className="hidden grid-cols-[1fr_0.55fr_0.65fr_0.8fr_0.65fr_0.85fr_0.65fr_0.7fr] gap-3 bg-slate-50 px-5 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500 xl:grid">
            <span>Descrição</span>
            <span>Tipo</span>
            <span>Categoria</span>
            <span>Pagamento</span>
            <span>Data</span>
            <span>Nota fiscal</span>
            <span>Ações</span>
            <span className="text-right">Valor</span>
          </div>

          <div className="grid gap-px bg-slate-100">
            {transactions.length === 0 && (
              <div className="bg-white px-5 py-12">
                <EmptyState label="Nenhum lançamento apareceu com os filtros atuais. Ajuste a busca ou registre uma nova movimentação." />
              </div>
            )}

            {transactions.map((transaction) => (
              <div
                key={transaction.id}
                className="grid gap-4 bg-white px-5 py-4 transition hover:bg-slate-50/80 xl:grid-cols-[1fr_0.55fr_0.65fr_0.8fr_0.65fr_0.85fr_0.65fr_0.7fr] xl:items-center"
              >
                <div>
                  <p className="font-semibold text-slate-900">{transaction.description}</p>
                  <p className="mt-1 text-sm text-slate-500 xl:hidden">
                    {categoryLabels[transaction.category]} · {formatIsoDate(transaction.transactionDate)}
                  </p>
                </div>
                <Tag tone={transaction.type === 'RECEITA' ? 'positive' : 'negative'}>
                  {transaction.type === 'RECEITA' ? 'Receita' : 'Despesa'}
                </Tag>
                <span className="text-sm text-slate-600">{categoryLabels[transaction.category]}</span>
                <span className="text-sm text-slate-600">{getPaymentLabel(transaction.paymentMethod, transaction.installments)}</span>
                <span className="text-sm text-slate-600">{formatIsoDate(transaction.transactionDate)}</span>
                <div className="flex flex-col items-start gap-2">
                  <span className="text-xs text-slate-500">
                    {transaction.receipt ? `Anexada em ${formatIsoDate(transaction.receipt.uploadedAt.slice(0, 10))}` : 'Sem anexo'}
                  </span>
                  <button
                    className={`rounded-full px-3 py-2 text-xs font-semibold transition ${
                      transaction.receipt
                        ? 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                        : 'bg-emerald-500 text-white hover:bg-emerald-600'
                    }`}
                    onClick={() => onUploadReceipt(transaction)}
                    type="button"
                  >
                    {transaction.receipt ? 'Trocar nota' : 'Anexar nota'}
                  </button>
                </div>
                <div className="flex items-start">
                  <button
                    className="button-pop rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={isDeleting}
                    onClick={() => onDeleteTransaction(transaction)}
                    type="button"
                  >
                    {isDeleting ? 'Apagando...' : 'Apagar'}
                  </button>
                </div>
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
