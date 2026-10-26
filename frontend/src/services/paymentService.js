import apiClient from './apiClient'

export const paymentService = {
  processPayment: (data) => apiClient.post('/api/v1/payments', data),
  getPaymentById: (id) => apiClient.get(`/api/v1/payments/${id}`),
  getPaymentByBillId: (billId) => apiClient.get(`/api/v1/payments/bill/${billId}`),
}

export default paymentService
