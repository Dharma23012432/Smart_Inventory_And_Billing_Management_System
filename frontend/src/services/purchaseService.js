import apiClient from './apiClient'

export const purchaseService = {
  getPurchases: (params = {}) => apiClient.get('/api/v1/purchases', { params }),
  getPurchaseById: (id) => apiClient.get(`/api/v1/purchases/${id}`),
  createPurchase: (data) => apiClient.post('/api/v1/purchases', data),
  receivePurchase: (id) => apiClient.put(`/api/v1/purchases/${id}/receive`),
  cancelPurchase: (id) => apiClient.put(`/api/v1/purchases/${id}/cancel`),
}

export default purchaseService
