import React from 'react'

export function Card({ children, className = '', hover = true, ...props }) {
  return (
    <div
      className={`w-full min-w-0 max-w-full bg-[var(--color-card-bg)] rounded-xl border border-[var(--color-card-border)] overflow-hidden transition-all duration-200 ${
        hover ? 'hover:-translate-y-0.5 hover:shadow-lg hover:border-[var(--color-border-hover)]' : ''
      } ${className}`}
      style={{ boxShadow: 'var(--color-card-shadow)' }}
      {...props}
    >
      {children}
    </div>
  )
}

export function CardHeader({ title, subtitle, action, className = '' }) {
  return (
    <div className={`p-5 sm:p-6 border-b border-[var(--color-border-primary)] flex flex-wrap items-center justify-between gap-4 ${className}`}>
      <div>
        {title && <h3 className="text-base sm:text-lg font-bold text-[var(--color-text-primary)] tracking-tight">{title}</h3>}
        {subtitle && <p className="text-xs sm:text-sm text-[var(--color-text-tertiary)] mt-0.5">{subtitle}</p>}
      </div>
      {action && <div className="flex items-center gap-2">{action}</div>}
    </div>
  )
}

export function CardBody({ children, className = '', noPadding = false }) {
  return (
    <div className={`${noPadding ? '' : 'p-5 sm:p-6'} ${className}`}>
      {children}
    </div>
  )
}

export default Card
