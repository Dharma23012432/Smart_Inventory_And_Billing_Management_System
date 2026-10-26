import React from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

export function Pagination({
  currentPage = 0,
  totalPages = 1,
  totalElements = 0,
  pageSize = 10,
  onPageChange,
  onPageSizeChange,
  className = '',
}) {
  const startItem = totalElements === 0 ? 0 : currentPage * pageSize + 1
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements)

  return (
    <div
      className={`px-6 py-4 flex flex-wrap items-center justify-between gap-4 border-t border-[var(--color-table-border)] bg-[var(--color-card-bg)] text-xs sm:text-sm text-[var(--color-text-secondary)] ${className}`}
    >
      <div className="flex items-center gap-2">
        <span>
          Showing <span className="font-semibold text-[var(--color-text-primary)]">{startItem}</span> to{' '}
          <span className="font-semibold text-[var(--color-text-primary)]">{endItem}</span> of{' '}
          <span className="font-semibold text-[var(--color-text-primary)]">{totalElements}</span> results
        </span>

        {onPageSizeChange && (
          <div className="ml-4 flex items-center gap-1.5 text-xs">
            <span>Per page:</span>
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="border border-[var(--color-border-primary)] rounded-lg px-2.5 py-1 bg-[var(--color-input-bg)] text-[var(--color-text-primary)] font-medium focus:outline-hidden focus:ring-1 focus:ring-[var(--color-accent)] cursor-pointer"
            >
              <option value={5} className="bg-[var(--color-card-bg)] text-[var(--color-text-primary)]">5</option>
              <option value={10} className="bg-[var(--color-card-bg)] text-[var(--color-text-primary)]">10</option>
              <option value={20} className="bg-[var(--color-card-bg)] text-[var(--color-text-primary)]">20</option>
              <option value={50} className="bg-[var(--color-card-bg)] text-[var(--color-text-primary)]">50</option>
            </select>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1.5">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          className="p-1.5 rounded-lg border border-[var(--color-border-primary)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          aria-label="Previous page"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <span className="px-3 py-1 font-semibold text-xs text-[var(--color-text-primary)]">
          Page {totalPages > 0 ? currentPage + 1 : 0} of {totalPages || 1}
        </span>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="p-1.5 rounded-lg border border-[var(--color-border-primary)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          aria-label="Next page"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}

export default Pagination
