// Centraliza as chamadas HTTP e usa cookies HttpOnly para a sessão, sem expor tokens à aplicação.
import axios from 'axios'
import { useAuthStore } from '../store/auth'

type RetriableRequestConfig = {
  _retry?: boolean
  skipAuthRefresh?: boolean
  url?: string
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

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
  async (error) => {
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

      try {
        await api.post('/auth/refresh', undefined, { skipAuthRefresh: true } as RetriableRequestConfig)
        return api(error.config)
      } catch {
        useAuthStore.getState().logout()

        if (window.location.pathname.startsWith('/app')) {
          window.location.assign('/login')
        }
      }
    }

    if (error.response?.status === 401 && !isAuthRoute) {
      useAuthStore.getState().logout()

      if (window.location.pathname.startsWith('/app')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

export default api
