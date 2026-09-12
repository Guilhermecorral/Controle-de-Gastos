export type TaxProfileType = 'PF' | 'PJ';
export type TaxStatus = 'A_PAGAR' | 'PAGA' | 'ATRASADA' | 'ISENTA' | 'EM_REVISAO';
export type TaxCategory = 'IRPF' | 'IPVA' | 'IPTU' | 'ISS' | 'INSS' | 'LICENCIAMENTO' | 'DARF' | 'PERSONALIZADO';
export type TaxRecurrence = 'UNICA' | 'MENSAL' | 'ANUAL' | 'PERSONALIZADA';

export interface TaxObligation {
  id: string;
  name: string;
  issuingAuthority: string | null;
  category: TaxCategory;
  dueDate: string;
  estimatedAmount: number;
  paidAmount: number | null;
  paidDate: string | null;
  status: TaxStatus;
  statusDisplay: string;
  documentStage: 'ESTIMATIVA' | 'GUIA_EMITIDA';
  recurrence: TaxRecurrence;
  competenceYear: number;
  competenceMonth: number | null;
  notes: string | null;
  origin: 'MANUAL' | 'INVESTIMENTO' | 'IMPORTACAO';
  paymentAccountDescription: string | null;
  receiptReference: string | null;
  linkedTransactionId: number | null;
  linkedDarfId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaxObligationInput {
  name: string;
  issuingAuthority: string;
  category: TaxCategory;
  dueDate: string;
  estimatedAmount: number;
  recurrence: TaxRecurrence;
  competenceYear: number;
  competenceMonth: number | null;
  notes: string;
  origin: 'MANUAL';
  status: 'A_PAGAR' | 'ISENTA' | 'EM_REVISAO';
  documentStage: 'ESTIMATIVA' | 'GUIA_EMITIDA';
}

export interface PaymentConfirmationInput {
  paidAmount: number;
  paidDate: string;
  accountDescription: string;
  receiptReference: string;
}
