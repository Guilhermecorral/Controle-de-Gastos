// Gerencia estado de autenticação global, sem armazenar tokens aqui (usa cookies)
import { create } from 'zustand';
import Cookies from 'js-cookie';
import { AuthResponse } from '../types';

interface AuthState {
  user: { name: string; email: string; role: string } | null;
  isAuthenticated: boolean;
  login: (data: AuthResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: !!Cookies.get('accessToken'),
  login: (data) => {
    Cookies.set('accessToken', data.accessToken, { sameSite: 'strict' });
    Cookies.set('refreshToken', data.refreshToken, { sameSite: 'strict' });
    set({ user: { name: data.name, email: data.email, role: data.role }, isAuthenticated: true });
  },
  logout: () => {
    Cookies.remove('accessToken');
    Cookies.remove('refreshToken');
    set({ user: null, isAuthenticated: false });
  },
}));
