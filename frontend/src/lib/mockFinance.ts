// Centraliza os dados e regras simuladas do frontend para refletir o comportamento esperado do produto.
export type Category =
  | 'ALIMENTACAO'
  | 'TRANSPORTE'
  | 'MORADIA'
  | 'SAUDE'
  | 'LAZER'
  | 'EDUCACAO'
  | 'COMPRAS'
  | 'OUTROS';

export type PaymentMethod =
  | 'PIX'
  | 'DINHEIRO'
  | 'CARTAO_DEBITO'
  | 'CARTAO_CREDITO_AVISTA'
  | 'CARTAO_CREDITO_PARCELADO';

export type TransactionType = 'RECEITA' | 'DESPESA';
export type WishlistStatus = 'PENDENTE' | 'COMPRADO';
export type WishlistPriority = 'BAIXO' | 'MEDIA' | 'ALTO';
export type Trend = 'MELHOR' | 'PIOR' | 'IGUAL';

export interface AppTransaction {
  id: number;
  type: TransactionType;
  description: string;
  category: Category;
  amount: number;
  paymentMethod: PaymentMethod;
  transactionDate: string;
  origin: 'MANUAL' | 'WISHLIST';
  wishlistItemId?: number;
  installmentLabel?: string;
}

export interface WishlistList {
  id: number;
  name: string;
  description: string;
  isDefault: boolean;
}

export interface WishlistHistoryEntry {
  id: number;
  itemId: number;
  actionType: 'CREATED' | 'UPDATED' | 'PURCHASED' | 'PURCHASE_UNDONE';
  message: string;
  createdAt: string;
}

export interface AppWishlistItem {
  id: number;
  description: string;
  notes: string;
  originalPrice: number;
  discountPercent: number;
  finalPrice: number;
  priority: WishlistPriority;
  category: Category;
  status: WishlistStatus;
  listId: number;
  purchaseDate?: string;
  paymentMethod?: PaymentMethod;
  installments?: number;
  firstInstallmentNextMonth?: boolean;
}

export interface DashboardSnapshot {
  receitasMesAtual: number;
  despesasMesAtual: number;
  resultadoMesAtual: number;
  saldoAcumulado: number;
  receitasAnoReferencia: number;
  despesasAnoReferencia: number;
  resultadoAnoReferencia: number;
  totalReceitasAcumuladas: number;
  totalDespesasAcumuladas: number;
  ultimasTransacoes: AppTransaction[];
  gastosPorCategoria: { category: Category; totalAmount: number }[];
}

export interface ComparisonSnapshot {
  year: number;
  month: number;
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  diferencaReceitas: number;
  diferencaDespesas: number;
  diferencaSaldo: number;
  tendenciaReceitas: Trend;
  tendenciaDespesas: Trend;
  tendenciaSaldo: Trend;
  tendenciaGeral: Trend;
}

export interface YearToDateSummary {
  year: number;
  monthLimit: number;
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
}

export interface MonthlyAnalysisSnapshot {
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  maiorGasto: AppTransaction | null;
  menorGasto: AppTransaction | null;
  gastosPorCategoria: { category: Category; totalAmount: number }[];
  comparativoMesAnterior: ComparisonSnapshot;
  comparativoMesmoMesAnoAnterior: ComparisonSnapshot;
  acumuladoAnoAtual: YearToDateSummary;
  comparativoAcumuladoAnoAnterior: {
    anoAtual: YearToDateSummary;
    anoAnterior: YearToDateSummary;
    diferencaReceitas: number;
    diferencaDespesas: number;
    diferencaSaldo: number;
    tendenciaReceitas: Trend;
    tendenciaDespesas: Trend;
    tendenciaSaldo: Trend;
    tendenciaGeral: Trend;
  };
}

export const categoryLabels: Record<Category, string> = {
  ALIMENTACAO: 'Alimentação',
  TRANSPORTE: 'Transporte',
  MORADIA: 'Moradia',
  SAUDE: 'Saúde',
  LAZER: 'Lazer',
  EDUCACAO: 'Educação',
  COMPRAS: 'Compras',
  OUTROS: 'Outros',
};

