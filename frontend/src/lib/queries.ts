// Reúne os hooks de leitura e mutação para os módulos que já existem no backend.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from './api'
import {
  AuthResponse,
  DisableTwoFactorRequest,
  DashboardResponse,
  DeleteAccountRequest,
  ForgotPasswordResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MonthlyAnalysisResponse,
  RegisterRequest,
  ResetPasswordRequest,
  SimpleMessageResponse,
  TransactionRequest,
  TransactionReceiptResponse,
  TransactionResponse,
  TwoFactorChallengeResponse,
  TwoFactorSetupResponse,
  TwoFactorStatusResponse,
  TwoFactorVerifyRequest,
  UpdateProfileRequest,
  WishlistHistoryResponse,
  WishlistItemRequest,
  WishlistItemResponse,
  WishlistListRequest,
  WishlistListResponse,
  WishlistPurchaseRequest,
  WishlistSortBy,
  WishlistStatus,
  WishlistSummaryResponse,
} from '../types'

export function useLoginMutation() {
  return useMutation({
    mutationFn: async (data: LoginRequest) =>
      (await api.post<AuthResponse | TwoFactorChallengeResponse>('/auth/login', data)).data,
  })
}

export function useRegisterMutation() {
  return useMutation({
    mutationFn: async (data: RegisterRequest) => (await api.post<AuthResponse>('/auth/register', data)).data,
  })
}

export function useForgotPasswordMutation() {
  return useMutation({
    mutationFn: async (data: ForgotPasswordRequest) =>
      (await api.post<ForgotPasswordResponse>('/auth/forgot-password', data)).data,
  })
}

export function useResetPasswordMutation() {
  return useMutation({
    mutationFn: async (data: ResetPasswordRequest) =>
      (await api.post<SimpleMessageResponse>('/auth/reset-password', data)).data,
  })
}

export function useLogoutMutation() {
  return useMutation({
    mutationFn: async () => (await api.post<SimpleMessageResponse>('/auth/logout')).data,
  })
}

export function useUpdateProfileMutation() {
  return useMutation({
    mutationFn: async (data: UpdateProfileRequest) => (await api.put<AuthResponse>('/users/me', data)).data,
  })
}

export function useDeleteAccountMutation() {
  return useMutation({
    mutationFn: async (data: DeleteAccountRequest) =>
      (await api.delete<SimpleMessageResponse>('/users/me', { data })).data,
  })
}

export function useTwoFactorStatusQuery(enabled = true) {
  return useQuery({
    queryKey: ['two-factor', 'status'],
    queryFn: async () => (await api.get<TwoFactorStatusResponse>('/users/me/two-factor')).data,
    enabled,
    staleTime: 30_000,
  })
}

export function useBeginTwoFactorSetupMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async () => (await api.post<TwoFactorSetupResponse>('/users/me/two-factor/setup')).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['two-factor', 'status'] })
    },
  })
}

export function useConfirmTwoFactorMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: TwoFactorVerifyRequest) =>
      (await api.post<TwoFactorStatusResponse>('/users/me/two-factor/confirm', data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['two-factor', 'status'] })
    },
  })
}

export function useDisableTwoFactorMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: DisableTwoFactorRequest) =>
      (await api.post<TwoFactorStatusResponse>('/users/me/two-factor/disable', data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['two-factor', 'status'] })
    },
  })
}

export function useDashboardQuery(year?: number, month?: number, enabled = true) {
  return useQuery({
    queryKey: ['dashboard', year, month],
    queryFn: async () =>
      (
        await api.get<DashboardResponse>('/dashboard', {
          params: {
            ...(year ? { year } : {}),
            ...(month ? { month } : {}),
          },
        })
      ).data,
    enabled,
    staleTime: 60_000,
  })
}

export function useTransactionsQuery(params: {
  type?: 'RECEITA' | 'DESPESA' | 'TODOS'
  category?: string | 'TODAS'
  enabled?: boolean
}) {
  const { type, category, enabled = true } = params

  return useQuery({
    queryKey: ['transactions', type, category],
    queryFn: async () =>
      (
        await api.get<TransactionResponse[]>('/transactions', {
          params: {
            ...(type && type !== 'TODOS' ? { type } : {}),
            ...(category && category !== 'TODAS' ? { category } : {}),
          },
        })
      ).data,
    enabled,
    staleTime: 30_000,
  })
}

