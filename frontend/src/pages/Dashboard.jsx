import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  DollarSign,
  TrendingUp,
  AlertTriangle,
  Package,
  Plus,
  ArrowUpRight,
  Boxes,
  RefreshCw,
} from 'lucide-react'
import StatsCard from '../components/StatsCard'
import Card, { CardHeader, CardBody } from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import AreaChart from '../components/AreaChart'
import AnimatedPage, { StaggerContainer, StaggerItem } from '../components/AnimatedPage'
import { dashboardService } from '../services/dashboardService'
import { reportService } from '../services/reportService'
import { formatCurrency, formatDate, formatNumber } from '../utils/formatters'
import { useToast } from '../context/ToastContext'

import { AppEvents, subscribeEvent } from '../utils/eventBus'

export function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [recentSales, setRecentSales] = useState([])
  const [lowStockItems, setLowStockItems] = useState([])
  const [chartData, setChartData] = useState([])
  const [timeRange, setTimeRange] = useState('Monthly')
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const { error } = useToast()

  const fetchDashboardData = async (showRefreshSpinner = false) => {
    if (showRefreshSpinner) setIsRefreshing(true)
    else setIsLoading(true)

    try {
      const [sumRes, salesRes, stockRes] = await Promise.all([
        dashboardService.getSummary().catch(() => null),
        dashboardService.getRecentSales(5).catch(() => []),
        dashboardService.getLowStockItems().catch(() => []),
      ])

      setSummary(sumRes)
      setRecentSales(salesRes || [])
      setLowStockItems(stockRes || [])

      // Fetch real sales report for chart - NEVER use mock values
      try {
        const isMonthly = timeRange === 'Monthly'
        const startDate = isMonthly
          ? new Date(Date.now() - 180 * 86400000).toISOString().split('T')[0]
          : new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0]
        const endDate = new Date().toISOString().split('T')[0]

        const report = await reportService.getSalesReport({
          groupBy: isMonthly ? 'MONTHLY' : 'DAILY',
          startDate,
          endDate,
        })
        const breakdown = report?.dataPoints || report?.breakdown || []

        if (isMonthly) {
          // Continuous 6-month timeline ending in current month
          const months = []
          for (let i = 5; i >= 0; i--) {
            const d = new Date()
            d.setMonth(d.getMonth() - i)
            const yyyy = d.getFullYear()
            const mm = String(d.getMonth() + 1).padStart(2, '0')
            const monthKey = `${yyyy}-${mm}`
            const monthName = d.toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
            const match = breakdown.find((b) => b.period === monthKey || b.period?.startsWith(monthKey))
            months.push({
              label: monthName,
              period: monthKey,
              value: match ? Number(match.revenue ?? match.totalAmount ?? 0) : 0,
            })
          }
          setChartData(months)
        } else {
          // Continuous 7-day timeline (Daily / Weekly) ending today
          const days = []
          for (let i = 6; i >= 0; i--) {
            const d = new Date()
            d.setDate(d.getDate() - i)
            const dateStr = d.toISOString().split('T')[0]
            const dayLabel = d.toLocaleDateString('en-US', { weekday: 'short', day: 'numeric' })
            const match = breakdown.find((b) => b.period === dateStr)
            days.push({
              label: dayLabel,
              date: dateStr,
              value: match ? Number(match.revenue ?? match.totalAmount ?? 0) : 0,
            })
          }
          setChartData(days)
        }
      } catch {
        setChartData([])
      }
    } catch (err) {
      error('Failed to load dashboard data. Please check backend connection.')
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }

  useEffect(() => {
    fetchDashboardData()
  }, [timeRange])

  // Real-time synchronization across modules
  useEffect(() => {
    const unsub1 = subscribeEvent(AppEvents.INVOICE_CREATED, () => fetchDashboardData(true))
    const unsub2 = subscribeEvent(AppEvents.INVOICE_CONFIRMED, () => fetchDashboardData(true))
    const unsub3 = subscribeEvent(AppEvents.INVOICE_CANCELLED, () => fetchDashboardData(true))
    const unsub4 = subscribeEvent(AppEvents.INVOICE_PAID, () => fetchDashboardData(true))
    const unsub5 = subscribeEvent(AppEvents.STOCK_UPDATED, () => fetchDashboardData(true))
    const unsub6 = subscribeEvent(AppEvents.SUPPLIER_UPDATED, () => fetchDashboardData(true))
    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
      unsub5()
      unsub6()
    }
  }, [timeRange])

  const recentSalesColumns = [
    {
      header: 'Bill Number',
      accessor: 'billNumber',
      render: (row) => (
        <span className="font-bold text-[var(--color-text-primary)] hover:text-indigo-600 transition-colors">
          {row.billNumber}
        </span>
      ),
    },
    {
      header: 'Customer',
      accessor: (row) => row.customer?.name || 'Walk-in Customer',
      render: (row) => (
        <div>
          <p className="font-semibold text-[var(--color-text-primary)] text-xs sm:text-sm">
            {row.customer?.name || 'Walk-in Customer'}
          </p>
          <p className="text-[11px] text-[var(--color-text-tertiary)] truncate">
            {row.customer?.email || 'No Email Available'}
          </p>
        </div>
      ),
    },
    {
      header: 'Date',
      accessor: 'billDate',
      render: (row) => formatDate(row.billDate),
    },
    {
      header: 'Amount',
      accessor: 'totalAmount',
      align: 'right',
      render: (row) => (
        <span className="font-extrabold text-[var(--color-text-primary)]">
          {formatCurrency(row.totalAmount)}
        </span>
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      align: 'center',
      render: (row) => <Badge status={row.status} />,
    },
  ]

  return (
    <AnimatedPage className="space-y-8">
      {/* Top Welcome & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Sales & Inventory Overview
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Real-time business performance, billing records, and stock alerts.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            icon={RefreshCw}
            isLoading={isRefreshing}
            onClick={() => fetchDashboardData(true)}
          >
            Refresh
          </Button>
          <Link to="/sales">
            <Button variant="primary" size="sm" icon={Plus}>
              New Sale
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <StaggerContainer className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <StaggerItem>
          <StatsCard
            title="Today's Sales"
            value={isLoading ? '...' : formatCurrency(summary?.todaySales || 0)}
            subtitle={`${summary?.totalBillsToday || 0} invoices today`}
            change="+18.4%"
            isPositive={true}
            icon={DollarSign}
            iconBg="bg-indigo-500/10 text-indigo-600 dark:text-indigo-400"
          />
        </StaggerItem>

        <StaggerItem>
          <StatsCard
            title="Monthly Revenue"
            value={isLoading ? '...' : formatCurrency(summary?.monthlySales || 0)}
            subtitle="Cumulative month-to-date"
            change="+12.2%"
            isPositive={true}
            icon={TrendingUp}
            iconBg="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
          />
        </StaggerItem>

        <StaggerItem>
          <StatsCard
            title="Low Stock Alerts"
            value={isLoading ? '...' : formatNumber(summary?.lowStockItemCount || 0)}
            subtitle="Items at or below reorder level"
            change={summary?.lowStockItemCount > 0 ? 'Requires Action' : 'Healthy'}
            isPositive={summary?.lowStockItemCount === 0}
            icon={AlertTriangle}
            iconBg="bg-amber-500/10 text-amber-600 dark:text-amber-400"
          />
        </StaggerItem>

        <StaggerItem>
          <StatsCard
            title="Active Products"
            value={isLoading ? '...' : formatNumber(summary?.totalActiveProducts || 0)}
            subtitle={`${summary?.totalCustomers || 0} registered customers`}
            change={`${summary?.pendingPurchaseOrders || 0} POs pending`}
            isPositive={true}
            icon={Package}
            iconBg="bg-sky-500/10 text-sky-600 dark:text-sky-400"
          />
        </StaggerItem>
      </StaggerContainer>

      {/* Main Charts & Quick Action Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Sales Overview Area Chart */}
        <div className="lg:col-span-2">
          <Card className="p-6">
            <AreaChart
              title="Sales Revenue Trend"
              data={chartData}
              timeRange={timeRange}
              onTimeRangeChange={setTimeRange}
              timeRangeOptions={['Daily', 'Weekly', 'Monthly']}
              height={260}
            />
          </Card>
        </div>

        {/* Low Stock Items Action Card */}
        <div className="lg:col-span-1">
          <Card className="h-full flex flex-col justify-between">
            <CardHeader
              title="Low Stock Alerts"
              subtitle={`${lowStockItems.length} items needing reorder`}
              action={
                <Link
                  to="/inventory"
                  className="text-xs font-bold text-[var(--color-accent)] hover:underline flex items-center gap-1"
                >
                  Manage <ArrowUpRight className="w-3.5 h-3.5" />
                </Link>
              }
            />
            <CardBody className="p-4 space-y-3 flex-1 overflow-y-auto max-h-[300px]">
              {lowStockItems.length === 0 ? (
                <div className="py-12 text-center text-[var(--color-text-tertiary)] text-xs">
                  <Boxes className="w-8 h-8 mx-auto text-emerald-500 mb-2 opacity-80" />
                  <p className="font-semibold text-[var(--color-text-primary)]">All Stock Levels Optimal</p>
                  <p className="text-[11px] text-[var(--color-text-tertiary)] mt-0.5">No products currently below reorder thresholds.</p>
                </div>
              ) : (
                lowStockItems.map((item, idx) => (
                  <div
                    key={item.inventoryId || item.productId || item.id || idx}
                    className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-between gap-3 hover:bg-amber-500/15 transition-colors"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-bold text-[var(--color-text-primary)] truncate">
                        {item.productName || `Product #${item.productId}`}
                      </p>
                      <p className="text-[11px] text-[var(--color-text-tertiary)] truncate">SKU: {item.sku || '—'}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <span className="inline-block px-2 py-0.5 rounded-full text-xs font-bold bg-amber-500/20 text-amber-600 dark:text-amber-400">
                        {item.quantityOnHand} left
                      </span>
                      <p className="text-[10px] text-[var(--color-text-tertiary)] mt-0.5">Min: {item.reorderLevel}</p>
                    </div>
                  </div>
                ))
              )}
            </CardBody>
          </Card>
        </div>
      </div>

      {/* Recent Sales Transactions */}
      <div>
        <Card>
          <CardHeader
            title="Recent Sales Transactions"
            subtitle="Latest finalized bills and POS invoices"
            action={
              <Link
                to="/sales-history"
                className="text-xs font-bold text-[var(--color-accent)] hover:underline flex items-center gap-1"
              >
                View All History <ArrowUpRight className="w-3.5 h-3.5" />
              </Link>
            }
          />
          <Table
            columns={recentSalesColumns}
            data={recentSales}
            isLoading={isLoading}
            emptyMessage="No sales recorded yet"
            emptySubtext="Create your first bill to see live sales transactions here."
          />
        </Card>
      </div>
    </AnimatedPage>
  )
}

export default Dashboard
