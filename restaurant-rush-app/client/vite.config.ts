import path from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// In development the Java API server runs on 8090; Vite forwards /api calls to it.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8090',
    },
  },
})