export const priorityLabels: Record<WishlistPriority, string> = {
  BAIXO: 'Baixa',
  MEDIA: 'Média',
  ALTO: 'Alta',
};

export const paymentMethodLabels: Record<PaymentMethod, string> = {
  PIX: 'Pix',
  DINHEIRO: 'Dinheiro',
  CARTAO_DEBITO: 'Cartão de débito',
  CARTAO_CREDITO_AVISTA: 'Cartão à vista',
  CARTAO_CREDITO_PARCELADO: 'Cartão parcelado',
};

export const wishlistLists: WishlistList[] = [
  { id: 1, name: 'Lista Principal', description: 'Tudo o que ainda está em decisão.', isDefault: true },
  { id: 2, name: 'Tecnologia', description: 'Equipamentos para trabalho e estudo.', isDefault: false },
  { id: 3, name: 'Casa', description: 'Melhorias do dia a dia e conforto.', isDefault: false },
];

export const initialTransactions: AppTransaction[] = [
  {
    id: 1,
    type: 'RECEITA',
    description: 'Salário principal',
    category: 'OUTROS',
    amount: 4200,
    paymentMethod: 'PIX',
    transactionDate: '2026-05-05',
    origin: 'MANUAL',
  },
  {
    id: 2,
    type: 'DESPESA',
    description: 'Aluguel',
    category: 'MORADIA',
    amount: 1400,
    paymentMethod: 'PIX',
    transactionDate: '2026-05-07',
    origin: 'MANUAL',
  },
  {
    id: 3,
    type: 'DESPESA',
    description: 'Lanche com amigos',
    category: 'ALIMENTACAO',
    amount: 62,
    paymentMethod: 'CARTAO_DEBITO',
    transactionDate: '2026-05-09',
    origin: 'MANUAL',
  },
  {
    id: 4,
    type: 'RECEITA',
    description: 'Freelance',
    category: 'OUTROS',
    amount: 850,
    paymentMethod: 'PIX',
    transactionDate: '2026-04-12',
    origin: 'MANUAL',
  },
  {
    id: 5,
    type: 'DESPESA',
    description: 'Consulta médica',
    category: 'SAUDE',
    amount: 180,
    paymentMethod: 'DINHEIRO',
    transactionDate: '2026-04-16',
    origin: 'MANUAL',
  },
  {
    id: 6,
    type: 'DESPESA',
    description: 'Uber faculdade',
    category: 'TRANSPORTE',
    amount: 94,
    paymentMethod: 'PIX',
    transactionDate: '2025-05-10',
    origin: 'MANUAL',
  },
  {
    id: 7,
    type: 'RECEITA',
    description: 'Salário ano passado',
    category: 'OUTROS',
    amount: 3600,
    paymentMethod: 'PIX',
    transactionDate: '2025-05-05',
    origin: 'MANUAL',
  },
];

export const initialWishlistItems: AppWishlistItem[] = [
  {
    id: 101,
    description: 'Notebook para trabalho',
    notes: 'Prioridade porque ajuda no estudo e freelas.',
    originalPrice: 5300,
    discountPercent: 8,
    finalPrice: 4876,
    priority: 'ALTO',
    category: 'EDUCACAO',
    status: 'PENDENTE',
    listId: 2,
  },
  {
    id: 102,
    description: 'Fone bluetooth',
    notes: 'Usar nas calls e no transporte.',
    originalPrice: 420,
    discountPercent: 10,
    finalPrice: 378,
    priority: 'MEDIA',
    category: 'COMPRAS',
    status: 'PENDENTE',
    listId: 2,
  },
  {
    id: 103,
    description: 'Air fryer',
    notes: 'Ajuda a economizar lanche na rua.',
    originalPrice: 520,
    discountPercent: 5,
    finalPrice: 494,
    priority: 'BAIXO',
    category: 'COMPRAS',
    status: 'COMPRADO',
    listId: 3,
    purchaseDate: '2026-05-03',
    paymentMethod: 'CARTAO_CREDITO_AVISTA',
    installments: 1,
  },
];

