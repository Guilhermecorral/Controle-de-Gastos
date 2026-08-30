// Centraliza o estado da sessão no frontend, mas deixa os cookies sensíveis sob controle exclusivo do backend.
import { create } from 'zustand'
import { AuthResponse, AuthUser } from '../types'
import { apiBaseUrl } from '../lib/runtimeConfig'

type AuthState = {
  user: AuthUser | null
  isAuthenticated: boolean
  hydrated: boolean
  bootStatus: 'checking' | 'waking' | 'ready'
  hydrate: () => Promise<boolean>
  login: (data: AuthResponse) => void
  updateUser: (data: AuthResponse) => void
  logout: () => void
}

const RENDER_WAKE_UP_DELAYS_MS = [1_000, 2_000, 3_000, 5_000, 8_000, 12_000, 15_000, 20_000]

let hydrationPromise: Promise<boolean> | null = null

function shouldRetryBootRequest(response: Response) {
  return response.status === 502 || response.status === 503 || response.status === 504
}

function wait(milliseconds: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds))
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  hydrated: false,
  bootStatus: 'checking',
  hydrate: () => {
    if (hydrationPromise) {
      return hydrationPromise
    }

    hydrationPromise = (async () => {
      try {
        let response: Response | undefined

        for (const delay of RENDER_WAKE_UP_DELAYS_MS) {
          response = await fetch(`${apiBaseUrl}/auth/me`, {
            credentials: 'include',
            headers: {
              Accept: 'application/json',
            },
          })

          if (!shouldRetryBootRequest(response)) {
            break
          }

          set({ bootStatus: 'waking' })
          await wait(delay)
        }

        if (!response) {
          set({ user: null, isAuthenticated: false, hydrated: true, bootStatus: 'ready' })
          return false
        }

        if (response.status === 401) {
          const refreshResponse = await fetch(`${apiBaseUrl}/auth/refresh`, {
            method: 'POST',
            credentials: 'include',
            headers: {
              Accept: 'application/json',
            },
          })

          if (refreshResponse.ok) {
            response = await fetch(`${apiBaseUrl}/auth/me`, {
              credentials: 'include',
              headers: {
                Accept: 'application/json',
              },
            })
          }
        }

        if (!response.ok) {
          set({ user: null, isAuthenticated: false, hydrated: true, bootStatus: 'ready' })
          return false
        }

        if (response.status === 204) {
          set({ user: null, isAuthenticated: false, hydrated: true, bootStatus: 'ready' })
          return false
        }

        const user = (await response.json()) as AuthUser
        set({ user, isAuthenticated: true, hydrated: true, bootStatus: 'ready' })
        return true
      } catch {
        set({ user: null, isAuthenticated: false, hydrated: true, bootStatus: 'ready' })
        return false
      }
    })().finally(() => {
      hydrationPromise = null
    })

    return hydrationPromise
  },
  login: (data) => {
    set({
      user: {
        name: data.name,
        email: data.email,
        role: data.role,
        twoFactorEnabled: data.twoFactorEnabled,
      },
      isAuthenticated: true,
      hydrated: true,
    })
  },
  updateUser: (data) => {
    set((currentValue) => ({
      ...currentValue,
      user: {
        name: data.name,
        email: data.email,
        role: data.role,
        twoFactorEnabled: data.twoFactorEnabled,
      },
      isAuthenticated: true,
      hydrated: true,
    }))
  },
  logout: () => {
    set({ user: null, isAuthenticated: false, hydrated: true })
  },
}))
