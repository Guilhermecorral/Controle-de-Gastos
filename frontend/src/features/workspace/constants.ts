import { ViewId } from './types';

export const onboardingKey = 'cg-demo-onboarding-dismissed';

export const navItems: Array<{ id: ViewId; label: string; description: string }> = [
  { id: 'painel', label: 'Painel', description: 'Resumo do período e acumulado' },
  { id: 'transacoes', label: 'Transações', description: 'Entradas, saídas e histórico' },
  { id: 'analise', label: 'Análise mensal', description: 'Comparativos e tendência' },
  { id: 'wishlist', label: 'Lista de desejos', description: 'Desejos, compras e histórico' },
  { id: 'configuracoes', label: 'Configurações', description: 'Conta, privacidade e preferências' },
];

const today = new Date();
export const currentYear = today.getFullYear();
export const currentMonth = today.getMonth() + 1;

export const monthOptions = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: new Date(2026, index, 1).toLocaleDateString('pt-BR', { month: 'long' }),
}));

export const yearOptions = Array.from({ length: 5 }, (_, index) => currentYear - 2 + index);

export const viewMeta: Record<ViewId, { label: string; description: string }> = {
  painel: { label: 'Painel', description: 'Resumo do período e acumulado' },
  transacoes: { label: 'Transações', description: 'Entradas, saídas e histórico' },
  analise: { label: 'Análise mensal', description: 'Comparativos e tendência' },
  wishlist: { label: 'Lista de desejos', description: 'Desejos, compras e histórico' },
  configuracoes: { label: 'Configurações', description: 'Conta, privacidade e preferências' },
};
