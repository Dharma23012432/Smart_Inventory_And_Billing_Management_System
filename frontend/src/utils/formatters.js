/**
 * formatters.js
 *
 * Utility functions for formatting values in the UI.
 * Implementations will be expanded in Phase 5 (Frontend).
 */

/**
 * Formats a number as Indian Rupee currency.
 * @param {number} amount
 * @returns {string}
 */
export function formatCurrency(amount) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
  }).format(amount)
}

/**
 * Formats an ISO date string into a readable date.
 * @param {string} isoDate
 * @returns {string}
 */
export function formatDate(isoDate) {
  return new Intl.DateTimeFormat('en-IN', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  }).format(new Date(isoDate))
}

/**
 * Formats an ISO date string into a readable date-time.
 * @param {string} isoDate
 * @returns {string}
 */
export function formatDateTime(isoDate) {
  return new Intl.DateTimeFormat('en-IN', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(isoDate))
}
