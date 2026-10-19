import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // Chạy `npm run dev` ngoài Docker thì /api được proxy sang backend local
    proxy: {
      // ws: true để proxy cả kết nối realtime /api/ws (WebSocket)
      '/api': { target: 'http://localhost:8080', ws: true },
    },
  },
});
