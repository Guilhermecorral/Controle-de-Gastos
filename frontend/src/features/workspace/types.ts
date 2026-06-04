import { Category, PaymentMethod, WishlistPriority, WishlistHistoryResponse } from '../../types';

export type ViewId = 'painel' | 'transacoes' | 'analise' | 'wishlist' | 'configuracoes';

export type TransactionDraft = {
  type: 'RECEITA' | 'DESPESA';
  description: string;
  amount: string;
  paymentMethod: PaymentMethod;
  installments: number;
  transactionDate: string;
  category: Category;
  notes: string;
};

export type WishlistDraft = {
  description: string;
  notes: string;
  originalPrice: string;
  discountPercent: string;
  priority: WishlistPriority;
  category: Category;
  listId: string;
};

export type PurchaseDraft = {
  purchaseDate: string;
  paymentMethod: PaymentMethod;
  installments: number;
  firstInstallmentNextMonth: boolean;
};

export type ToastTone = 'success' | 'info';

export type ToastMessage = {
  id: number;
  message: string;
  tone: ToastTone;
};

export function buildTransactionDraft(type: 'RECEITA' | 'DESPESA'): TransactionDraft {
  return {
    type,
    description: '',
    amount: '',
    paymentMethod: 'PIX',
    installments: 1,
    transactionDate: new Date().toISOString().slice(0, 10),
    category: 'OUTROS',
    notes: '',
  };
}

export function historyTone(actionType: WishlistHistoryResponse['actionType']): 'positive' | 'negative' | 'warning' | 'neutral' {
  if (actionType === 'PURCHASED') {
    return 'positive';
  }

  if (actionType === 'PURCHASE_UNDONE') {
    return 'warning';
  }

  if (actionType === 'UPDATED' || actionType === 'MOVED') {
    return 'neutral';
  }

  return 'warning';
}

export function historyLabel(actionType: WishlistHistoryResponse['actionType']) {
  switch (actionType) {
    case 'CREATED':
      return 'Criado';
    case 'UPDATED':
      return 'Atualizado';
    case 'MOVED':
      return 'Movido';
    case 'PURCHASED':
      return 'Comprado';
    case 'PURCHASE_UNDONE':
      return 'Compra desfeita';
    case 'DELETED':
      return 'Excluído';
    default:
      return actionType;
  }
}
