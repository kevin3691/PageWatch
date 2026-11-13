import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    watch: { usePolling: true, interval: 300 },
    proxy: {
      '/api': { target: process.env.API_PROXY ?? 'http://127.0.0.1:8088', changeOrigin: true }
    }
  },
  preview: {
    proxy: {
      '/api': { target: process.env.API_PROXY ?? 'http://127.0.0.1:8088', changeOrigin: true }
    }
  }
})
