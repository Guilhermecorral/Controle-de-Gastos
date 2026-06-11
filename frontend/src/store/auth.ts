// Centraliza o estado da sessão no frontend, mas deixa os cookies sensíveis sob controle exclusivo do backend.
import { create } from 'zustand'
import { AuthResponse, AuthUser } from '../types'

type AuthState = {
  user: AuthUser | null
  isAuthenticated: boolean
  hydrated: boolean
  hydrate: () => Promise<void>
  login: (data: AuthResponse) => void
  updateUser: (data: AuthResponse) => void
  logout: () => void
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() || '/api'

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  hydrated: false,
  hydrate: async () => {
    try {
      let response = await fetch(`${apiBaseUrl}/auth/me`, {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
        },
      })

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
        set({ user: null, isAuthenticated: false, hydrated: true })
        return
      }

      const user = (await response.json()) as AuthUser
      set({ user, isAuthenticated: true, hydrated: true })
    } catch {
      set({ user: null, isAuthenticated: false, hydrated: true })
    }
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
