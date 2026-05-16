// Define tipos baseados nos DTOs do backend para type safety
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  name: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
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
  ultimasTransacoes: Transaction[];
  gastosPorCategoria: CategorySummary[];
}

export interface Transaction {
  id: number;
  type: 'RECEITA' | 'DESPESA';
  description: string;
  category: string;
  amount: number;
  paymentMethod: string;
  transactionDate: string;
  createdAt: string;
}

export interface CategorySummary {
  category: string;
  totalAmount: number;
}

export interface TransactionRequest {
  type: 'RECEITA' | 'DESPESA';
  description: string;
  category: string;
  amount: number;
  paymentMethod: string;
  transactionDate: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

export interface WishlistItem {
  id: number;
  name: string;
  targetAmount: number;
  currentAmount: number;
  description?: string;
}

export interface MonthlyAnalysisResponse {
  year: number;
  month: number;
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  maiorGasto: {
    description: string;
    amount: number;
    category: string;
    transactionDate: string;
  };
  gastosPorCategoria: CategorySummary[];
  comparativoMesAnterior: Comparison;
  comparativoMesmoMesAnoAnterior: Comparison;
  acumuladoAnoAtual: {
    year: number;
    monthLimit: number;
    totalReceitas: number;
    totalDespesas: number;
    saldo: number;
  };
  comparativoAcumuladoAnoAnterior: {
    anoAtual: {
      year: number;
      monthLimit: number;
      totalReceitas: number;
      totalDespesas: number;
      saldo: number;
    };
    anoAnterior: {
      year: number;
      monthLimit: number;
      totalReceitas: number;
      totalDespesas: number;
      saldo: number;
    };
    diferencaReceitas: number;
    diferencaDespesas: number;
    diferencaSaldo: number;
    tendenciaReceitas: 'MELHOR' | 'PIOR' | 'IGUAL';
    tendenciaDespesas: 'MELHOR' | 'PIOR' | 'IGUAL';
    tendenciaSaldo: 'MELHOR' | 'PIOR' | 'IGUAL';
    tendenciaGeral: 'MELHOR' | 'PIOR' | 'IGUAL';
  };
}

export interface Comparison {
  year: number;
  month: number;
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  diferencaReceitas: number;
  diferencaDespesas: number;
  diferencaSaldo: number;
  tendenciaReceitas: 'MELHOR' | 'PIOR' | 'IGUAL';
  tendenciaDespesas: 'MELHOR' | 'PIOR' | 'IGUAL';
  tendenciaSaldo: 'MELHOR' | 'PIOR' | 'IGUAL';
  tendenciaGeral: 'MELHOR' | 'PIOR' | 'IGUAL';
}
