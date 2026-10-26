/**
 * Frontend Event Bus for reactive, real-time cross-module synchronization.
 * Dispatches and listens to application domain events to keep Dashboard,
 * Reports, Notifications, Invoices, Customers, and Suppliers in sync without full page reloads.
 */

export const AppEvents = {
  INVOICE_CREATED: 'app:invoice-created',
  INVOICE_CONFIRMED: 'app:invoice-confirmed',
  INVOICE_CANCELLED: 'app:invoice-cancelled',
  INVOICE_PAID: 'app:invoice-paid',
  EMAIL_QUEUED: 'app:email-queued',
  EMAIL_SENT: 'app:email-sent',
  EMAIL_SKIPPED: 'app:email-skipped',
  EMAIL_FAILED: 'app:email-failed',
  SUPPLIER_UPDATED: 'app:supplier-updated',
  CUSTOMER_UPDATED: 'app:customer-updated',
  STOCK_UPDATED: 'app:stock-updated',
}

export function publishEvent(eventName, detail = {}) {
  try {
    const event = new CustomEvent(eventName, { detail, bubbles: true })
    window.dispatchEvent(event)
  } catch (err) {
    console.error(`Failed to publish event ${eventName}:`, err)
  }
}

export function subscribeEvent(eventName, handler) {
  const listener = (event) => handler(event.detail)
  window.addEventListener(eventName, listener)
  return () => window.removeEventListener(eventName, listener)
}
