import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// Mantém o servidor local previsível para fluxos como redefinição de senha via e-mail.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devPort = Number(env.VITE_PORT || 5173)

  return {
    plugins: [react()],
    server: {
      host: env.VITE_HOST || '127.0.0.1',
      port: devPort,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
      },
    },
  }
})
