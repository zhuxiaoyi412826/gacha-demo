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
        changeOrigin: true,
        configure: (proxy) => {
          // 用局域网 IP 访问时，浏览器会给 POST/PUT/DELETE 带上局域网 Origin，
          // 经同源代理转发后会被后端 CORS 判定为非法来源，这里直接去掉 Origin
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin')
          })
        }
      }
    }
  }
})