export const initialWishlistHistory: WishlistHistoryEntry[] = [
  {
    id: 1,
    itemId: 101,
    actionType: 'CREATED',
    message: 'Item criado na lista Tecnologia.',
    createdAt: '2026-05-01T09:00:00',
  },
  {
    id: 2,
    itemId: 101,
    actionType: 'UPDATED',
    message: 'Desconto atualizado para 8%.',
    createdAt: '2026-05-02T10:15:00',
  },
  {
    id: 3,
    itemId: 103,
    actionType: 'PURCHASED',
    message: 'Compra lançada à vista e vinculada ao financeiro.',
    createdAt: '2026-05-03T12:40:00',
  },
];

const suggestionRules: Array<{ keywords: string[]; category: Category }> = [
  { keywords: ['mercado', 'lanche', 'restaurante', 'ifood', 'padaria', 'pizza', 'hamburguer'], category: 'ALIMENTACAO' },
  { keywords: ['uber', 'ônibus', 'onibus', 'combustivel', 'gasolina', 'metro', '99'], category: 'TRANSPORTE' },
  { keywords: ['aluguel', 'condominio', 'internet', 'luz', 'agua', 'casa'], category: 'MORADIA' },
  { keywords: ['medico', 'médico', 'consulta', 'farmacia', 'farmácia', 'remedio', 'remédio'], category: 'SAUDE' },
  { keywords: ['cinema', 'jogo', 'show', 'viagem', 'streaming'], category: 'LAZER' },
  { keywords: ['curso', 'faculdade', 'livro', 'caderno', 'notebook', 'estudo'], category: 'EDUCACAO' },
  { keywords: ['fone', 'tenis', 'tênis', 'camisa', 'celular', 'mouse'], category: 'COMPRAS' },
];

// Sugere a categoria com base na descrição, mas mantém o usuário no controle da escolha final.
export function getSuggestedCategory(description: string): Category | null {
  const normalized = description
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

  const match = suggestionRules.find((rule) => rule.keywords.some((keyword) => normalized.includes(keyword)));
  return match?.category ?? null;
}

