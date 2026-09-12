import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';
import type { TaxProfileType } from '../types';

const profileKey = ['tax', 'profile'] as const;

export function useTaxProfile() {
  return useQuery({
    queryKey: profileKey,
    queryFn: async () => (await api.get<{ taxProfileType: TaxProfileType | null }>('/tax/profile')).data.taxProfileType,
  });
}

export function useSaveTaxProfile() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (taxProfileType: TaxProfileType) =>
      (await api.put<{ taxProfileType: TaxProfileType }>('/tax/profile', { taxProfileType })).data.taxProfileType,
    onSuccess: (profile) => {
      client.setQueryData(profileKey, profile);
      client.invalidateQueries({ queryKey: ['tax', 'obligations'] });
    },
  });
}
