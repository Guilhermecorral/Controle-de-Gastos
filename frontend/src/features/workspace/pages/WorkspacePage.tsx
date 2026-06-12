import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import { Category, TransactionResponse, WishlistItemResponse } from '../../../types';
import api from '../../../lib/api';
import { formatMonthLabel, getSuggestedCategory } from '../../../lib/mockFinance';
import {
  useCreateTransactionMutation,
  useCreateWishlistItemMutation,
  useDashboardQuery,
  useDeleteTransactionMutation,
  useLogoutMutation,
  useMonthlyAnalysisQuery,
  usePurchaseWishlistItemMutation,
  useTransactionReceiptsQuery,
  useTransactionsQuery,
  useUndoWishlistPurchaseMutation,
  useUploadTransactionReceiptMutation,
  useWishlistHistoryQuery,
  useWishlistItemsQuery,
  useWishlistListsQuery,
  useWishlistSummaryQuery,
} from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { useAuthStore } from '../../../store/auth';
import DashboardPage from '../../dashboard/pages/DashboardPage';
import MonthlyAnalysisPage from '../../monthly-analysis/pages/MonthlyAnalysisPage';
import ReceiptsPage from '../../receipts/pages/ReceiptsPage';
import SettingsPage from '../../settings/pages/SettingsPage';
import { ToastStack } from '../../shared/ui';
import ReceiptUploadModal from '../../transactions/components/ReceiptUploadModal';
import TransactionsPage from '../../transactions/pages/TransactionsPage';
import WishlistPage from '../../wishlist/pages/WishlistPage';
import { currentMonth, currentYear, monthOptions, navItems, onboardingKey, yearOptions } from '../constants';
import PurchaseModal from '../components/PurchaseModal';
import TransactionModal from '../components/TransactionModal';
import WorkspaceHero from '../components/WorkspaceHero';
import {
  buildTransactionDraft,
  PurchaseDraft,
  ToastMessage,
  ToastTone,
  TransactionDraft,
  ViewId,
  WishlistDraft,
} from '../types';

type WorkspacePageProps = {
  onLogout: () => void;
};

