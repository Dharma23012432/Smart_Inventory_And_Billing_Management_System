import apiClient from './apiClient'

export const reportService = {
  getSalesReport: (params = {}) => apiClient.get('/api/v1/reports/sales', { params }),
  getInventoryValuationReport: () => apiClient.get('/api/v1/reports/inventory'),
  getPurchaseReport: (params = {}) => apiClient.get('/api/v1/reports/purchases', { params }),
}

export default reportService
