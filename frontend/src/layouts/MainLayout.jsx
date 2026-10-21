import { Outlet } from 'react-router-dom'

/**
 * MainLayout — shared shell for all pages.
 * Sidebar navigation and top header will be implemented in Phase 5 (Frontend).
 */
function MainLayout() {
  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar — placeholder for Phase 5 */}
      <aside className="w-64 bg-white border-r border-gray-200 flex items-center justify-center">
        <p className="text-gray-400 text-sm">Sidebar (Phase 5)</p>
      </aside>

      {/* Main content area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top header — placeholder for Phase 5 */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center px-6">
          <p className="text-gray-400 text-sm">Header (Phase 5)</p>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default MainLayout
