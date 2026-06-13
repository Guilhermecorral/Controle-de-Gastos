import { ViewId } from './types';

export const onboardingKey = 'cg-demo-onboarding-dismissed';

export const navItems: Array<{ id: ViewId; label: string; description: string }> = [
  { id: 'painel', label: 'Painel', description: 'Resumo do periodo e acumulado' },
  { id: 'transacoes', label: 'Transacoes', description: 'Entradas, saidas e historico' },
  { id: 'analise', label: 'Analise mensal', description: 'Comparativos e tendencia' },
  { id: 'wishlist', label: 'Lista de desejos', description: 'Desejos, compras e historico' },
  { id: 'notasFiscais', label: 'Notas fiscais', description: 'Anexos por ano, mes e dia' },
  { id: 'admin', label: 'Admin', description: 'Usuarios, permissoes e comando' },
  { id: 'configuracoes', label: 'Configuracoes', description: 'Conta, privacidade e preferencias' },
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
  painel: { label: 'Painel', description: 'Resumo do periodo e acumulado' },
  transacoes: { label: 'Transacoes', description: 'Entradas, saidas e historico' },
  analise: { label: 'Analise mensal', description: 'Comparativos e tendencia' },
  wishlist: { label: 'Lista de desejos', description: 'Desejos, compras e historico' },
  notasFiscais: { label: 'Notas fiscais', description: 'Anexos por ano, mes e dia' },
  admin: { label: 'Admin', description: 'Usuarios, permissoes e comando' },
  configuracoes: { label: 'Configuracoes', description: 'Conta, privacidade e preferencias' },
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
    eyebrow: 'Visao atual',
    accentClass: 'text-emerald-300',
    panelClass: 'border-white/10 bg-slate-950 text-white',
    description: 'O painel resume o mes atual, mostra o acumulado ate o periodo de referencia e conecta o usuario as acoes mais importantes.',
  },
  transacoes: {
    eyebrow: 'Movimentacoes',
    accentClass: 'text-sky-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#10233f_100%)] text-white',
    description: 'A area de transacoes ja nasce preparada para modal, sugestao automatica de categoria, parcelamento e anexos fiscais por lancamento.',
  },
  analise: {
    eyebrow: 'Comparativo mensal',
    accentClass: 'text-violet-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#111827_0%,#312e81_100%)] text-white',
    description: 'A analise mensal foi desenhada para ler o mes atual, comparar com periodos anteriores e deixar a tendencia explicita.',
  },
  wishlist: {
    eyebrow: 'Planejamento de compra',
    accentClass: 'text-amber-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#1f2937_0%,#7c2d12_100%)] text-white',
    description: 'A lista de desejos funciona como um bloco forte do produto: desejo, prioridade, desconto, compra e impacto financeiro visualizados juntos.',
  },
  notasFiscais: {
    eyebrow: 'Organizacao fiscal',
    accentClass: 'text-cyan-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#082f49_0%,#0f172a_100%)] text-white',
    description: 'As notas fiscais ficam organizadas por ano, mes e momento do envio para facilitar consulta futura e rotina fiscal.',
  },
  admin: {
    eyebrow: 'Sala de comando',
    accentClass: 'text-amber-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#111827_0%,#4a1d1f_100%)] text-white',
    description: 'O admin acompanha a saude global, revisa contas, ajusta permissoes e aplica protecoes operacionais com confirmacoes rigidas.',
  },
  configuracoes: {
    eyebrow: 'Conta e privacidade',
    accentClass: 'text-rose-300',
    panelClass: 'border-white/10 bg-[linear-gradient(135deg,#111827_0%,#3f1d2e_100%)] text-white',
    description: 'A area de configuracoes reune seus dados da conta, privacidade e preferencias de uso.',
  },
};
