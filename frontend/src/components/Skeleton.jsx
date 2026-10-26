import React from 'react'

export function Skeleton({ className = '', width, height, rounded = 'rounded-lg' }) {
  return (
    <div
      className={`skeleton-pulse ${rounded} ${className}`}
      style={{
        width: width || '100%',
        height: height || '16px',
        minHeight: height || '16px',
      }}
    />
  )
}

export function SkeletonText({ lines = 3, className = '' }) {
  return (
    <div className={`space-y-2.5 ${className}`}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton
          key={i}
          height="12px"
          width={i === lines - 1 ? '60%' : '100%'}
        />
      ))}
    </div>
  )
}

export function SkeletonCard({ className = '' }) {
  return (
    <div className={`p-6 rounded-2xl border border-[var(--color-border-primary)] bg-[var(--color-card-bg)] ${className}`}>
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 space-y-3">
          <Skeleton height="12px" width="40%" />
          <Skeleton height="28px" width="55%" rounded="rounded-lg" />
        </div>
        <Skeleton height="44px" width="44px" rounded="rounded-xl" />
      </div>
      <div className="mt-5 pt-3 border-t border-[var(--color-border-secondary)]">
        <Skeleton height="10px" width="70%" />
      </div>
    </div>
  )
}

export function SkeletonTableRow({ columns = 5, className = '' }) {
  return (
    <tr className={className}>
      {Array.from({ length: columns }).map((_, i) => (
        <td key={i} className="py-4 px-4 sm:px-6">
          <Skeleton height="14px" width={i === 0 ? '60%' : i === columns - 1 ? '40%' : '50%'} />
        </td>
      ))}
    </tr>
  )
}

export function SkeletonTable({ rows = 5, columns = 5, className = '' }) {
  return (
    <div className={`w-full ${className}`}>
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonTableRow key={i} columns={columns} />
      ))}
    </div>
  )
}

export default Skeleton
