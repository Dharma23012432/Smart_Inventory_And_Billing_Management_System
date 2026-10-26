import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    try {
      const stored = localStorage.getItem('app-theme')
      if (stored === 'dark' || stored === 'light') return stored
      if (window.matchMedia('(prefers-color-scheme: dark)').matches) return 'dark'
    } catch {
      // Fallback
    }
    return 'light'
  })

  // Synchronize <html> root class & localStorage
  useEffect(() => {
    const root = document.documentElement
    if (theme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
    try {
      localStorage.setItem('app-theme', theme)
    } catch {
      // ignore
    }
  }, [theme])

  /**
   * Premium circular ripple theme animation.
   * Originates from the toggle button coordinates.
   * Spreads outward across the screen in a circular wave in both directions:
   * Light -> Dark and Dark -> Light.
   * 600ms GPU accelerated, 0 flicker, preserving component state.
   */
  const toggleTheme = useCallback((event) => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'

    // Fallback if View Transitions API is not supported or user prefers reduced motion
    if (!document.startViewTransition || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setTheme(nextTheme)
      return
    }

    // Calculate exact button coordinates
    let x = window.innerWidth - 80
    let y = 32
    if (event?.clientX !== undefined && event?.clientY !== undefined && event.clientX > 0) {
      x = event.clientX
      y = event.clientY
    } else if (event?.currentTarget) {
      const rect = event.currentTarget.getBoundingClientRect()
      x = rect.left + rect.width / 2
      y = rect.top + rect.height / 2
    }

    // Maximum distance from click point to furthest screen corner
    const endRadius = Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y)
    )

    const transition = document.startViewTransition(() => {
      setTheme(nextTheme)
      if (nextTheme === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    })

    transition.ready.then(() => {
      document.documentElement.animate(
        {
          clipPath: [
            `circle(0px at ${x}px ${y}px)`,
            `circle(${endRadius}px at ${x}px ${y}px)`,
          ],
        },
        {
          duration: 600,
          easing: 'cubic-bezier(0.25, 1, 0.5, 1)',
          pseudoElement: '::view-transition-new(root)',
        }
      )
    }).catch(() => {
      // Fallback
    })
  }, [theme])

  const isDark = theme === 'dark'

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme, isDark }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}

export default ThemeContext
