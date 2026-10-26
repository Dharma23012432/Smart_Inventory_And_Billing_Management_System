import React from 'react'

const STATUS_VARIANTS = {
  // Sales / Billing
  PAID: 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dot-emerald-500 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30',
  CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200/80 dot-blue-500 dark:bg-blue-500/15 dark:text-blue-400 dark:border-blue-500/30',
  DRAFT: 'bg-amber-50 text-amber-700 border-amber-200/80 dot-amber-500 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30',
  CANCELLED: 'bg-rose-50 text-rose-700 border-rose-200/80 dot-rose-500 dark:bg-rose-500/15 dark:text-rose-400 dark:border-rose-500/30',

  // Purchases
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200/80 dot-amber-500 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30',
  RECEIVED: 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dot-emerald-500 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30',

  // Master Data
  ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dot-emerald-500 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30',
  INACTIVE: 'bg-slate-100 text-slate-600 border-slate-200 dot-slate-400 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700',

  // Notifications
  SENT: 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dot-emerald-500 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30',
  FAILED: 'bg-rose-50 text-rose-700 border-rose-200/80 dot-rose-500 dark:bg-rose-500/15 dark:text-rose-400 dark:border-rose-500/30',
  SKIPPED: 'bg-slate-100 text-slate-600 border-slate-200 dot-slate-400 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700',

  // Inventory Stock
  IN_STOCK: 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dot-emerald-500 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30',
  LOW_STOCK: 'bg-amber-50 text-amber-700 border-amber-200/80 dot-amber-500 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30',
  OUT_OF_STOCK: 'bg-rose-50 text-rose-700 border-rose-200/80 dot-rose-500 dark:bg-rose-500/15 dark:text-rose-400 dark:border-rose-500/30',
}

const DOT_COLORS = {
  'dot-emerald-500': 'bg-emerald-500',
  'dot-blue-500': 'bg-blue-500',
  'dot-amber-500': 'bg-amber-500',
  'dot-rose-500': 'bg-rose-500',
  'dot-slate-400': 'bg-slate-400',
}

export function Badge({ children, status, variant, dot = true, className = '' }) {
  const key = (status || variant || '').toString().toUpperCase()
  const styleClasses = STATUS_VARIANTS[key] || 'bg-slate-100 text-slate-700 border-slate-200 dot-slate-400'
  const dotKey = styleClasses.split(' ').find((c) => c.startsWith('dot-'))
  const dotBg = DOT_COLORS[dotKey] || 'bg-slate-400'

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide border ${styleClasses.replace(dotKey, '')} ${className}`}
    >
      {dot && <span className={`w-1.5 h-1.5 rounded-full ${dotBg}`} />}
      {children || status}
    </span>
  )
}

export default Badge
