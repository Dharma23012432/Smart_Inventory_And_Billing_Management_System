import axios from 'axios'

/**
 * apiClient.js
 *
 * Centralized Axios instance configured for all backend microservices:
 * - Inventory & Billing Service (Port 8080) -> /api/v1/products, /bills, /inventory, etc.
 * - Payment Service (Port 8081)             -> /api/v1/payments
 * - Notification Service (Port 8082)        -> /api/v1/notifications
 *
 * In local development, Vite proxies these paths to their respective containers.
 * In production or custom environments, VITE_API_BASE_URL can override the root prefix.
 */

const BASE_URL = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || ''

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  timeout: 15000,
})

// Response interceptor to handle ApiResponse envelopes and normalize errors
apiClient.interceptors.response.use(
  (response) => {
    // If backend returns standardized { success: true, data: ..., message: ... }
    if (response.data && response.data.success !== undefined) {
      return response.data.data !== undefined ? response.data.data : response.data
    }
    return response.data
  },
  (error) => {
    let message = 'An unexpected error occurred. Please try again.'

    if (error.response) {
      // Backend returned an error response
      const data = error.response.data
      if (data && typeof data === 'object') {
        if (data.error && typeof data.error === 'object') {
          const errObj = data.error
          if (Array.isArray(errObj.details) && errObj.details.length > 0) {
            const detailMessages = errObj.details.map((d) => (d.field ? `${d.field}: ${d.message}` : d.message)).join('; ')
            message = errObj.message ? `${errObj.message} (${detailMessages})` : detailMessages
          } else if (errObj.message) {
            message = errObj.message
          }
        } else if (data.message) {
          message = data.message
        } else if (typeof data.error === 'string') {
          message = data.error
        } else if (Array.isArray(data.errors) && data.errors.length > 0) {
          message = data.errors.map((e) => (typeof e === 'object' && e.message ? e.message : String(e))).join('; ')
        } else {
          message = `Request failed with status ${error.response.status}`
        }
      } else if (typeof data === 'string' && data.trim()) {
        message = data
      } else {
        message = `Request failed with status ${error.response.status}`
      }
    } else if (error.request) {
      message = 'Unable to connect to the backend server. Please verify services are running.'
    } else {
      message = error.message || message
    }

    const enhancedError = new Error(message)
    enhancedError.status = error.response?.status
    enhancedError.originalError = error
    return Promise.reject(enhancedError)
  }
)

export default apiClient
