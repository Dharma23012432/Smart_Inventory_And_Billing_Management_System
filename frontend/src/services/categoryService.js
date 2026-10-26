import apiClient from './apiClient'

export const categoryService = {
  getAllCategories: () => apiClient.get('/api/v1/categories'),
  getCategoryById: (id) => apiClient.get(`/api/v1/categories/${id}`),
  createCategory: (data) => apiClient.post('/api/v1/categories', data),
  updateCategory: (id, data) => apiClient.put(`/api/v1/categories/${id}`, data),
  deleteCategory: (id) => apiClient.delete(`/api/v1/categories/${id}`),
}

export default categoryService