export function useCreateTransactionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: TransactionRequest) => (await api.post<TransactionResponse>('/transactions', data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useUploadTransactionReceiptMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, file }: { id: number; file: File }) => {
      const formData = new FormData()
      formData.append('file', file)

      return (
        await api.post<TransactionResponse>(`/transactions/${id}/receipt`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      ).data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['transaction-receipts'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useUpdateTransactionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: TransactionRequest }) =>
      (await api.put<TransactionResponse>(`/transactions/${id}`, data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useDeleteTransactionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/transactions/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useMonthlyAnalysisQuery(year: number, month: number, enabled = true) {
  return useQuery({
    queryKey: ['monthly-analysis', year, month],
    queryFn: async () => (await api.get<MonthlyAnalysisResponse>('/monthly-analysis', { params: { year, month } })).data,
    enabled,
    staleTime: 60_000,
  })
}

export function useTransactionReceiptsQuery(year: number, month: number, enabled = true) {
  return useQuery({
    queryKey: ['transaction-receipts', year, month],
    queryFn: async () =>
      (await api.get<TransactionReceiptResponse[]>('/transactions/receipts', { params: { year, month } })).data,
    enabled,
    staleTime: 30_000,
  })
}

export function useWishlistListsQuery(enabled = true) {
  return useQuery({
    queryKey: ['wishlist', 'lists'],
    queryFn: async () => (await api.get<WishlistListResponse[]>('/wishlist/lists')).data,
    enabled,
    staleTime: 60_000,
  })
}

export function useWishlistItemsQuery(params: {
  status?: WishlistStatus | 'TODOS'
  sortBy?: WishlistSortBy
  listId?: string | number | 'TODAS'
  enabled?: boolean
}) {
  const { status, sortBy, listId, enabled = true } = params

  return useQuery({
    queryKey: ['wishlist', 'items', status, sortBy, listId],
    queryFn: async () =>
      (
        await api.get<WishlistItemResponse[]>('/wishlist', {
          params: {
            ...(status && status !== 'TODOS' ? { status } : {}),
            ...(sortBy ? { sortBy } : {}),
            ...(listId && listId !== 'TODAS' ? { listId } : {}),
          },
        })
      ).data,
    enabled,
    staleTime: 30_000,
  })
}

export function useWishlistSummaryQuery(enabled = true) {
  return useQuery({
    queryKey: ['wishlist', 'summary'],
    queryFn: async () => (await api.get<WishlistSummaryResponse>('/wishlist/summary')).data,
    enabled,
    staleTime: 30_000,
  })
}

export function useWishlistHistoryQuery(itemId?: number | null, enabled = true) {
  return useQuery({
    queryKey: ['wishlist', 'history', itemId],
    queryFn: async () => (await api.get<WishlistHistoryResponse[]>(`/wishlist/${itemId}/history`)).data,
    enabled: enabled && !!itemId,
    staleTime: 30_000,
  })
}

export function useCreateWishlistItemMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: WishlistItemRequest) => (await api.post<WishlistItemResponse>('/wishlist', data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
    },
  })
}

export function useUpdateWishlistItemMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: WishlistItemRequest }) =>
      (await api.put<WishlistItemResponse>(`/wishlist/${id}`, data)).data,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      queryClient.invalidateQueries({ queryKey: ['wishlist', 'history', variables.id] })
    },
  })
}

export function usePurchaseWishlistItemMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: WishlistPurchaseRequest }) =>
      (await api.post<WishlistItemResponse>(`/wishlist/${id}/purchase`, data)).data,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      queryClient.invalidateQueries({ queryKey: ['wishlist', 'history', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useUndoWishlistPurchaseMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: number) => (await api.post<WishlistItemResponse>(`/wishlist/${id}/undo-purchase`)).data,
    onSuccess: (_, itemId) => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      queryClient.invalidateQueries({ queryKey: ['wishlist', 'history', itemId] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useDeleteWishlistItemMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/wishlist/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['monthly-analysis'] })
    },
  })
}

export function useCreateWishlistListMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (data: WishlistListRequest) => (await api.post<WishlistListResponse>('/wishlist/lists', data)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist', 'lists'] })
    },
  })
}