export function formatCurrency(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function formatMonthLabel(year: number, month: number): string {
  return new Date(year, month - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}

export function formatIsoDate(date: string): string {
  const [year, month, day] = date.split('-');
  if (!year || !month || !day) {
    return date;
  }

  return `${day}/${month}/${year}`;
}

export function calculateFinalPrice(originalPrice: number, discountPercent: number): number {
  const safeDiscount = Math.max(0, Math.min(discountPercent, 100));
  const discounted = originalPrice * (1 - safeDiscount / 100);
  return Number(discounted.toFixed(2));
}

export function buildPurchaseTransactions(
  item: AppWishlistItem,
  purchaseDate: string,
  paymentMethod: PaymentMethod,
  installments: number,
  firstInstallmentNextMonth: boolean,
  nextIdSeed: number,
): AppTransaction[] {
  const safeInstallments = Math.max(1, installments);
  const baseAmount = Number((item.finalPrice / safeInstallments).toFixed(2));
  const remainder = Number((item.finalPrice - baseAmount * safeInstallments).toFixed(2));
  const purchase = new Date(`${purchaseDate}T12:00:00`);
  const startMonth = firstInstallmentNextMonth ? 1 : 0;

  return Array.from({ length: safeInstallments }, (_, index) => {
    const scheduledDate = new Date(purchase);
    scheduledDate.setMonth(purchase.getMonth() + index + startMonth);

    const amount = index === safeInstallments - 1 ? Number((baseAmount + remainder).toFixed(2)) : baseAmount;

    return {
      id: nextIdSeed + index,
      type: 'DESPESA',
      description: `${item.description}${safeInstallments > 1 ? ` (${index + 1}/${safeInstallments})` : ''}`,
      category: item.category,
      amount,
      paymentMethod,
      transactionDate: scheduledDate.toISOString().slice(0, 10),
      origin: 'WISHLIST',
      wishlistItemId: item.id,
      installmentLabel: safeInstallments > 1 ? `${index + 1}/${safeInstallments}` : undefined,
    };
  });
}

function summarizeMonth(transactions: AppTransaction[], year: number, month: number) {
  const monthTransactions = transactions.filter((transaction) => {
    const date = new Date(`${transaction.transactionDate}T12:00:00`);
    return date.getFullYear() === year && date.getMonth() + 1 === month;
  });

  const totalReceitas = sumByType(monthTransactions, 'RECEITA');
  const totalDespesas = sumByType(monthTransactions, 'DESPESA');
  const saldo = totalReceitas - totalDespesas;
  const gastosPorCategoria = summarizeExpensesByCategory(monthTransactions);
  const maiorGasto =
    monthTransactions
      .filter((transaction) => transaction.type === 'DESPESA')
      .sort((left, right) => right.amount - left.amount)[0] ?? null;
  const menorGasto =
    monthTransactions
      .filter((transaction) => transaction.type === 'DESPESA')
      .sort((left, right) => left.amount - right.amount)[0] ?? null;

  return { monthTransactions, totalReceitas, totalDespesas, saldo, gastosPorCategoria, maiorGasto, menorGasto };
}

function summarizeYearToMonth(transactions: AppTransaction[], year: number, monthLimit: number): YearToDateSummary {
  const scopedTransactions = transactions.filter((transaction) => {
    const date = new Date(`${transaction.transactionDate}T12:00:00`);
    return date.getFullYear() === year && date.getMonth() + 1 <= monthLimit;
  });

  const totalReceitas = sumByType(scopedTransactions, 'RECEITA');
  const totalDespesas = sumByType(scopedTransactions, 'DESPESA');

  return {
    year,
    monthLimit,
    totalReceitas,
    totalDespesas,
    saldo: totalReceitas - totalDespesas,
  };
}

function sumByType(transactions: AppTransaction[], type: TransactionType): number {
  return Number(
    transactions
      .filter((transaction) => transaction.type === type)
      .reduce((total, transaction) => total + transaction.amount, 0)
      .toFixed(2),
  );
}

function summarizeExpensesByCategory(transactions: AppTransaction[]) {
  const bucket = new Map<Category, number>();

  transactions
    .filter((transaction) => transaction.type === 'DESPESA')
    .forEach((transaction) => {
      bucket.set(transaction.category, (bucket.get(transaction.category) ?? 0) + transaction.amount);
    });

  return Array.from(bucket.entries())
    .map(([category, totalAmount]) => ({ category, totalAmount: Number(totalAmount.toFixed(2)) }))
    .sort((left, right) => right.totalAmount - left.totalAmount);
}

function buildTrend(current: number, previous: number, inverse = false): Trend {
  if (current === previous) {
    return 'IGUAL';
  }

  if (inverse) {
    return current < previous ? 'MELHOR' : 'PIOR';
  }

  return current > previous ? 'MELHOR' : 'PIOR';
}

function buildComparison(current: ReturnType<typeof summarizeMonth>, referenceYear: number, referenceMonth: number, previous: ReturnType<typeof summarizeMonth>): ComparisonSnapshot {
  return {
    year: referenceYear,
    month: referenceMonth,
    totalReceitas: previous.totalReceitas,
    totalDespesas: previous.totalDespesas,
    saldo: previous.saldo,
    diferencaReceitas: Number((current.totalReceitas - previous.totalReceitas).toFixed(2)),
    diferencaDespesas: Number((current.totalDespesas - previous.totalDespesas).toFixed(2)),
    diferencaSaldo: Number((current.saldo - previous.saldo).toFixed(2)),
    tendenciaReceitas: buildTrend(current.totalReceitas, previous.totalReceitas),
    tendenciaDespesas: buildTrend(current.totalDespesas, previous.totalDespesas, true),
    tendenciaSaldo: buildTrend(current.saldo, previous.saldo),
    tendenciaGeral: buildTrend(current.saldo, previous.saldo),
  };
}

// Monta a leitura do Painel como o backend real faz: mês de referência + acumulado até ele.
export function buildDashboardSnapshot(transactions: AppTransaction[], year: number, month: number): DashboardSnapshot {
  const current = summarizeMonth(transactions, year, month);

  const scopedTransactions = transactions.filter((transaction) => {
    const date = new Date(`${transaction.transactionDate}T12:00:00`);
    const sameYearPastMonth = date.getFullYear() < year || (date.getFullYear() === year && date.getMonth() + 1 <= month);
    return sameYearPastMonth;
  });

  const totalReceitasAcumuladas = sumByType(scopedTransactions, 'RECEITA');
  const totalDespesasAcumuladas = sumByType(scopedTransactions, 'DESPESA');
  const yearly = summarizeYearToMonth(transactions, year, month);

  return {
    receitasMesAtual: current.totalReceitas,
    despesasMesAtual: current.totalDespesas,
    resultadoMesAtual: current.saldo,
    saldoAcumulado: Number((totalReceitasAcumuladas - totalDespesasAcumuladas).toFixed(2)),
    receitasAnoReferencia: yearly.totalReceitas,
    despesasAnoReferencia: yearly.totalDespesas,
    resultadoAnoReferencia: yearly.saldo,
    totalReceitasAcumuladas,
    totalDespesasAcumuladas,
    ultimasTransacoes: scopedTransactions
      .slice()
      .sort((left, right) => right.transactionDate.localeCompare(left.transactionDate))
      .slice(0, 5),
    gastosPorCategoria: current.gastosPorCategoria,
  };
}

// Monta a análise mensal v2 com os mesmos conceitos que já existem no backend.
export function buildMonthlyAnalysisSnapshot(transactions: AppTransaction[], year: number, month: number): MonthlyAnalysisSnapshot {
  const current = summarizeMonth(transactions, year, month);
  const previousMonthDate = new Date(year, month - 2, 1);
  const previousYearDate = new Date(year - 1, month - 1, 1);
  const previousMonth = summarizeMonth(transactions, previousMonthDate.getFullYear(), previousMonthDate.getMonth() + 1);
  const sameMonthPreviousYear = summarizeMonth(transactions, previousYearDate.getFullYear(), previousYearDate.getMonth() + 1);
  const currentYearToDate = summarizeYearToMonth(transactions, year, month);
  const previousYearToDate = summarizeYearToMonth(transactions, year - 1, month);

  const differReceitas = Number((currentYearToDate.totalReceitas - previousYearToDate.totalReceitas).toFixed(2));
  const differDespesas = Number((currentYearToDate.totalDespesas - previousYearToDate.totalDespesas).toFixed(2));
  const differSaldo = Number((currentYearToDate.saldo - previousYearToDate.saldo).toFixed(2));

  return {
    totalReceitas: current.totalReceitas,
    totalDespesas: current.totalDespesas,
    saldo: current.saldo,
    maiorGasto: current.maiorGasto,
    menorGasto: current.menorGasto,
    gastosPorCategoria: current.gastosPorCategoria,
    comparativoMesAnterior: buildComparison(current, previousMonthDate.getFullYear(), previousMonthDate.getMonth() + 1, previousMonth),
    comparativoMesmoMesAnoAnterior: buildComparison(current, previousYearDate.getFullYear(), previousYearDate.getMonth() + 1, sameMonthPreviousYear),
    acumuladoAnoAtual: currentYearToDate,
    comparativoAcumuladoAnoAnterior: {
      anoAtual: currentYearToDate,
      anoAnterior: previousYearToDate,
      diferencaReceitas: differReceitas,
      diferencaDespesas: differDespesas,
      diferencaSaldo: differSaldo,
      tendenciaReceitas: buildTrend(currentYearToDate.totalReceitas, previousYearToDate.totalReceitas),
      tendenciaDespesas: buildTrend(currentYearToDate.totalDespesas, previousYearToDate.totalDespesas, true),
      tendenciaSaldo: buildTrend(currentYearToDate.saldo, previousYearToDate.saldo),
      tendenciaGeral: buildTrend(currentYearToDate.saldo, previousYearToDate.saldo),
    },
  };
}
