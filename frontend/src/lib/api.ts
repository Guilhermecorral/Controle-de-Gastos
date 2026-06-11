// Centraliza as chamadas HTTP e usa cookies HttpOnly para a sessÃ£o, sem expor tokens Ã  aplicaÃ§Ã£o.
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/auth'

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
  skipAuthRefresh?: boolean
}

type PendingRequest = {
  resolve: () => void
  reject: (error: unknown) => void
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

let isRefreshing = false
let failedQueue: PendingRequest[] = []

function processQueue(error?: unknown) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
      return
    }

    resolve()
  })

  failedQueue = []
}

function forceLogout() {
  useAuthStore.getState().logout()

  if (window.location.pathname.startsWith('/app')) {
    window.location.assign('/login')
  }
}

if (import.meta.env.DEV) {
  api.interceptors.request.use((config) => {
    const method = (config.method ?? 'GET').toUpperCase()
    console.info('[API request]', method, config.baseURL ? `${config.baseURL}${config.url ?? ''}` : config.url ?? '')
    return config
  })

  api.interceptors.response.use(
    (response) => {
      const method = (response.config.method ?? 'GET').toUpperCase()
      console.info('[API response]', method, response.config.url ?? '', response.status)
      return response
    },
    (error) => {
      const method = (error.config?.method ?? 'GET').toUpperCase()
      const url = error.config?.url ?? ''
      const status = error.response?.status ?? 'sem-resposta'
      console.error('[API error]', method, url, status, error.response?.data ?? error.message)
      return Promise.reject(error)
    },
  )
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = (error.config ?? {}) as RetriableRequestConfig
    const requestUrl = String(originalRequest.url ?? '')
    const isAuthRoute = requestUrl.includes('/auth/login')
      || requestUrl.includes('/auth/register')
      || requestUrl.includes('/auth/forgot-password')
      || requestUrl.includes('/auth/reset-password')

    if (
      error.response?.status === 401
      && !originalRequest._retry
      && !originalRequest.skipAuthRefresh
      && !isAuthRoute
    ) {
      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: () => resolve(api(originalRequest)),
            reject,
          })
        })
      }

      isRefreshing = true

      try {
        await api.post('/auth/refresh', undefined, { skipAuthRefresh: true } as RetriableRequestConfig)
        processQueue()
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        forceLogout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status === 401 && !isAuthRoute) {
      forceLogout()
    }

    return Promise.reject(error)
  },
)

export default api
