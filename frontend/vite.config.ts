import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // Vite 代理绕过 CORS，前端请求 /api 实际转发到 Spring Boot 8088
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }
})
