import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: path => path
      }
    }
  },
  build: {
    outDir: 'dist'
  }
});