export default function WorkspacePage({ onLogout }: WorkspacePageProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const [currentView, setCurrentView] = useState<ViewId>('painel');
  const [dashboardYear, setDashboardYear] = useState(currentYear);
  const [dashboardMonth, setDashboardMonth] = useState(currentMonth);
  const [analysisYear, setAnalysisYear] = useState(currentYear);
  const [analysisMonth, setAnalysisMonth] = useState(currentMonth);
  const [receiptsYear, setReceiptsYear] = useState(currentYear);
  const [receiptsMonth, setReceiptsMonth] = useState(currentMonth);
  const [transactionSearch, setTransactionSearch] = useState('');
  const [transactionTypeFilter, setTransactionTypeFilter] = useState<'TODOS' | 'RECEITA' | 'DESPESA'>('TODOS');
  const [transactionCategoryFilter, setTransactionCategoryFilter] = useState<'TODAS' | Category>('TODAS');
  const [wishlistStatusFilter, setWishlistStatusFilter] = useState<'TODOS' | 'PENDENTE' | 'COMPRADO'>('TODOS');
  const [wishlistListFilter, setWishlistListFilter] = useState<'TODAS' | string>('TODAS');
  const [transactionModalOpen, setTransactionModalOpen] = useState(false);
  const [transactionReceiptFile, setTransactionReceiptFile] = useState<File | null>(null);
  const [receiptModalTransaction, setReceiptModalTransaction] = useState<TransactionResponse | null>(null);
  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [receiptsSelectedTransactionId, setReceiptsSelectedTransactionId] = useState('');
  const [receiptsSelectedFile, setReceiptsSelectedFile] = useState<File | null>(null);
  const [purchaseModalItemId, setPurchaseModalItemId] = useState<number | null>(null);
  const [purchaseReceiptFile, setPurchaseReceiptFile] = useState<File | null>(null);
  const [historyItemId, setHistoryItemId] = useState<number | null>(null);
  const [onboardingDismissed, setOnboardingDismissed] = useState(false);
  const [transactionCategoryTouched, setTransactionCategoryTouched] = useState(false);
  const [wishlistCategoryTouched, setWishlistCategoryTouched] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
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
  const allTransactionsQuery = useTransactionsQuery({ enabled: true });
  const transactionsQuery = useTransactionsQuery({
    type: transactionTypeFilter,
    category: transactionCategoryFilter,
    enabled: true,
  });
  const monthlyAnalysisQuery = useMonthlyAnalysisQuery(analysisYear, analysisMonth);
  const receiptsQuery = useTransactionReceiptsQuery(receiptsYear, receiptsMonth);
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
  const deleteTransactionMutation = useDeleteTransactionMutation();
  const uploadTransactionReceiptMutation = useUploadTransactionReceiptMutation();
  const createWishlistItemMutation = useCreateWishlistItemMutation();
  const purchaseWishlistItemMutation = usePurchaseWishlistItemMutation();
  const undoWishlistPurchaseMutation = useUndoWishlistPurchaseMutation();
  const logoutMutation = useLogoutMutation();

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

  const transactionSuggestion = useMemo(() => getSuggestedCategory(transactionDraft.description), [transactionDraft.description]);
  const wishlistSuggestion = useMemo(() => getSuggestedCategory(wishlistDraft.description), [wishlistDraft.description]);

  const dashboardSnapshot = dashboardQuery.data ?? null;
  const monthlySnapshot = monthlyAnalysisQuery.data ?? null;
  const allTransactions = allTransactionsQuery.data ?? [];
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
          transaction.category.toLowerCase().includes(normalizedSearch)
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

    const selectedReceipt = transactionReceiptFile;

    createTransactionMutation.mutate(
      {
        type: transactionDraft.type,
        description: transactionDraft.description.trim(),
        category: transactionDraft.category,
        amount: parsedAmount,
        paymentMethod: transactionDraft.paymentMethod,
        installments: transactionDraft.paymentMethod === 'CARTAO_CREDITO_PARCELADO' ? transactionDraft.installments : 1,
        transactionDate: transactionDraft.transactionDate,
      },
      {
        onSuccess: (createdTransaction) => {
          const finishSuccess = () => {
            setTransactionDraft(buildTransactionDraft(transactionDraft.type));
            setTransactionReceiptFile(null);
            setTransactionCategoryTouched(false);
            setTransactionModalOpen(false);
            pushToast(
              transactionDraft.type === 'RECEITA'
                ? 'Receita adicionada ao histórico.'
                : 'Despesa adicionada ao histórico.',
            );
          };

          if (!selectedReceipt) {
            finishSuccess();
            return;
          }

          uploadTransactionReceiptMutation.mutate(
            {
              id: createdTransaction.id,
              file: selectedReceipt,
            },
            {
              onSuccess: () => {
                finishSuccess();
                pushToast('Nota fiscal anexada junto com o lançamento.');
              },
              onError: (error) => {
                finishSuccess();
                pushToast(getApiErrorMessage(error, 'A transação foi criada, mas não conseguimos anexar a nota fiscal.'), 'info');
              },
            },
          );
        },
        onError: () => {
          pushToast('Não foi possível salvar a transação agora.', 'info');
        },
      },
    );
  };

  const handleUploadReceipt = () => {
    if (!receiptModalTransaction || !receiptFile) {
      return;
    }

    uploadTransactionReceiptMutation.mutate(
      {
        id: receiptModalTransaction.id,
        file: receiptFile,
      },
      {
        onSuccess: () => {
          setReceiptFile(null);
          setReceiptModalTransaction(null);
          pushToast('Nota fiscal salva e vinculada à transação.');
        },
        onError: (error) => {
          pushToast(getApiErrorMessage(error, 'Não foi possível salvar a nota fiscal agora.'), 'info');
        },
      },
    );
  };

  const handleUploadReceiptFromReceiptsView = () => {
    if (!receiptsSelectedTransactionId || !receiptsSelectedFile) {
      return;
    }

    uploadTransactionReceiptMutation.mutate(
      {
        id: Number(receiptsSelectedTransactionId),
        file: receiptsSelectedFile,
      },
      {
        onSuccess: () => {
          setReceiptsSelectedTransactionId('');
          setReceiptsSelectedFile(null);
          pushToast('Nota fiscal salva a partir da aba fiscal.');
        },
        onError: (error) => {
          pushToast(getApiErrorMessage(error, 'Não foi possível salvar a nota fiscal agora.'), 'info');
        },
      },
    );
  };

  const handleDownloadReceipt = async (transactionId: number, filename: string) => {
    try {
      const response = await api.get(`/transactions/${transactionId}/receipt/download`, {
        responseType: 'blob',
      });
      const blobUrl = window.URL.createObjectURL(response.data);
      const anchor = document.createElement('a');
      anchor.href = blobUrl;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(blobUrl);
    } catch (error) {
      pushToast(getApiErrorMessage(error, 'Não foi possível baixar a nota fiscal agora.'), 'info');
    }
  };

  const handleDeleteTransaction = (transaction: TransactionResponse) => {
    const isInstallmentGroup =
      transaction.paymentMethod === 'CARTAO_CREDITO_PARCELADO' &&
      (transaction.installments ?? 1) > 1;

    const confirmed = window.confirm(
      isInstallmentGroup
        ? `Essa compra parcelada possui ${transaction.installments} parcelas. Apagar esta transação removerá o grupo inteiro. Deseja continuar?`
        : `Deseja apagar a transação "${transaction.description}"?`,
    );

    if (!confirmed) {
      return;
    }

    deleteTransactionMutation.mutate(transaction.id, {
      onSuccess: () => {
        if (receiptModalTransaction?.id === transaction.id) {
          setReceiptModalTransaction(null);
          setReceiptFile(null);
        }

        pushToast(
          isInstallmentGroup
            ? 'Grupo parcelado removido do histórico.'
            : 'Transação removida do histórico.',
          'info',
        );
      },
      onError: (error) => {
        pushToast(getApiErrorMessage(error, 'Não foi possível apagar a transação agora.'), 'info');
      },
    });
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
        onError: (error) => {
          pushToast(getApiErrorMessage(error, 'Não foi possível criar o item agora.'), 'info');
        },
      },
    );
  };

  const handlePurchaseWishlistItem = () => {
    if (!purchaseModalItem) {
      return;
    }

    const selectedReceipt = purchaseReceiptFile;

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
        onSuccess: (purchasedItem) => {
          const finishSuccess = () => {
            setHistoryItemId(purchaseModalItem.id);
            setPurchaseDraft({
              purchaseDate: new Date().toISOString().slice(0, 10),
              paymentMethod: 'PIX',
              installments: 1,
              firstInstallmentNextMonth: false,
            });
            setPurchaseReceiptFile(null);
            setPurchaseModalItemId(null);
            pushToast('Compra concluída e lançamentos gerados.');
          };

          if (!selectedReceipt || !purchasedItem.linkedTransactionId) {
            finishSuccess();
            return;
          }

          uploadTransactionReceiptMutation.mutate(
            {
              id: purchasedItem.linkedTransactionId,
              file: selectedReceipt,
            },
            {
              onSuccess: () => {
                finishSuccess();
                pushToast('Nota fiscal anexada junto com a compra.');
              },
              onError: (error) => {
                finishSuccess();
                pushToast(getApiErrorMessage(error, 'A compra foi concluída, mas não conseguimos anexar a nota fiscal.'), 'info');
              },
            },
          );
        },
        onError: (error) => {
          pushToast(getApiErrorMessage(error, 'Não foi possível concluir a compra agora.'), 'info');
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
      onError: (error) => {
        pushToast(getApiErrorMessage(error, 'Não foi possível desfazer a compra agora.'), 'info');
      },
    });
  };

  const handleOpenReceiptForWishlistItem = (item: WishlistItemResponse) => {
    if (!item.linkedTransactionId) {
      pushToast('Esse item ainda não possui transação vinculada para receber nota fiscal.', 'info');
      return;
    }

    const linkedTransaction = allTransactions.find((transaction) => transaction.id === item.linkedTransactionId);
    if (!linkedTransaction) {
      pushToast('A transação da compra ainda não apareceu na listagem atual. Tente novamente em instantes.', 'info');
      return;
    }

    setReceiptFile(null);
    setReceiptModalTransaction(linkedTransaction);
  };

  const handleWorkspaceLogout = () => {
    logoutMutation.mutate(undefined, {
      onSettled: () => {
        queryClient.clear();
        setUserMenuOpen(false);
        onLogout();
      },
    });
  };

  return (
    <div className="ambient-grid ambient-noise min-h-screen overflow-x-hidden bg-[#f4f6f1] text-slate-900">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[360px] bg-[radial-gradient(circle_at_12%_0%,rgba(16,185,129,0.15),transparent_38%),radial-gradient(circle_at_88%_10%,rgba(45,212,191,0.1),transparent_26%)]" />

      <div className="relative z-40 border-b border-emerald-100 bg-white/82 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500 text-lg font-bold text-white shadow-lg shadow-emerald-500/30">
              FF
            </div>
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-emerald-600">Farol Financeiro</p>
              <h1 className="text-xl font-semibold text-slate-900">Seu ambiente financeiro</h1>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              className="button-pop rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
              onClick={() => {
                setTransactionDraft(buildTransactionDraft('RECEITA'));
                setTransactionReceiptFile(null);
                setTransactionCategoryTouched(false);
                setTransactionModalOpen(true);
              }}
              type="button"
            >
              Nova receita
            </button>
            <button
              className="button-pop rounded-full border border-rose-200 bg-rose-50 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-100"
              onClick={() => {
                setTransactionDraft(buildTransactionDraft('DESPESA'));
                setTransactionReceiptFile(null);
                setTransactionCategoryTouched(false);
                setTransactionModalOpen(true);
              }}
              type="button"
            >
              Nova despesa
            </button>
            <div className="relative z-[70]">
              <button
                className="button-pop flex h-11 w-11 items-center justify-center rounded-full border border-slate-200 bg-white/92 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                onClick={() => setUserMenuOpen((currentValue) => !currentValue)}
                type="button"
              >
                {(user?.name ?? 'U').slice(0, 1).toUpperCase()}
              </button>

              {userMenuOpen && (
                <div className="absolute right-0 top-[calc(100%+0.75rem)] z-[80] w-72 rounded-[24px] border border-slate-200 bg-white p-3 shadow-[0_28px_80px_rgba(15,23,42,0.2)]">
                  <div className="rounded-[18px] bg-slate-50 px-4 py-3">
                    <p className="text-sm font-semibold text-slate-900">{user?.name ?? 'Usuário'}</p>
                    <p className="mt-1 text-xs leading-6 text-slate-500">{user?.email ?? 'sem e-mail carregado'}</p>
                  </div>
                  <div className="mt-3 grid gap-2">
                    <button
                      className="rounded-[18px] bg-white px-4 py-3 text-left text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                      onClick={() => {
                        setCurrentView('configuracoes');
                        setUserMenuOpen(false);
                      }}
                      type="button"
                    >
                      Configurações
                    </button>
                    <button
                      className="rounded-[18px] bg-rose-50 px-4 py-3 text-left text-sm font-semibold text-rose-700 transition hover:bg-rose-100"
                      onClick={handleWorkspaceLogout}
                      type="button"
                    >
                      Sair da conta
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="relative z-10 mx-auto grid max-w-7xl gap-8 px-4 py-8 lg:grid-cols-[260px_minmax(0,1fr)] lg:px-8">
        <aside className="glass-panel rounded-[28px] p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">Navegação</p>
          <nav className="mt-4 grid gap-2">
            {navItems.map((item) => (
              <button
                key={item.id}
                className={`card-lift rounded-[22px] px-4 py-4 text-left transition ${
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
              Aqui você acompanha seus números, seus lançamentos e seus desejos de compra em um só lugar.
            </p>
          </div>
        </aside>

        <main className="space-y-6">
          <WorkspaceHero currentView={currentView}>
            <div className="grid gap-3 rounded-[24px] border border-white/10 bg-white/8 p-4 text-sm text-slate-200 backdrop-blur-sm md:grid-cols-3">
              <div>
                <p className="font-semibold text-white">Período do painel</p>
                <p className="mt-1">{formatMonthLabel(dashboardYear, dashboardMonth)}</p>
              </div>
              <div>
                <p className="font-semibold text-white">Período da análise</p>
                <p className="mt-1">{formatMonthLabel(analysisYear, analysisMonth)}</p>
              </div>
              <div>
                <p className="font-semibold text-white">Período fiscal</p>
                <p className="mt-1">{formatMonthLabel(receiptsYear, receiptsMonth)}</p>
              </div>
            </div>
          </WorkspaceHero>

          {currentView === 'painel' && (
            <DashboardPage
              snapshot={dashboardSnapshot}
              annualTransactions={allTransactions}
              wishlistSummary={wishlistSummary}
              hasError={dashboardQuery.isError}
              isLoading={dashboardQuery.isLoading}
              year={dashboardYear}
              month={dashboardMonth}
              onReferenceYearChange={setDashboardYear}
              onReferenceMonthChange={setDashboardMonth}
              onOpenWishlist={() => setCurrentView('wishlist')}
              onboardingDismissed={onboardingDismissed}
              sessionName={user?.name ?? 'Usuário'}
              onDismissOnboarding={dismissOnboarding}
              onOpenTransactions={() => setCurrentView('transacoes')}
            />
          )}

          {currentView === 'transacoes' && (
            <TransactionsPage
              transactions={filteredTransactions}
              search={transactionSearch}
              typeFilter={transactionTypeFilter}
              categoryFilter={transactionCategoryFilter}
              hasError={transactionsQuery.isError}
              isLoading={transactionsQuery.isLoading}
              isDeleting={deleteTransactionMutation.isPending}
              onSearchChange={setTransactionSearch}
              onTypeFilterChange={setTransactionTypeFilter}
              onCategoryFilterChange={setTransactionCategoryFilter}
              onOpenModal={() => setTransactionModalOpen(true)}
              onOpenReceipts={() => setCurrentView('notasFiscais')}
              onUploadReceipt={(transaction) => {
                setReceiptFile(null);
                setReceiptModalTransaction(transaction);
              }}
              onDeleteTransaction={handleDeleteTransaction}
            />
          )}

          {currentView === 'analise' && (
            <MonthlyAnalysisPage
              snapshot={monthlySnapshot}
              hasError={monthlyAnalysisQuery.isError}
              isLoading={monthlyAnalysisQuery.isLoading}
              year={analysisYear}
              month={analysisMonth}
              yearOptions={yearOptions}
              monthOptions={monthOptions}
              onYearChange={setAnalysisYear}
              onMonthChange={setAnalysisMonth}
            />
          )}

          {currentView === 'wishlist' && (
            <WishlistPage
              lists={wishlistLists}
              filteredItems={wishlistItems}
              summary={wishlistSummary}
              draft={wishlistDraft}
              suggestion={wishlistSuggestion}
              categoryTouched={wishlistCategoryTouched}
              currentStatusFilter={wishlistStatusFilter}
              currentListFilter={wishlistListFilter}
              hasError={wishlistItemsQuery.isError || wishlistListsQuery.isError}
              isLoading={wishlistItemsQuery.isLoading || wishlistListsQuery.isLoading || wishlistSummaryQuery.isLoading}
              onStatusFilterChange={setWishlistStatusFilter}
              onListFilterChange={setWishlistListFilter}
              onDraftChange={setWishlistDraft}
              onCreate={handleCreateWishlistItem}
              onMarkPurchased={(itemId) => {
                setPurchaseReceiptFile(null);
                setPurchaseModalItemId(itemId);
              }}
              onUndoPurchase={handleUndoPurchase}
              onOpenHistory={setHistoryItemId}
              onOpenReceiptForItem={handleOpenReceiptForWishlistItem}
              history={selectedWishlistHistory}
              setCategoryTouched={setWishlistCategoryTouched}
            />
          )}

          {currentView === 'notasFiscais' && (
            <ReceiptsPage
              receipts={receiptsQuery.data ?? []}
              availableTransactions={allTransactions}
              selectedTransactionId={receiptsSelectedTransactionId}
              selectedFile={receiptsSelectedFile}
              year={receiptsYear}
              month={receiptsMonth}
              yearOptions={yearOptions}
              monthOptions={monthOptions}
              hasError={receiptsQuery.isError}
              isLoading={receiptsQuery.isLoading}
              isUploading={uploadTransactionReceiptMutation.isPending}
              onYearChange={setReceiptsYear}
              onMonthChange={setReceiptsMonth}
              onSelectedTransactionChange={setReceiptsSelectedTransactionId}
              onSelectedFileChange={setReceiptsSelectedFile}
              onUploadReceipt={handleUploadReceiptFromReceiptsView}
              onDownloadReceipt={handleDownloadReceipt}
            />
          )}

          {currentView === 'configuracoes' && <SettingsPage onLogout={onLogout} user={user} />}
        </main>
      </div>

      <TransactionModal
        isOpen={transactionModalOpen}
        draft={transactionDraft}
        receiptFile={transactionReceiptFile}
        suggestion={transactionSuggestion}
        onDraftChange={setTransactionDraft}
        onReceiptFileChange={setTransactionReceiptFile}
        onDescriptionChange={handleTransactionDescriptionChange}
        onCategoryTouched={setTransactionCategoryTouched}
        onSubmit={handleCreateTransaction}
        onClose={() => {
          setTransactionReceiptFile(null);
          setTransactionModalOpen(false);
        }}
      />

      <ReceiptUploadModal
        transaction={receiptModalTransaction}
        file={receiptFile}
        isSubmitting={uploadTransactionReceiptMutation.isPending}
        onFileChange={setReceiptFile}
        onSubmit={handleUploadReceipt}
        onClose={() => {
          setReceiptFile(null);
          setReceiptModalTransaction(null);
        }}
      />

      <PurchaseModal
        isOpen={!!purchaseModalItem}
        item={purchaseModalItem}
        draft={purchaseDraft}
        receiptFile={purchaseReceiptFile}
        history={purchaseModalHistory}
        onDraftChange={setPurchaseDraft}
        onReceiptFileChange={setPurchaseReceiptFile}
        onSubmit={handlePurchaseWishlistItem}
        onClose={() => {
          setPurchaseReceiptFile(null);
          setPurchaseModalItemId(null);
        }}
      />

      <ToastStack toasts={toasts} />
    </div>
  );
}
