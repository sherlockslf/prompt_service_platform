import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isProduction = mode === 'production'
  const publicBase = process.env.VITE_PUBLIC_BASE || (isProduction ? '/psu/' : '/')

  return {
    base: publicBase,
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
        },
        '/psu-api': {
          target: 'http://localhost:8084',
          changeOrigin: true,
          rewrite: path => path.replace(/^\/psu-api/, '/api')
        }
      }
    },
    build: {
      outDir: 'dist'
    }
  }
});
