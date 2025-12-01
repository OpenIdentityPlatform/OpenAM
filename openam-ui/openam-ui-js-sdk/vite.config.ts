import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  server: {
    allowedHosts: ["localhost", "openam.example.org"]
  },
  base: './',
  plugins: [react()],
  build: {
    outDir: 'target/app',
    rollupOptions: {
      output: {
        manualChunks: undefined,
      },
    },
  },
})
