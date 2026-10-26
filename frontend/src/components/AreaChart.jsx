import React, { useState, useId } from 'react'

export function AreaChart({
  data = [],
  title = 'Revenue Trend',
  height = 240,
  timeRange = 'Monthly',
  onTimeRangeChange,
  timeRangeOptions = ['Daily', 'Weekly', 'Monthly'],
}) {
  const [hoveredIndex, setHoveredIndex] = useState(null)
  const gradientId = useId()

  const width = 640
  const paddingLeft = 52
  const paddingRight = 24
  const paddingTop = 28
  const paddingBottom = 32
  const chartWidth = width - paddingLeft - paddingRight
  const chartHeight = height - paddingTop - paddingBottom

  // If no data is provided or array is completely empty
  const hasData = Array.isArray(data) && data.length > 0
  const cleanData = hasData ? data : [{ label: 'Today', value: 0 }]

  const rawValues = cleanData.map((d) => Number(d.value ?? d.amount ?? 0))
  const maxRaw = Math.max(...rawValues, 0)
  // Give 15% headroom above highest value so stroke doesn't clip top
  const maxVal = maxRaw > 0 ? maxRaw * 1.15 : 100
  const minVal = 0

  // Calculate coordinates for points
  let points = []
  if (cleanData.length === 1) {
    // SINGLE DATA POINT HANDLING:
    // Create an elegant smooth wave spanning across the entire chart
    const singleVal = rawValues[0]
    const yVal = paddingTop + chartHeight - (singleVal / maxVal) * chartHeight
    points = [
      {
        x: paddingLeft + chartWidth * 0.5,
        y: yVal,
        label: cleanData[0].label || cleanData[0].date || cleanData[0].period || 'Current',
        value: singleVal,
        isSingle: true,
      },
    ]
  } else {
    points = cleanData.map((d, i) => {
      const x = paddingLeft + (i / (cleanData.length - 1)) * chartWidth
      const val = Number(d.value ?? d.amount ?? 0)
      const y = paddingTop + chartHeight - ((val - minVal) / (maxVal - minVal)) * chartHeight
      return {
        x,
        y,
        label: d.label || d.date || d.period || `P${i + 1}`,
        value: val,
      }
    })
  }

  // Generate SVG Path
  let pathD = ''
  let areaD = ''

  if (points.length === 1) {
    const pt = points[0]
    const baseline = height - paddingBottom
    // Smooth hill from left baseline, peaking at center, down to right baseline
    pathD = `M ${paddingLeft} ${baseline} Q ${paddingLeft + chartWidth * 0.25} ${pt.y}, ${pt.x} ${pt.y} T ${width - paddingRight} ${baseline}`
    areaD = `${pathD} L ${width - paddingRight} ${baseline} L ${paddingLeft} ${baseline} Z`
  } else {
    // Smooth cubic bezier spline
    pathD = points.reduce((acc, pt, i, arr) => {
      if (i === 0) return `M ${pt.x} ${pt.y}`
      const prev = arr[i - 1]
      const cp1x = prev.x + (pt.x - prev.x) / 2
      const cp1y = prev.y
      const cp2x = prev.x + (pt.x - prev.x) / 2
      const cp2y = pt.y
      return `${acc} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${pt.x} ${pt.y}`
    }, '')

    const lastPt = points[points.length - 1]
    const firstPt = points[0]
    areaD = `${pathD} L ${lastPt.x} ${height - paddingBottom} L ${firstPt.x} ${height - paddingBottom} Z`
  }

  // Gridline step values
  const gridSteps = [1, 0.66, 0.33, 0]

  const formatShortCurrency = (val) => {
    if (val >= 10000000) return `₹${(val / 10000000).toFixed(1)}Cr`
    if (val >= 100000) return `₹${(val / 100000).toFixed(1)}L`
    if (val >= 1000) return `₹${(val / 1000).toFixed(1)}k`
    return `₹${Math.round(val)}`
  }

  return (
    <div className="w-full select-none">
      {/* Header with Title and Range Switcher */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h4 className="text-sm sm:text-base font-bold text-[var(--color-text-primary)] tracking-tight">
            {title}
          </h4>
          <p className="text-[11px] text-[var(--color-text-tertiary)] mt-0.5">
            {points.length === 1
              ? `Period: ${points[0].label} • ₹${points[0].value.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
              : 'Continuous revenue aggregation across active billing periods'}
          </p>
        </div>

        {onTimeRangeChange && (
          <div className="flex items-center bg-[var(--color-surface-tertiary)] p-1 rounded-xl text-xs font-semibold border border-[var(--color-border-secondary)]">
            {timeRangeOptions.map((opt) => (
              <button
                key={opt}
                type="button"
                onClick={() => onTimeRangeChange(opt)}
                className={`px-3 py-1 rounded-lg transition-all cursor-pointer ${
                  timeRange === opt
                    ? 'bg-[var(--color-card-bg)] text-[var(--color-accent)] shadow-xs font-bold'
                    : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                }`}
              >
                {opt}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* SVG Chart Area */}
      <div className="relative w-full overflow-hidden">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto overflow-visible"
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-chart-line)" stopOpacity="0.32" />
              <stop offset="60%" stopColor="var(--color-chart-line)" stopOpacity="0.08" />
              <stop offset="100%" stopColor="var(--color-chart-line)" stopOpacity="0.0" />
            </linearGradient>

            <filter id={`glow-${gradientId}`} x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="0" dy="4" stdDeviation="4" floodColor="var(--color-chart-line)" floodOpacity="0.35" />
            </filter>
          </defs>

          {/* Grid lines and Left Axis Labels */}
          {gridSteps.map((ratio) => {
            const y = paddingTop + chartHeight * (1 - ratio)
            const gridVal = minVal + (maxVal - minVal) * ratio
            return (
              <g key={ratio}>
                <text
                  x={paddingLeft - 8}
                  y={y + 3.5}
                  textAnchor="end"
                  className="fill-[var(--color-text-tertiary)] text-[9px] font-medium"
                >
                  {formatShortCurrency(gridVal)}
                </text>
                <line
                  x1={paddingLeft}
                  y1={y}
                  x2={width - paddingRight}
                  y2={y}
                  stroke="currentColor"
                  className="text-[var(--color-chart-grid)]"
                  strokeWidth="1"
                  strokeDasharray="4 4"
                />
              </g>
            )
          })}

          {/* Gradient Area Fill */}
          <path d={areaD} fill={`url(#${gradientId})`} />

          {/* Spline Stroke Line with subtle glow */}
          <path
            d={pathD}
            fill="none"
            stroke="var(--color-chart-line)"
            strokeWidth="3.2"
            strokeLinecap="round"
            strokeLinejoin="round"
            filter={`url(#glow-${gradientId})`}
          />

          {/* Interactive or Single Data Points */}
          {points.map((pt, i) => {
            const isHovered = hoveredIndex === i || points.length === 1
            return (
              <g
                key={i}
                className="cursor-pointer"
                onMouseEnter={() => setHoveredIndex(i)}
                onMouseLeave={() => setHoveredIndex(null)}
              >
                {/* Pulsing Aura Circle for single point or active hover */}
                {isHovered && (
                  <circle
                    cx={pt.x}
                    cy={pt.y}
                    r="12"
                    fill="var(--color-chart-line)"
                    opacity="0.22"
                    className="animate-pulse"
                  />
                )}
                <circle
                  cx={pt.x}
                  cy={pt.y}
                  r={isHovered ? 5.5 : 3.5}
                  fill="var(--color-card-bg)"
                  stroke="var(--color-chart-line)"
                  strokeWidth="2.5"
                  className="transition-all duration-150"
                />
                {/* Large transparent hit target for easy mouse hover */}
                <circle cx={pt.x} cy={pt.y} r="18" fill="transparent" />
              </g>
            )
          })}
        </svg>

        {/* Hover Tooltip or Single Point Value Tag */}
        {(hoveredIndex !== null || points.length === 1) && (() => {
          const activePt = hoveredIndex !== null ? points[hoveredIndex] : points[0]
          if (!activePt) return null

          const leftPercent = (activePt.x / width) * 100
          const topPercent = (activePt.y / height) * 100

          return (
            <div
              className="absolute z-20 pointer-events-none transform -translate-x-1/2 -translate-y-[115%] px-3 py-1.5 rounded-xl bg-slate-900/95 dark:bg-slate-800/95 text-white text-xs font-semibold shadow-xl border border-slate-700/60 backdrop-blur-md transition-all duration-150"
              style={{
                left: `${leftPercent}%`,
                top: `${topPercent}%`,
              }}
            >
              <div className="flex items-center gap-1.5 text-[10px] text-slate-400 font-normal">
                <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-chart-line)]" />
                <span>{activePt.label}</span>
              </div>
              <div className="font-extrabold text-[var(--color-accent-text)] text-xs sm:text-sm mt-0.5">
                ₹{activePt.value.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
            </div>
          )
        })()}
      </div>

      {/* X-Axis Labels */}
      <div className="flex justify-between pl-12 pr-6 pt-2 text-[10px] sm:text-[11px] font-semibold text-[var(--color-text-tertiary)]">
        {points.length === 1 ? (
          <span className="w-full text-center">{points[0].label} (Recorded Revenue)</span>
        ) : (
          points.map((pt, i) => {
            // Display clean subset of labels if too many points
            const shouldShow =
              points.length <= 8 ||
              i === 0 ||
              i === points.length - 1 ||
              i % Math.ceil(points.length / 6) === 0
            if (!shouldShow) return <span key={i} className="hidden" />
            return (
              <span key={i} className="truncate max-w-[70px] text-center">
                {pt.label}
              </span>
            )
          })
        )}
      </div>
    </div>
  )
}

export default AreaChart
