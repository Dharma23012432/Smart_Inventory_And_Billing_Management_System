import apiClient from './apiClient'

export const billService = {
  getBills: (params = {}) => apiClient.get('/api/v1/bills', { params }),
  getBillById: (id) => apiClient.get(`/api/v1/bills/${id}`),
  getBillByBillNumber: (billNumber) => apiClient.get(`/api/v1/bills/number/${encodeURIComponent(billNumber)}`),
  createBill: (data) => apiClient.post('/api/v1/bills', data),
  confirmBill: (id) => apiClient.put(`/api/v1/bills/${id}/confirm`),
  cancelBill: (id) => apiClient.put(`/api/v1/bills/${id}/cancel`),
}

export default billService
