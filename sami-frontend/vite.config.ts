import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Auto-imports Vuetify components on demand (tree-shaking friendly).
    vuetify({ autoImport: true }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 7474,
    // The repository-owned Playwright container reaches the dev server through
    // host.docker.internal. Keep the allow-list explicit; this is development
    // infrastructure only and does not change production serving behavior.
    allowedHosts: ['host.docker.internal', 'localhost', '127.0.0.1'],
    // Proxy API calls to the backend in development so the browser makes
    // same-origin requests and there are no CORS surprises.
    proxy: {
      '/api': {
        target: process.env.VITE_DEV_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
