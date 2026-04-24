import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import VueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    VueDevTools(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // Dev proxy targets iam-registry (port 18081 per iam-registry/src/main/resources/application.yml).
    // The legacy iam-core module runs on 8081; do NOT point this here — iam-core is being retired.
    proxy: {
      '/api': {
        target: 'http://localhost:18081',
        changeOrigin: true,
      },
      '/scim': {
        target: 'http://localhost:18081',
        changeOrigin: true,
      }
    }
  }
})
