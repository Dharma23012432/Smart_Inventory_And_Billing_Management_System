import React, { useState, useEffect, useCallback } from 'react'
import { Outlet, NavLink, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import ErrorBoundary from '../components/ErrorBoundary'
import PageContainer from '../components/PageContainer'
import {
  LayoutDashboard,
  ShoppingCart,
  Receipt,
  Package,
  Layers,
  Boxes,
  Truck,
  Users,
  Building2,
  BarChart3,
  Bell,
  Settings,
  Menu,
  X,
  Search,
  Moon,
  Sun,
  PanelLeftClose,
  PanelLeft,
  LogOut,
  Sparkles,
} from 'lucide-react'
import { notificationService } from '../services/notificationService'
import { useTheme } from '../context/ThemeContext'
import { AppEvents, subscribeEvent } from '../utils/eventBus'

const NAV_GROUPS = [
  {
    id: 'overview',
    title: 'OVERVIEW',
    items: [
      { label: 'Dashboard', path: '/', icon: LayoutDashboard },
    ],
  },
  {
    id: 'sales',
    title: 'SALES',
    items: [
      { label: 'New Sale', path: '/sales', icon: ShoppingCart },
      { label: 'Sales History', path: '/sales-history', icon: Receipt },
    ],
  },
  {
    id: 'inventory',
    title: 'INVENTORY',
    items: [
      { label: 'Products', path: '/products', icon: Package },
      { label: 'Categories', path: '/categories', icon: Layers },
      { label: 'Stock Control', path: '/inventory', icon: Boxes },
      { label: 'Purchases', path: '/purchases', icon: Truck },
    ],
  },
  {
    id: 'contacts',
    title: 'CONTACTS',
    items: [
      { label: 'Customers', path: '/customers', icon: Users },
      { label: 'Suppliers', path: '/suppliers', icon: Building2 },
    ],
  },
  {
    id: 'intelligence',
    title: 'INTELLIGENCE',
    items: [
      { label: 'Reports', path: '/reports', icon: BarChart3 },
      { label: 'Notifications', path: '/notifications', icon: Bell, hasBadge: true },
    ],
  },
]

export function MainLayout() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  // Default to collapsed floating rail (or read stored user preference)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try {
      const stored = localStorage.getItem('app-sidebar-collapsed')
      if (stored !== null) return stored === 'true'
      return window.innerWidth < 1280
    } catch {
      return true
    }
  })
  const [actionableCount, setActionableCount] = useState(0)
  const location = useLocation()
  const { isDark, toggleTheme } = useTheme()

  // Save sidebar collapsed state
  const handleToggleCollapse = useCallback(() => {
    setSidebarCollapsed((prev) => {
      const next = !prev
      try {
        localStorage.setItem('app-sidebar-collapsed', String(next))
      } catch {
        // ignore
      }
      return next
    })
  }, [])

  // Auto-collapse on tablet / medium screens
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024 && window.innerWidth < 1280) {
        setSidebarCollapsed(true)
      }
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // Fetch actionable count for notification badge
  const fetchActionableBadge = useCallback(async () => {
    try {
      const res = await notificationService.getActionableCount()
      const count = typeof res === 'number' ? res : (res?.data ?? 0)
      setActionableCount(Number(count) || 0)
    } catch {
      // Fallback
    }
  }, [])

  useEffect(() => {
    fetchActionableBadge()
    const interval = setInterval(fetchActionableBadge, 15000)
    return () => clearInterval(interval)
  }, [fetchActionableBadge])

  // Real-time synchronization for badge updates
  useEffect(() => {
    const unsub1 = subscribeEvent(AppEvents.INVOICE_CREATED, fetchActionableBadge)
    const unsub2 = subscribeEvent(AppEvents.INVOICE_CONFIRMED, fetchActionableBadge)
    const unsub3 = subscribeEvent(AppEvents.INVOICE_CANCELLED, fetchActionableBadge)
    const unsub4 = subscribeEvent(AppEvents.INVOICE_PAID, fetchActionableBadge)
    const unsub5 = subscribeEvent(AppEvents.EMAIL_SENT, fetchActionableBadge)
    const unsub6 = subscribeEvent(AppEvents.EMAIL_FAILED, fetchActionableBadge)
    const unsub7 = subscribeEvent(AppEvents.EMAIL_SKIPPED, fetchActionableBadge)
    const unsub8 = subscribeEvent(AppEvents.STOCK_UPDATED, fetchActionableBadge)
    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
      unsub5()
      unsub6()
      unsub7()
      unsub8()
    }
  }, [fetchActionableBadge])

  // Close mobile drawer on route change
  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

  // Lock body scroll when mobile menu open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileMenuOpen])

  const currentPageTitle = NAV_GROUPS
    .flatMap((s) => s.items)
    .find((item) => {
      if (item.path === '/') return location.pathname === '/'
      return location.pathname.startsWith(item.path)
    })?.label || 'Dashboard'

  // Logout handler
  const handleLogout = () => {
    if (window.confirm('Are you sure you want to log out of SmartInventory?')) {
      window.location.reload()
    }
  }

  // Floating Sidebar Component
  const renderSidebarContent = (isMobile = false) => {
    const isCollapsed = !isMobile && sidebarCollapsed

    return (
      <div className="flex flex-col h-full bg-[#111827] text-slate-300 select-none py-4 px-2.5">
        {/* TOP: Brand Logo & Collapse Toggle */}
        <div className={`flex items-center shrink-0 mb-3 px-1 transition-all duration-300 ${isCollapsed ? 'flex-col gap-2 items-center' : 'justify-between px-2'}`}>
          <NavLink
            to="/"
            className="flex items-center gap-3 group focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 rounded-2xl"
            title="SmartInventory Home"
          >
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-purple-600 via-purple-500 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-purple-600/35 group-hover:scale-105 group-hover:shadow-purple-600/50 transition-all duration-200 shrink-0">
              <Boxes className="w-5 h-5" strokeWidth={2} />
            </div>

            {(!isCollapsed || isMobile) && (
              <div className="overflow-hidden whitespace-nowrap transition-opacity duration-200">
                <span className="font-extrabold text-[15px] tracking-tight text-white block leading-tight">
                  Smart<span className="text-purple-400">Inventory</span>
                </span>
                <span className="text-[10px] font-semibold tracking-wider uppercase text-slate-400 flex items-center gap-1 mt-0.5">
                  <Sparkles className="w-2.5 h-2.5 text-purple-400" /> Enterprise
                </span>
              </div>
            )}
          </NavLink>

          {isMobile ? (
            <button
              onClick={() => setMobileMenuOpen(false)}
              className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/[0.08] transition-colors"
              aria-label="Close menu drawer"
            >
              <X className="w-5 h-5" />
            </button>
          ) : (
            <button
              onClick={handleToggleCollapse}
              className={`p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/[0.08] transition-all duration-200 cursor-pointer ${
                isCollapsed ? 'hover:scale-110 text-purple-400' : ''
              }`}
              title={isCollapsed ? 'Expand sidebar (250px)' : 'Collapse to rail'}
              aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {isCollapsed ? (
                <PanelLeft className="w-4 h-4" strokeWidth={2} />
              ) : (
                <PanelLeftClose className="w-4 h-4" strokeWidth={2} />
              )}
            </button>
          )}
        </div>

        {/* Divider below header */}
        <div className="w-full h-px bg-white/[0.06] mb-3" />

        {/* CENTER: Navigation Icons (with subtle group dividers) */}
        <nav className="flex-1 overflow-y-auto space-y-3 scrollbar-none py-1">
          {NAV_GROUPS.map((group, groupIdx) => (
            <div key={group.id} className="space-y-1">
              {/* Section Header or Divider */}
              {groupIdx > 0 && (
                <div className="py-1">
                  {(!isCollapsed || isMobile) ? (
                    <div className="px-3 text-[10px] font-extrabold tracking-[0.16em] text-slate-500 uppercase">
                      {group.title}
                    </div>
                  ) : (
                    <div className="w-7 h-px bg-white/[0.08] mx-auto my-1.5" />
                  )}
                </div>
              )}

              {/* Items in group */}
              <div className="space-y-1">
                {group.items.map((item) => {
                  const Icon = item.icon
                  const hasBadge = item.hasBadge && actionableCount > 0

                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      end={item.path === '/'}
                      className={({ isActive }) =>
                        `group relative flex items-center transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 ${
                          isCollapsed
                            ? 'w-11 h-11 mx-auto justify-center rounded-full'
                            : 'px-3.5 py-2.5 gap-3 rounded-2xl mx-1'
                        } ${
                          isActive
                            ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white font-semibold shadow-lg shadow-purple-600/35 ring-1 ring-purple-400/30'
                            : 'text-[#9CA3AF] hover:text-[#A855F7] hover:bg-purple-600/15 hover:scale-[1.03]'
                        }`
                      }
                      title={isCollapsed ? item.label : undefined}
                    >
                      {({ isActive }) => (
                        <>
                          <Icon
                            className={`w-5 h-5 shrink-0 transition-colors duration-200 ${
                              isActive ? 'text-white' : 'text-[#9CA3AF] group-hover:text-[#A855F7]'
                            }`}
                            strokeWidth={isActive ? 2.2 : 1.9}
                          />

                          {(!isCollapsed || isMobile) && (
                            <span className="flex-1 truncate tracking-tight text-[13px]">{item.label}</span>
                          )}

                          {/* Actionable Notification Badge */}
                          {hasBadge && (
                            <span
                              className={`px-1.5 py-0.5 rounded-full text-[10px] font-extrabold shadow-sm ${
                                isActive
                                  ? 'bg-white text-purple-700 ring-1 ring-white/40'
                                  : 'bg-rose-500 text-white ring-1 ring-white/20'
                              } ${
                                isCollapsed
                                  ? 'absolute -top-1 -right-1 w-4 h-4 flex items-center justify-center p-0 text-[9px]'
                                  : ''
                              }`}
                            >
                              {actionableCount > 99 ? '99+' : actionableCount}
                            </span>
                          )}

                          {/* Collapsed Tooltip on Hover */}
                          {isCollapsed && (
                            <div className="absolute left-full ml-3 px-3 py-1.5 bg-[#0f172a] text-white text-xs font-semibold rounded-xl shadow-2xl opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity z-50 whitespace-nowrap border border-white/10 flex items-center gap-2">
                              <span>{item.label}</span>
                              {hasBadge && (
                                <span className="px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-rose-500 text-white">
                                  {actionableCount}
                                </span>
                              )}
                            </div>
                          )}
                        </>
                      )}
                    </NavLink>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Divider above bottom actions */}
        <div className="w-full h-px bg-white/[0.06] my-2" />

        {/* BOTTOM: Settings, Profile Card & Logout */}
        <div className="shrink-0 space-y-1.5 pt-1">
          {/* Settings Nav Item */}
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              `group relative flex items-center transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 ${
                isCollapsed
                  ? 'w-11 h-11 mx-auto justify-center rounded-full'
                  : 'px-3.5 py-2.5 gap-3 rounded-2xl mx-1'
              } ${
                isActive
                  ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white font-semibold shadow-lg shadow-purple-600/35'
                  : 'text-[#9CA3AF] hover:text-[#A855F7] hover:bg-purple-600/15'
              }`
            }
            title={isCollapsed ? 'Settings' : undefined}
          >
            {({ isActive }) => (
              <>
                <Settings
                  className={`w-5 h-5 shrink-0 transition-colors duration-200 ${
                    isActive ? 'text-white' : 'text-[#9CA3AF] group-hover:text-[#A855F7]'
                  }`}
                  strokeWidth={isActive ? 2.2 : 1.9}
                />
                {(!isCollapsed || isMobile) && (
                  <span className="flex-1 truncate tracking-tight text-[13px]">Settings</span>
                )}
                {isCollapsed && (
                  <div className="absolute left-full ml-3 px-3 py-1.5 bg-[#0f172a] text-white text-xs font-semibold rounded-xl shadow-2xl opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity z-50 whitespace-nowrap border border-white/10">
                    Settings
                  </div>
                )}
              </>
            )}
          </NavLink>

          {/* User Profile Card */}
          <div
            className={`flex items-center gap-2.5 p-2 rounded-2xl bg-white/[0.03] border border-white/[0.05] hover:bg-white/[0.06] transition-all duration-200 ${
              isCollapsed ? 'justify-center p-1.5' : 'mx-1'
            }`}
          >
            <div className="relative shrink-0">
              <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-600 to-indigo-500 text-white flex items-center justify-center font-bold text-xs shadow-md ring-2 ring-purple-400/25">
                AD
              </div>
              {/* Online Green Indicator */}
              <span className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 border-2 border-[#111827] rounded-full" />
            </div>

            {(!isCollapsed || isMobile) && (
              <div className="flex-1 min-w-0">
                <p className="text-xs font-bold text-white truncate leading-tight">Administrator</p>
                <p className="text-[10px] text-slate-400 truncate mt-0.5">Store #01 &bull; Warehouse</p>
              </div>
            )}

            {(!isCollapsed || isMobile) && (
              <button
                onClick={handleLogout}
                className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors cursor-pointer"
                title="Logout"
                aria-label="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-screen w-screen max-w-full overflow-hidden bg-[var(--color-surface-secondary)] text-[var(--color-text-primary)]">
      {/* Mobile Backdrop Overlay */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.25 }}
            onClick={() => setMobileMenuOpen(false)}
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs lg:hidden"
            aria-hidden="true"
          />
        )}
      </AnimatePresence>

      {/* Mobile Slide-out Drawer */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.aside
            initial={{ x: '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: '-100%' }}
            transition={{ type: 'spring', damping: 28, stiffness: 280 }}
            className="fixed inset-y-0 left-0 z-50 w-[260px] bg-[#111827] shadow-2xl flex flex-col lg:hidden"
            aria-label="Mobile Navigation Drawer"
          >
            {renderSidebarContent(true)}
          </motion.aside>
        )}
      </AnimatePresence>

      {/* Floating Pill Navigation Rail for Desktop & Tablet */}
      <div className="hidden lg:flex flex-col shrink-0 p-3 sm:p-4 pr-0 z-30 select-none">
        <aside
          className={`flex flex-col h-[calc(100vh-1.5rem)] sm:h-[calc(100vh-2rem)] rounded-[32px] bg-[#111827] border border-white/[0.06] shadow-2xl shadow-black/40 transition-[width] duration-300 ease-in-out relative overflow-hidden ${
            sidebarCollapsed ? 'w-[76px]' : 'w-[250px]'
          }`}
          aria-label="Desktop Navigation Rail"
        >
          {renderSidebarContent(false)}
        </aside>
      </div>

      {/* Main App Layout */}
      <div className="flex-1 flex flex-col min-w-0 max-w-full overflow-hidden">
        {/* Top Bar (#0f172a floating with border rgba(255,255,255,0.08)) */}
        <header className="h-16 bg-[var(--color-header-bg)] backdrop-blur-xl border-b border-[var(--color-header-border)] px-4 sm:px-6 flex items-center justify-between gap-4 sticky top-0 z-20 shrink-0 transition-colors">
          <div className="flex items-center gap-3 flex-1 max-w-md">
            {/* Mobile Hamburger Toggle */}
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden p-2 rounded-xl text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none cursor-pointer"
              aria-label="Open mobile navigation menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            {/* Page Title (Mobile) */}
            <h2 className="sm:hidden font-extrabold text-[var(--color-text-primary)] text-sm truncate">
              {currentPageTitle}
            </h2>

            {/* Global Search Bar (Desktop) */}
            <div className="relative w-full hidden sm:block">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-tertiary)]" />
              <input
                type="text"
                placeholder="Search inventory, invoices, products, customers..."
                className="w-full pl-10 pr-4 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] placeholder-[var(--color-input-placeholder)] focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/20 transition-all"
              />
            </div>
          </div>

          {/* Right Action Icons */}
          <div className="flex items-center gap-3">
            {/* Theme Radial Reveal Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2.5 rounded-xl text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 cursor-pointer"
              title={isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme'}
              aria-label="Toggle light and dark theme"
            >
              <AnimatePresence mode="wait" initial={false}>
                {isDark ? (
                  <motion.div
                    key="sun"
                    initial={{ rotate: -90, opacity: 0 }}
                    animate={{ rotate: 0, opacity: 1 }}
                    exit={{ rotate: 90, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                  >
                    <Sun className="w-5 h-5 text-amber-400" />
                  </motion.div>
                ) : (
                  <motion.div
                    key="moon"
                    initial={{ rotate: 90, opacity: 0 }}
                    animate={{ rotate: 0, opacity: 1 }}
                    exit={{ rotate: 90, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                  >
                    <Moon className="w-5 h-5 text-slate-700" />
                  </motion.div>
                )}
              </AnimatePresence>
            </button>

            {/* Actionable Notification Bell */}
            <NavLink
              to="/notifications"
              className="relative p-2.5 rounded-xl text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500"
              title="Notifications Audit Log"
              aria-label="Notifications"
            >
              <Bell className="w-5 h-5" />
              {actionableCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-rose-500 text-white text-[9px] font-black rounded-full flex items-center justify-center ring-2 ring-[var(--color-header-bg)] animate-bounce">
                  {actionableCount > 9 ? '9+' : actionableCount}
                </span>
              )}
            </NavLink>
          </div>
        </header>

        {/* Main Content Area */}
        <main className="flex-1 min-w-0 w-full overflow-y-auto overflow-x-hidden">
          <PageContainer>
            <ErrorBoundary key={location.pathname}>
              <Outlet />
            </ErrorBoundary>
          </PageContainer>
        </main>
      </div>
    </div>
  )
}

export default MainLayout
