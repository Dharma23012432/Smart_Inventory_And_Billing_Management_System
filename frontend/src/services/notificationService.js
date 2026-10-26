import apiClient from './apiClient'

export const notificationService = {
  getNotifications: (params = {}) => apiClient.get('/api/v1/notifications', { params }),
  getNotificationById: (id) => apiClient.get(`/api/v1/notifications/${id}`),
  approveNotification: (id) => apiClient.put(`/api/v1/notifications/${id}/approve`),
  retryNotification: (id) => apiClient.post(`/api/v1/notifications/${id}/retry`),
  getActionableCount: () => apiClient.get('/api/v1/notifications/actionable-count'),
}

export default notificationService
