// Define hooks para queries e mutations usando TanStack Query
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from './api';
import { AuthResponse, DashboardResponse, Transaction, TransactionRequest, User, WishlistItem, MonthlyAnalysisResponse } from '../types';

// Auth
export const useLogin = () => {
  return useMutation({
    mutationFn: (data: { email: string; password: string }) => api.post<AuthResponse>('/auth/login', data).then(res => res.data),
  });
};

export const useRegister = () => {
  return useMutation({
    mutationFn: (data: { name: string; email: string; password: string }) => api.post<AuthResponse>('/auth/register', data).then(res => res.data),
  });
};

// Dashboard
export const useDashboard = (year?: number, month?: number) => {
  return useQuery({
    queryKey: ['dashboard', year, month],
    queryFn: () => api.get<DashboardResponse>(`/dashboard?year=${year}&month=${month}`).then(res => res.data),
    enabled: !!year && !!month,
  });
};

// Transactions
export const useTransactions = (params?: { type?: string; category?: string; startDate?: string; endDate?: string; page?: number }) => {
  return useQuery({
    queryKey: ['transactions', params],
    queryFn: () => api.get<{ content: Transaction[]; totalPages: number }>(`/transactions`, { params }).then(res => res.data),
  });
};

export const useCreateTransaction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TransactionRequest) => api.post('/transactions', data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  });
};

export const useUpdateTransaction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<TransactionRequest> }) => api.put(`/transactions/${id}`, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  });
};

export const useDeleteTransaction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete(`/transactions/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  });
};

// User
export const useUser = () => {
  return useQuery({
    queryKey: ['user'],
    queryFn: () => api.get<User>('/user').then(res => res.data),
  });
};

export const useUpdateUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<User>) => api.put('/user', data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['user'] }),
  });
};

// Monthly Analysis
export const useMonthlyAnalysis = (year: number, month: number) => {
  return useQuery({
    queryKey: ['monthly-analysis', year, month],
    queryFn: () => api.get<MonthlyAnalysisResponse>(`/monthly-analysis?year=${year}&month=${month}`).then(res => res.data),
  });
};

// Wishlist
export const useWishlist = () => {
  return useQuery({
    queryKey: ['wishlist'],
    queryFn: () => api.get<WishlistItem[]>('/wishlist').then(res => res.data),
  });
};

export const useCreateWishlistItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Omit<WishlistItem, 'id'>) => api.post('/wishlist', data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wishlist'] }),
  });
};

export const useUpdateWishlistItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<WishlistItem> }) => api.put(`/wishlist/${id}`, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wishlist'] }),
  });
};

export const useDeleteWishlistItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete(`/wishlist/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wishlist'] }),
  });
};
