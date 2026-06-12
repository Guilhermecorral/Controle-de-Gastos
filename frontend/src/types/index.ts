// Alinha os tipos do frontend com os contratos reais expostos pelo backend.
export type Role = 'USER' | 'ADMIN' | string;

export type TransactionType = 'RECEITA' | 'DESPESA';

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

export type WishlistPriority = 'BAIXO' | 'MEDIA' | 'ALTO';
export type WishlistStatus = 'PENDENTE' | 'COMPRADO';
export type WishlistSortBy =
  | 'MENOR_PRECO'
  | 'MAIOR_PRECO'
  | 'PRIORIDADE'
  | 'ADICIONADOS_RECENTEMENTE'
  | 'PERSONALIZADO';

export type Trend = 'MELHOR' | 'PIOR' | 'IGUAL';

export interface AuthResponse {
  name: string;
  email: string;
  role: Role;
  twoFactorEnabled: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
  captchaToken?: string;
  twoFactorCode?: string;
}

export interface TwoFactorChallengeResponse {
  requiresTwoFactor: boolean;
  message: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  captchaToken?: string;
}

export interface ForgotPasswordRequest {
  email: string;
  captchaToken?: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
  confirmPassword: string;
  captchaToken?: string;
}

export interface AuthUser {
  name: string;
  email: string;
  role: Role;
  twoFactorEnabled: boolean;
}

export interface SimpleMessageResponse {
  message: string;
}

export interface ForgotPasswordResponse {
  message: string;
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
}

export interface DeleteAccountRequest {
  password: string;
}

export interface TwoFactorStatusResponse {
  enabled: boolean;
  pendingSetup: boolean;
  issuer: string;
}

export interface TwoFactorSetupResponse {
  secret: string;
  issuer: string;
  accountName: string;
  otpAuthUri: string;
  qrCodeSvg: string;
}

export interface TwoFactorVerifyRequest {
  code: string;
}

export interface DisableTwoFactorRequest {
  password: string;
  code: string;
}

export interface TransactionRequest {
  type: TransactionType;
  description: string;
  category: Category;
  amount: number;
  paymentMethod: PaymentMethod;
  installments: number;
  transactionDate: string;
}

export interface TransactionReceiptSummary {
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface TransactionResponse {
  id: number;
  type: TransactionType;
  description: string;
  category: Category;
  amount: number;
  paymentMethod: PaymentMethod;
  installments: number | null;
  transactionDate: string;
  createdAt: string;
  receipt: TransactionReceiptSummary | null;
}

export interface TransactionReceiptResponse {
  transactionId: number;
  type: TransactionType;
  description: string;
  category: Category;
  amount: number;
  paymentMethod: PaymentMethod;
  installments: number | null;
  transactionDate: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
  coveredTransactions: number;
}

export interface DashboardCategorySummary {
  category: Category;
  totalAmount: number;
}

export interface DashboardResponse {
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  totalReceitasAcumuladas: number;
  totalDespesasAcumuladas: number;
  saldoAcumulado: number;
  receitasAnoReferencia: number;
  despesasAnoReferencia: number;
  resultadoAnoReferencia: number;
  receitasMesAtual: number;
  despesasMesAtual: number;
  resultadoMesAtual: number;
  anoReferencia: number;
  mesReferencia: number;
  ultimasTransacoes: TransactionResponse[];
  gastosPorCategoria: DashboardCategorySummary[];
}

export interface MonthlyExpenseSummary {
  description: string;
  amount: number;
  category: Category;
  transactionDate: string;
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

export interface YearToDateComparison {
  anoAtual: YearToDateSummary;
  anoAnterior: YearToDateSummary;
  diferencaReceitas: number;
  diferencaDespesas: number;
  diferencaSaldo: number;
  tendenciaReceitas: Trend;
  tendenciaDespesas: Trend;
  tendenciaSaldo: Trend;
  tendenciaGeral: Trend;
}

export interface MonthlyAnalysisResponse {
  year: number;
  month: number;
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  maiorGasto: MonthlyExpenseSummary | null;
  receitasPorCategoria: DashboardCategorySummary[];
  gastosPorCategoria: DashboardCategorySummary[];
  comparativoMesAnterior: ComparisonSnapshot;
  comparativoMesmoMesAnoAnterior: ComparisonSnapshot;
  acumuladoAnoAtual: YearToDateSummary;
  comparativoAcumuladoAnoAnterior: YearToDateComparison;
}

export interface WishlistListResponse {
  id: number;
  name: string;
  description: string;
  isDefault: boolean;
  itemCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface WishlistListRequest {
  name: string;
  description: string;
}

export interface WishlistItemRequest {
  description: string;
  originalPrice: number;
  discountPercent: number;
  priority: WishlistPriority;
  category: Category;
  notes: string;
  listId: number;
}

export interface WishlistPurchaseRequest {
  purchaseDate: string;
  paymentMethod: PaymentMethod;
  installments: number;
  firstInstallmentNextMonth: boolean;
}

export interface WishlistItemResponse {
  id: number;
  description: string;
  originalPrice: number;
  discountPercent: number;
  finalPrice: number;
  priority: WishlistPriority;
  category: Category;
  notes: string;
  status: WishlistStatus;
  purchaseDate: string | null;
  paymentMethod: PaymentMethod | null;
  installments: number | null;
  firstInstallmentNextMonth: boolean | null;
  archivedAfterPurchase: boolean | null;
  linkedTransactionId: number | null;
  listId: number;
  listName: string;
  createdAt: string;
  updatedAt: string;
}

export interface WishlistSummaryResponse {
  quantidadeItensDesejados: number;
  quantidadeItensComprados: number;
  valorTotalDesejados: number;
  valorTotalComprados: number;
}

export interface WishlistHistoryResponse {
  id: number;
  actionType: 'CREATED' | 'UPDATED' | 'MOVED' | 'PURCHASED' | 'PURCHASE_UNDONE' | 'DELETED';
  description: string;
  finalPriceSnapshot: number;
  listNameSnapshot: string;
  createdAt: string;
}
