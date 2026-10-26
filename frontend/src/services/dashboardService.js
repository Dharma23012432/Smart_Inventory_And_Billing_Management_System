import apiClient from './apiClient'

export const dashboardService = {
  getSummary: () => apiClient.get('/api/v1/dashboard/summary'),
  getRecentSales: (limit = 5) => apiClient.get('/api/v1/dashboard/recent-sales', { params: { limit } }),
  getLowStockItems: () => apiClient.get('/api/v1/dashboard/low-stock-items'),
}

export default dashboardService
