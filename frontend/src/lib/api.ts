// Centraliza chamadas API para o backend, com auth headers
import axios from 'axios';
import Cookies from 'js-cookie';

const api = axios.create({
  baseURL: '/api', // Proxy para http://localhost:8080/api
});

api.interceptors.request.use((config) => {
  const token = Cookies.get('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expirado, logout
      Cookies.remove('accessToken');
      Cookies.remove('refreshToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
