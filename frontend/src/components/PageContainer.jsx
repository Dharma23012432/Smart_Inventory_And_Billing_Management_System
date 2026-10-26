import React from 'react'

/**
 * Common responsive page container used across all pages in SmartInventory.
 * Guarantees equal 24px-32px padding, width calculated based on sidebar,
 * and strict overflow containment so borders/rounded corners are never clipped.
 */
export function PageContainer({ children, className = '' }) {
  return (
    <div className={`w-full min-w-0 max-w-full px-6 sm:px-8 py-6 ${className}`}>
      {children}
    </div>
  )
}

export default PageContainer
