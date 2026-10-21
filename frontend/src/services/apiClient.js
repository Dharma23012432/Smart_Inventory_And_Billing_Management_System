/**
 * apiClient.js
 *
 * Base Axios-style fetch wrapper for all API calls to the backend services.
 * Actual service calls will be added in Phase 5 (Frontend).
 *
 * Base URLs:
 *   Inventory & Billing Service : http://localhost:8080
 *   Payment Service             : http://localhost:8081
 */

const INVENTORY_SERVICE_BASE_URL = '/api'

/**
 * Generic GET request helper.
 * @param {string} endpoint
 * @returns {Promise<any>}
 */
export async function get(endpoint) {
  const response = await fetch(`${INVENTORY_SERVICE_BASE_URL}${endpoint}`)
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP error ${response.status}`)
  }
  return response.json()
}

/**
 * Generic POST request helper.
 * @param {string} endpoint
 * @param {object} body
 * @returns {Promise<any>}
 */
export async function post(endpoint, body) {
  const response = await fetch(`${INVENTORY_SERVICE_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP error ${response.status}`)
  }
  return response.json()
}

/**
 * Generic PUT request helper.
 * @param {string} endpoint
 * @param {object} body
 * @returns {Promise<any>}
 */
export async function put(endpoint, body) {
  const response = await fetch(`${INVENTORY_SERVICE_BASE_URL}${endpoint}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP error ${response.status}`)
  }
  return response.json()
}

/**
 * Generic DELETE request helper.
 * @param {string} endpoint
 * @returns {Promise<void>}
 */
export async function del(endpoint) {
  const response = await fetch(`${INVENTORY_SERVICE_BASE_URL}${endpoint}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP error ${response.status}`)
  }
}
