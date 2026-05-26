import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// Mantém o proxy configurável por ambiente sem espalhar URLs fixas pela aplicação.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
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
