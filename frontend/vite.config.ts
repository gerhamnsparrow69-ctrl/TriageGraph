import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Calls to /api go to the Spring Boot backend, so the browser sees one origin in dev.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
