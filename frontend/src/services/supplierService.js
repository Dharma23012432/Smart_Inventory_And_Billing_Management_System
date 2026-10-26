import apiClient from './apiClient'

export const supplierService = {
  getSuppliers: (params = {}) => apiClient.get('/api/v1/suppliers', { params }),
  getAllActiveSuppliers: () => apiClient.get('/api/v1/suppliers/active'),
  getSupplierById: (id) => apiClient.get(`/api/v1/suppliers/${id}`),
  createSupplier: (data) => apiClient.post('/api/v1/suppliers', data),
  updateSupplier: (id, data) => apiClient.put(`/api/v1/suppliers/${id}`, data),
  deleteSupplier: (id) => apiClient.delete(`/api/v1/suppliers/${id}`),
  updateSupplierStatus: (id, status) => apiClient.patch(`/api/v1/suppliers/${id}/status`, null, { params: { status } }),
}

export default supplierService
