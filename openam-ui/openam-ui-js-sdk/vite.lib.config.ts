import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import dts from 'vite-plugin-dts'

// https://vite.dev/config/
export default defineConfig({
  server: {
    allowedHosts: ["localhost", "openam.example.org"]
  },
  
  plugins: [
    react(),
    dts({
      include: ['src/lib/**/*'],
      outDir: 'target/lib',
      insertTypesEntry: true,
      rollupTypes: true,
      tsconfigPath: resolve(__dirname, "tsconfig.lib.json"),
    }),
  ],
  build: {
    outDir: 'target/lib',
    lib: {
      entry: resolve(__dirname, 'src/lib/index.ts'),
      name: 'openam-js-sdk',
      formats: ['es', 'umd'],
      fileName: (format) => `index.${format === 'es' ? 'js' : 'umd.cjs'}`,
    },
    rollupOptions: {
      external: ['react', 'react-dom'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
        },
      },
    },
  }
})
