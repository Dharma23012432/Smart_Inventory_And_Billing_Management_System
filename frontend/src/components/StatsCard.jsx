import { motion } from 'framer-motion'
import { TrendingUp, TrendingDown } from 'lucide-react'

export function StatsCard({
  title,
  value,
  subtitle,
  change,
  isPositive = true,
  icon: Icon,
  iconBg = 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]',
  className = '',
}) {
  return (
    <motion.div
      whileHover={{ y: -3, transition: { duration: 0.18 } }}
      className={`h-full bg-[var(--color-card-bg)] rounded-xl p-6 border border-[var(--color-card-border)] shadow-[var(--color-card-shadow)] hover:shadow-[var(--color-card-shadow-hover)] transition-all duration-200 flex flex-col justify-between ${className}`}
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs sm:text-sm font-semibold text-[var(--color-text-secondary)] uppercase tracking-wider">{title}</p>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight mt-2">{value}</h2>
        </div>
        {Icon && (
          <div className={`p-3 rounded-xl shrink-0 ${iconBg}`}>
            <Icon className="w-5 h-5 sm:w-6 sm:h-6" />
          </div>
        )}
      </div>

      {(subtitle || change !== undefined) && (
        <div className="flex items-center gap-2 mt-4 pt-3 border-t border-[var(--color-border-secondary)] text-xs">
          {change !== undefined && (
            <span
              className={`inline-flex items-center gap-1 font-bold px-2 py-0.5 rounded-full ${
                isPositive ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400'
              }`}
            >
              {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
              {change}
            </span>
          )}
          {subtitle && <span className="text-[var(--color-text-tertiary)] font-medium truncate">{subtitle}</span>}
        </div>
      )}
    </motion.div>
  )
}

export default StatsCard
