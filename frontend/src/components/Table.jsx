import React from 'react'
import { motion } from 'framer-motion'
import { Inbox } from 'lucide-react'
import { SkeletonTableRow } from './Skeleton'

export function Table({
  columns = [],
  data = [],
  isLoading = false,
  emptyMessage = 'No records found',
  emptySubtext = 'Try refining your search or add a new record to get started.',
  onRowClick,
  selectedId,
  className = '',
  tableLayout = 'auto',
}) {
  return (
    <div className={`w-full max-w-full min-w-0 overflow-x-auto bg-[var(--color-table-bg)] ${className}`}>
      <table className={`w-full text-left text-sm border-collapse ${tableLayout === 'fixed' ? 'table-fixed' : 'table-auto'}`}>
        <thead className="sticky top-0 z-10 bg-[var(--color-table-header-bg)] text-[11px] font-bold text-[var(--color-text-secondary)] uppercase tracking-wider border-b border-[var(--color-table-border)]">
          <tr>
            {columns.map((col, idx) => (
              <th
                key={col.key || idx}
                scope="col"
                className={`py-3 px-3 sm:px-4 ${col.nowrap !== false ? 'whitespace-nowrap' : ''} ${
                  col.align === 'right'
                    ? 'text-right'
                    : col.align === 'center'
                    ? 'text-center'
                    : 'text-left'
                } ${col.className || ''}`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[var(--color-table-border)] text-[var(--color-text-primary)]">
          {isLoading ? (
            Array.from({ length: 5 }).map((_, i) => (
              <SkeletonTableRow key={i} columns={columns.length} />
            ))
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="py-16 text-center">
                <div className="flex flex-col items-center justify-center gap-2 max-w-sm mx-auto">
                  <div className="w-12 h-12 rounded-2xl bg-[var(--color-surface-tertiary)] flex items-center justify-center text-[var(--color-text-tertiary)] mb-1">
                    <Inbox className="w-6 h-6" />
                  </div>
                  <p className="text-sm font-bold text-[var(--color-text-primary)]">{emptyMessage}</p>
                  <p className="text-xs text-[var(--color-text-tertiary)]">{emptySubtext}</p>
                </div>
              </td>
            </tr>
          ) : (
            data.map((row, rowIndex) => {
              const isSelected = selectedId && (row.id === selectedId || row.billId === selectedId)
              return (
                <motion.tr
                  key={row.id || row.billId || rowIndex}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.15, delay: Math.min(rowIndex * 0.02, 0.2) }}
                  onClick={() => onRowClick?.(row)}
                  className={`transition-colors duration-150 ${
                    isSelected
                      ? 'bg-purple-600/15 text-[var(--color-text-primary)]'
                      : 'hover:bg-[var(--color-table-row-hover)]'
                  } ${onRowClick ? 'cursor-pointer' : ''}`}
                >
                  {columns.map((col, colIndex) => {
                    const val = col.accessor
                      ? typeof col.accessor === 'function'
                        ? col.accessor(row)
                        : row[col.accessor]
                      : null
                    return (
                      <td
                        key={col.key || colIndex}
                        className={`py-3 px-3 sm:px-4 align-middle text-xs sm:text-sm ${
                          col.align === 'right'
                            ? 'text-right'
                            : col.align === 'center'
                            ? 'text-center'
                            : 'text-left'
                        } ${col.cellClassName || ''}`}
                      >
                        {col.render ? col.render(row, rowIndex) : val}
                      </td>
                    )
                  })}
                </motion.tr>
              )
            })
          )}
        </tbody>
      </table>
    </div>
  )
}

export default Table
