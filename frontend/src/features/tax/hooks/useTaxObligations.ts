import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import type { PaymentConfirmationInput, TaxObligation, TaxObligationInput } from '../types';

const obligationsKey = ['tax', 'obligations'] as const;

export function useTaxObligations(enabled: boolean) {
  return useQuery({
    queryKey: obligationsKey,
    queryFn: async () => (await api.get<TaxObligation[]>('/tax/obligations')).data,
    enabled,
  });
}

export function useCreateTaxObligation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (input: TaxObligationInput) => (await api.post<TaxObligation>('/tax/obligations', input)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: obligationsKey }),
  });
}

export function useUpdateTaxObligation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: TaxObligationInput }) =>
      (await api.put<TaxObligation>(`/tax/obligations/${id}`, input)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: obligationsKey }),
  });
}

export function useDeleteTaxObligation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => api.delete(`/tax/obligations/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: obligationsKey }),
  });
}

export function useConfirmTaxPayment() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: PaymentConfirmationInput }) =>
      (await api.post<TaxObligation>(`/tax/obligations/${id}/confirm-payment`, input)).data,
    onSuccess: () => {
      client.invalidateQueries({ queryKey: obligationsKey });
      client.invalidateQueries({ queryKey: ['transactions'] });
      client.invalidateQueries({ queryKey: ['dashboard'] });
      client.invalidateQueries({ queryKey: ['monthly-analysis'] });
    },
  });
}
