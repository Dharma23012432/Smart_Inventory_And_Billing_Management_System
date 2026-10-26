import apiClient from './apiClient'

export const inventoryService = {
  getInventoryList: (params = {}) => apiClient.get('/api/v1/inventory', { params }),
  getInventoryById: (id) => apiClient.get(`/api/v1/inventory/${id}`),
  getInventoryByProductId: (productId) => apiClient.get(`/api/v1/inventory/product/${productId}`),
  getLowStockInventory: (params = {}) => apiClient.get('/api/v1/inventory/low-stock', { params }),
  adjustStock: (productId, data) => apiClient.post(`/api/v1/inventory/product/${productId}/adjust`, data),
  updateReorderSettings: (productId, data) => apiClient.put(`/api/v1/inventory/product/${productId}/settings`, data),
  updateReorderLevel: (productId, reorderLevel) =>
    apiClient.patch(`/api/v1/inventory/product/${productId}/reorder-level`, null, { params: { reorderLevel } }),
}

export default inventoryService
