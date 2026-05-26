// Centraliza a sessão do usuário no frontend sem confiar em cookies acessíveis por JavaScript.
import { create } from 'zustand';
import { AuthResponse, AuthUser } from '../types';

type AuthSession = {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
};

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  hydrated: boolean;
  hydrate: () => void;
  login: (data: AuthResponse) => void;
  logout: () => void;
};

const storageKey = 'cg-auth-session';

function readSession(): AuthSession | null {
  const rawValue = sessionStorage.getItem(storageKey);

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as AuthSession;
  } catch {
    sessionStorage.removeItem(storageKey);
    return null;
  }
}

function persistSession(session: AuthSession) {
  sessionStorage.setItem(storageKey, JSON.stringify(session));
}

function clearSessionStorage() {
  sessionStorage.removeItem(storageKey);
}

export function getAccessToken() {
  return readSession()?.accessToken ?? null;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,
  hydrated: false,
  hydrate: () => {
    const session = readSession();

    if (!session) {
      set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false, hydrated: true });
      return;
    }

    set({
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      user: session.user,
      isAuthenticated: true,
      hydrated: true,
    });
  },
  login: (data) => {
    const nextSession = {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: {
        name: data.name,
        email: data.email,
        role: data.role,
      },
    };

    persistSession(nextSession);
    set({
      accessToken: nextSession.accessToken,
      refreshToken: nextSession.refreshToken,
      user: nextSession.user,
      isAuthenticated: true,
      hydrated: true,
    });
  },
  logout: () => {
    clearSessionStorage();
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false, hydrated: true });
  },
}));
