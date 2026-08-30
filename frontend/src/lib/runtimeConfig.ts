const officialFrontendHosts = new Set([
  'farolfinanceiro.online',
  'www.farolfinanceiro.online',
]);

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() || '/api';

// Produção usa o próprio domínio para que cookies de sessão nunca sejam tratados como terceiros.
export const apiBaseUrl = !import.meta.env.DEV && officialFrontendHosts.has(window.location.hostname.toLowerCase())
  ? '/api'
  : configuredApiBaseUrl;
