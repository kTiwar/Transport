import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/** Dev proxy target: must match Spring `server.port` (default 8080 in backend application.yml). */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: true,
      chunkSizeWarningLimit: 1600,
    },
  };
});
