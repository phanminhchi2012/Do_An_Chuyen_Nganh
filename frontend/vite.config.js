import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 2003,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:2012',
      '/oauth2': 'http://127.0.0.1:2012',
      '/login/oauth2': 'http://127.0.0.1:2012',
    },
  },
})
