import { Dispatch, SetStateAction } from 'react';
import { Category, WishlistHistoryResponse, WishlistItemResponse, WishlistListResponse, WishlistPriority } from '../../../types';
import { calculateFinalPrice, categoryLabels, getSuggestedCategory, priorityLabels, formatCurrency } from '../../../lib/mockFinance';
import { EmptyState, Field, LoadingCard, MetricCard, SectionCard, SelectField, Tag, UnavailableCard } from '../../shared/ui';
import { historyLabel, historyTone, WishlistDraft } from '../../workspace/types';

type WishlistPageProps = {
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
  onOpenReceiptForItem: (item: WishlistItemResponse) => void;
  history: WishlistHistoryResponse[];
  setCategoryTouched: (value: boolean) => void;
};

export default function WishlistPage({
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
  onOpenReceiptForItem,
  history,
  setCategoryTouched,
}: WishlistPageProps) {
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
                    <>
                      <button
                        className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                        onClick={() => onUndoPurchase(item.id)}
                        type="button"
                      >
                        Desfazer compra
                      </button>
                      {item.linkedTransactionId && (
                        <button
                          className="rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
                          onClick={() => onOpenReceiptForItem(item)}
                          type="button"
                        >
                          Anexar nota da compra
                        </button>
                      )}
                    </>
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
                  <Tag tone={historyTone(entry.actionType)}>{historyLabel(entry.actionType)}</Tag>
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
