import apiClient from './apiClient'

export const customerService = {
  getCustomers: (params = {}) => apiClient.get('/api/v1/customers', { params }),
  getAllActiveCustomers: () => apiClient.get('/api/v1/customers/active'),
  getCustomerById: (id) => apiClient.get(`/api/v1/customers/${id}`),
  createCustomer: (data) => apiClient.post('/api/v1/customers', data),
  updateCustomer: (id, data) => apiClient.put(`/api/v1/customers/${id}`, data),
  deleteCustomer: (id) => apiClient.delete(`/api/v1/customers/${id}`),
  updateCustomerStatus: (id, status) => apiClient.patch(`/api/v1/customers/${id}/status`, null, { params: { status } }),
}

export default customerService
