import React from 'react'
import { Search, X } from 'lucide-react'

export function SearchInput({
  value,
  onChange,
  placeholder = 'Search...',
  className = '',
  onClear,
}) {
  return (
    <div className={`relative flex items-center ${className}`}>
      <Search className="absolute left-3.5 w-4 h-4 text-[var(--color-text-tertiary)] pointer-events-none" />
      <input
        type="text"
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full pl-10 pr-9 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-sm text-[var(--color-text-primary)] placeholder-[var(--color-input-placeholder)] focus:bg-[var(--color-surface-primary)] focus:outline-hidden focus:ring-2 focus:ring-[var(--color-input-focus-ring)] focus:border-[var(--color-input-focus-border)] transition-all"
      />
      {value && (
        <button
          type="button"
          onClick={() => (onClear ? onClear() : onChange(''))}
          className="absolute right-3 p-0.5 text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)] rounded-full transition-colors"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  )
}

export default SearchInput
