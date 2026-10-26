import React from 'react'
import { motion } from 'framer-motion'
import { Loader2 } from 'lucide-react'

const VARIANTS = {
  primary:
    'bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-md shadow-purple-600/25 hover:shadow-purple-600/40 active:opacity-90 focus-visible:ring-purple-500',
  indigo:
    'bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-md shadow-purple-600/25 focus-visible:ring-purple-500',
  secondary:
    'bg-transparent border border-[var(--color-border-primary)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] active:opacity-80 focus-visible:ring-purple-500',
  outline:
    'bg-transparent border border-[var(--color-border-primary)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] active:opacity-80 focus-visible:ring-purple-500',
  danger:
    'bg-rose-600 hover:bg-rose-500 text-white shadow-md shadow-rose-600/25 active:opacity-90 focus-visible:ring-rose-600',
  success:
    'bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-600/25 active:opacity-90 focus-visible:ring-emerald-600',
  ghost:
    'bg-transparent text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] active:opacity-80 focus-visible:ring-purple-500',
}

const SIZES = {
  xs: 'p-1.5 text-xs rounded-lg gap-1 font-medium',
  sm: 'px-3 py-1.5 text-xs rounded-lg gap-1.5 font-semibold',
  md: 'px-4 py-2 text-sm rounded-xl gap-2 font-semibold',
  lg: 'px-5 py-2.5 text-sm rounded-xl gap-2.5 font-semibold',
  icon: 'p-2 rounded-xl',
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled = false,
  icon: Icon,
  className = '',
  type = 'button',
  ...props
}) {
  const variantStyles = VARIANTS[variant] || VARIANTS.primary
  const sizeStyles = SIZES[size] || SIZES.md

  return (
    <motion.button
      type={type}
      disabled={disabled || isLoading}
      whileHover={disabled || isLoading ? {} : { y: -1.5 }}
      whileTap={disabled || isLoading ? {} : { scale: 0.97 }}
      transition={{ duration: 0.18 }}
      className={`inline-flex items-center justify-center transition-all duration-200 cursor-pointer select-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--color-surface-primary)] disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none ${variantStyles} ${sizeStyles} ${className}`}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : Icon ? (
        <Icon className="w-4 h-4 shrink-0" />
      ) : null}
      {children}
    </motion.button>
  )
}

export default Button
