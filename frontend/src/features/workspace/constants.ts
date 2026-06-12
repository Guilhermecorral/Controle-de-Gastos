import { ViewId } from './types';

export const onboardingKey = 'cg-demo-onboarding-dismissed';

export const navItems: Array<{ id: ViewId; label: string; description: string }> = [
  { id: 'painel', label: 'Painel', description: 'Resumo do período e acumulado' },
  { id: 'transacoes', label: 'Transações', description: 'Entradas, saídas e histórico' },
  { id: 'analise', label: 'Análise mensal', description: 'Comparativos e tendência' },
  { id: 'wishlist', label: 'Lista de desejos', description: 'Desejos, compras e histórico' },
  { id: 'notasFiscais', label: 'Notas fiscais', description: 'Anexos por ano, mês e dia' },
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
  notasFiscais: { label: 'Notas fiscais', description: 'Anexos por ano, mês e dia' },
  configuracoes: { label: 'Configurações', description: 'Conta, privacidade e preferências' },
};

export const viewAccentMap: Record<
  ViewId,
  {
    eyebrow: string;
    accentClass: string;
    panelClass: string;
    description: string;
  }
> = {
  painel: {
    eyebrow: 'Visão atual',
    accentClass: 'text-emerald-300',
    panelClass: 'border-white/10 bg-slate-950 text-white',
    description: 'O painel resume o mês atual, mostra o acumulado até o período de referência e conecta o usuário às ações mais importantes.',
  },
  transacoes: {
    eyebrow: 'Movimentações',
    accentClass: 'text-sky-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#10233f_100%)] text-white',
    description: 'A área de transações já nasce preparada para modal, sugestão automática de categoria, parcelamento e anexos fiscais por lançamento.',
  },
  analise: {
    eyebrow: 'Comparativo mensal',
    accentClass: 'text-violet-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#111827_0%,#312e81_100%)] text-white',
    description: 'A análise mensal foi desenhada para ler o mês atual, comparar com períodos anteriores e deixar a tendência explícita.',
  },
  wishlist: {
    eyebrow: 'Planejamento de compra',
    accentClass: 'text-amber-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#1f2937_0%,#7c2d12_100%)] text-white',
    description: 'A lista de desejos funciona como um bloco forte do produto: desejo, prioridade, desconto, compra e impacto financeiro visualizados juntos.',
  },
  notasFiscais: {
    eyebrow: 'Organização fiscal',
    accentClass: 'text-cyan-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#082f49_0%,#0f172a_100%)] text-white',
    description: 'As notas fiscais ficam organizadas por ano, mês e momento do envio para facilitar consulta futura e rotina fiscal.',
  },
  configuracoes: {
    eyebrow: 'Conta e privacidade',
    accentClass: 'text-rose-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#111827_0%,#3f1d2e_100%)] text-white',
    description: 'A área de configurações reúne seus dados da conta, privacidade e preferências de uso.',
  },
};
