// Centraliza as chamadas HTTP e trata autenticação de forma consistente no frontend.
import axios from 'axios';
import { getAccessToken, useAuthStore } from '../store/auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 15000,
  headers: {
    Accept: 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();

      if (window.location.pathname.startsWith('/app')) {
        window.location.assign('/login');
      }
    }

    return Promise.reject(error);
  },
);

export default api;
