/**
 * formatters.js
 *
 * Formatting utilities for numbers, currencies, and dates.
 */

export function formatCurrency(amount) {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '₹0.00'
  }
  const numeric = typeof amount === 'number' ? amount : parseFloat(amount)
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numeric)
}

export function formatNumber(num) {
  if (num === null || num === undefined || isNaN(num)) {
    return '0'
  }
  const numeric = typeof num === 'number' ? num : parseFloat(num)
  return new Intl.NumberFormat('en-IN').format(numeric)
}

export function formatDate(isoDate) {
  if (!isoDate) return '—'
  try {
    const d = new Date(isoDate)
    if (isNaN(d.getTime())) return isoDate
    return new Intl.DateTimeFormat('en-IN', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
    }).format(d)
  } catch {
    return isoDate
  }
}

export function formatDateTime(isoDate) {
  if (!isoDate) return '—'
  try {
    const d = new Date(isoDate)
    if (isNaN(d.getTime())) return isoDate
    return new Intl.DateTimeFormat('en-IN', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(d)
  } catch {
    return isoDate
  }
}
