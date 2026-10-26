import apiClient from './apiClient'

export const settingService = {
  getAllSettings: () => apiClient.get('/api/v1/settings'),
  getSettingByKey: (key) => apiClient.get(`/api/v1/settings/${encodeURIComponent(key)}`),
  updateSettings: (settingsMap) => apiClient.put('/api/v1/settings', settingsMap),
}

export default settingService
