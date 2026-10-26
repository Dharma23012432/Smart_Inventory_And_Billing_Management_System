import apiClient from './apiClient'

export const productService = {
  getProducts: (params = {}) => apiClient.get('/api/v1/products', { params }),
  getProductById: (id) => apiClient.get(`/api/v1/products/${id}`),
  getProductBySku: (sku) => apiClient.get(`/api/v1/products/sku/${encodeURIComponent(sku)}`),
  createProduct: (data) => apiClient.post('/api/v1/products', data),
  updateProduct: (id, data) => apiClient.put(`/api/v1/products/${id}`, data),
  deleteProduct: (id) => apiClient.delete(`/api/v1/products/${id}`),
  updateProductStatus: (id, status) => apiClient.patch(`/api/v1/products/${id}/status`, null, { params: { status } }),
}

export default productService
