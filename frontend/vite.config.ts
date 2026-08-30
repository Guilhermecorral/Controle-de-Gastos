import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv, Plugin } from 'vite'

function buildMetadataPlugin(commit: string, branch: string, builtAt: string): Plugin {
  const buildInfo = {
    application: 'Farol Financeiro',
    commit,
    branch,
    builtAt,
  }

  return {
    name: 'farol-build-metadata',
    transformIndexHtml: {
      order: 'post',
      handler: () => [
        { tag: 'meta', attrs: { name: 'farol-build-commit', content: commit }, injectTo: 'head' },
        { tag: 'meta', attrs: { name: 'farol-build-time', content: builtAt }, injectTo: 'head' },
      ],
    },
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'build-info.json',
        source: `${JSON.stringify(buildInfo, null, 2)}\n`,
      })
    },
  }
}

// Mantém o servidor local previsível para fluxos como redefinição de senha via e-mail.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devPort = Number(env.VITE_PORT || 5173)
  const buildCommit = (
    process.env.VERCEL_GIT_COMMIT_SHA || process.env.GITHUB_SHA || env.VITE_BUILD_COMMIT || 'local'
  ).slice(0, 40)
  const buildBranch = process.env.VERCEL_GIT_COMMIT_REF || process.env.GITHUB_REF_NAME || 'local'
  const builtAt = new Date().toISOString()

  return {
    plugins: [react(), buildMetadataPlugin(buildCommit, buildBranch, builtAt)],
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
