import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const inventoryServiceUrl = env.VITE_INVENTORY_SERVICE_URL || env.INVENTORY_SERVICE_URL || 'http://localhost:8080'
  const paymentServiceUrl = env.VITE_PAYMENT_SERVICE_URL || env.PAYMENT_SERVICE_URL || 'http://localhost:8081'
  const notificationServiceUrl = env.VITE_NOTIFICATION_SERVICE_URL || env.NOTIFICATION_SERVICE_URL || 'http://localhost:8082'

  return {
    plugins: [
      react(),
      tailwindcss(),
    ],
    server: {
      port: 3000,
      proxy: {
        '/api/v1/payments': {
          target: paymentServiceUrl,
          changeOrigin: true,
        },
        '/api/v1/notifications': {
          target: notificationServiceUrl,
          changeOrigin: true,
        },
        '/api': {
          target: inventoryServiceUrl,
          changeOrigin: true,
        },
      },
    },
  }
})
