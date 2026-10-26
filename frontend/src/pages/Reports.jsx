import React, { useState, useEffect } from 'react'
import {
  BarChart3,
  DollarSign,
  TrendingUp,
  Boxes,
  Truck,
  RefreshCw,
  PieChart,
} from 'lucide-react'
import Card, { CardHeader, CardBody } from '../components/Card'
import Table from '../components/Table'
import Button from '../components/Button'
import StatsCard from '../components/StatsCard'
import AreaChart from '../components/AreaChart'
import AnimatedPage, { StaggerContainer, StaggerItem } from '../components/AnimatedPage'
import { reportService } from '../services/reportService'
import { supplierService } from '../services/supplierService'
import { formatCurrency, formatNumber, formatDate } from '../utils/formatters'
import { useToast } from '../context/ToastContext'
import { AppEvents, subscribeEvent } from '../utils/eventBus'

const REPORT_TABS = [
  { id: 'SALES', label: 'Sales Revenue Report' },
  { id: 'INVENTORY', label: 'Inventory Valuation Audit' },
  { id: 'PURCHASES', label: 'Procurement Expenses' },
]

export function Reports() {
  const { error } = useToast()

  const [activeTab, setActiveTab] = useState('SALES')
  const [isLoading, setIsLoading] = useState(true)

  // Sales Report State
  const [salesReport, setSalesReport] = useState(null)
  const [salesGroupBy, setSalesGroupBy] = useState('DAILY')
  const [salesStartDate, setSalesStartDate] = useState('')
  const [salesEndDate, setSalesEndDate] = useState('')

  // Inventory Valuation State
  const [inventoryReport, setInventoryReport] = useState(null)

  // Purchases Report State
  const [purchaseReport, setPurchaseReport] = useState(null)
  const [suppliers, setSuppliers] = useState([])
  const [purchaseSupplierId, setPurchaseSupplierId] = useState('')

  useEffect(() => {
    supplierService.getAllActiveSuppliers().then(setSuppliers).catch(() => {})
  }, [])

  const fetchCurrentReport = async () => {
    setIsLoading(true)
    try {
      if (activeTab === 'SALES') {
        const res = await reportService.getSalesReport({
          startDate: salesStartDate || undefined,
          endDate: salesEndDate || undefined,
          groupBy: salesGroupBy,
        })
        setSalesReport(res)
      } else if (activeTab === 'INVENTORY') {
        const res = await reportService.getInventoryValuationReport()
        setInventoryReport(res)
      } else if (activeTab === 'PURCHASES') {
        const res = await reportService.getPurchaseReport({
          supplierId: purchaseSupplierId || undefined,
        })
        setPurchaseReport(res)
      }
    } catch (err) {
      error(err.message || 'Failed to load report data')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchCurrentReport()
  }, [activeTab, salesGroupBy, salesStartDate, salesEndDate, purchaseSupplierId])

  // Listen to application events for real-time automatic refresh without page reloads
  useEffect(() => {
    const unsub1 = subscribeEvent(AppEvents.INVOICE_CREATED, fetchCurrentReport)
    const unsub2 = subscribeEvent(AppEvents.INVOICE_CONFIRMED, fetchCurrentReport)
    const unsub3 = subscribeEvent(AppEvents.INVOICE_CANCELLED, fetchCurrentReport)
    const unsub4 = subscribeEvent(AppEvents.INVOICE_PAID, fetchCurrentReport)
    const unsub5 = subscribeEvent(AppEvents.STOCK_UPDATED, fetchCurrentReport)
    const unsub6 = subscribeEvent(AppEvents.SUPPLIER_UPDATED, fetchCurrentReport)
    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
      unsub5()
      unsub6()
    }
  }, [activeTab, salesGroupBy, salesStartDate, salesEndDate, purchaseSupplierId])

  // Breakdown list from Sales Report (supports breakdown or dataPoints)
  const salesBreakdownList = salesReport?.dataPoints || salesReport?.breakdown || []

  // Chart data from Sales Report
  const chartPoints =
    salesBreakdownList.map((dp) => ({
      label: dp.period,
      value: Number(dp.revenue ?? dp.totalAmount ?? 0),
    }))

  // Columns for Sales Breakdown Table
  const salesColumns = [
    {
      header: 'Reporting Period',
      accessor: 'period',
      render: (row) => <span className="font-bold text-[var(--color-text-primary)] text-xs sm:text-sm">{row.period}</span>,
    },
    {
      header: 'Bills Finalized',
      accessor: 'billCount',
      align: 'center',
      render: (row) => <span className="font-semibold text-[var(--color-text-secondary)]">{row.billCount}</span>,
    },
    {
      header: 'Total Revenue',
      accessor: 'totalAmount',
      align: 'right',
      render: (row) => (
        <span className="font-extrabold text-[var(--color-text-primary)] text-sm">
          {formatCurrency(row.revenue ?? row.totalAmount ?? 0)}
        </span>
      ),
    },
  ]

  // Columns for Inventory Valuation Table
  const inventoryColumns = [
    {
      header: 'Product Name',
      accessor: 'productName',
      render: (row) => (
        <div>
          <p className="font-bold text-[var(--color-text-primary)] text-sm">{row.productName}</p>
          <p className="text-[11px] font-mono text-[var(--color-text-tertiary)]">{row.sku}</p>
        </div>
      ),
    },
    {
      header: 'Category',
      accessor: 'categoryName',
      render: (row) => (
        <span className="text-xs text-[var(--color-text-secondary)] bg-[var(--color-surface-tertiary)] px-2 py-0.5 rounded-md border border-[var(--color-border-secondary)]">
          {row.categoryName || 'General'}
        </span>
      ),
    },
    {
      header: 'In Stock',
      accessor: 'quantityOnHand',
      align: 'right',
      render: (row) => <span className="font-bold text-[var(--color-text-primary)]">{row.quantityOnHand} units</span>,
    },
    {
      header: 'Cost Price',
      accessor: 'purchasePrice',
      align: 'right',
      render: (row) => <span className="text-xs text-[var(--color-text-tertiary)]">{formatCurrency(row.purchasePrice)}</span>,
    },
    {
      header: 'Retail Price',
      accessor: 'unitPrice',
      align: 'right',
      render: (row) => (
        <span className="text-xs font-semibold text-[var(--color-text-secondary)]">{formatCurrency(row.unitPrice)}</span>
      ),
    },
    {
      header: 'Total Cost Val',
      accessor: 'totalCostValue',
      align: 'right',
      render: (row) => (
        <span className="text-xs font-semibold text-[var(--color-text-secondary)]">{formatCurrency(row.totalCostValue)}</span>
      ),
    },
    {
      header: 'Total Retail Val',
      accessor: 'totalRetailValue',
      align: 'right',
      render: (row) => (
        <span className="font-extrabold text-[var(--color-text-primary)] text-xs sm:text-sm">
          {formatCurrency(row.totalRetailValue)}
        </span>
      ),
    },
  ]

  // Columns for Procurement Breakdown Table
  const purchaseColumns = [
    {
      header: 'Supplier Vendor',
      accessor: 'supplierName',
      render: (row) => <span className="font-bold text-[var(--color-text-primary)] text-xs sm:text-sm">{row.supplierName}</span>,
    },
    {
      header: 'Orders Received',
      accessor: 'orderCount',
      align: 'center',
      render: (row) => <span className="font-semibold text-[var(--color-text-secondary)]">{row.orderCount}</span>,
    },
    {
      header: 'Total Procurement Expense',
      accessor: 'totalExpense',
      align: 'right',
      render: (row) => (
        <span className="font-extrabold text-[var(--color-text-primary)] text-sm">
          {formatCurrency(row.totalExpense ?? row.totalAmount ?? 0)}
        </span>
      ),
    },
  ]

  return (
    <AnimatedPage className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Financial & Inventory Reports
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Aggregated revenue audits, warehouse asset valuation, and procurement tracking.
          </p>
        </div>

        <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchCurrentReport}>
          Refresh Report
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-[var(--color-border-secondary)] overflow-x-auto pb-1">
        {REPORT_TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2.5 text-xs sm:text-sm font-bold border-b-2 transition-all whitespace-nowrap cursor-pointer ${
              activeTab === tab.id
                ? 'border-[var(--color-accent)] text-[var(--color-accent)]'
                : 'border-transparent text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* TAB 1: SALES REVENUE REPORT */}
      {activeTab === 'SALES' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* Filter Bar */}
          <Card className="p-4 flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-2 text-xs">
                <span className="text-[var(--color-text-tertiary)] font-medium">Date Range:</span>
                <input
                  type="date"
                  value={salesStartDate}
                  onChange={(e) => setSalesStartDate(e.target.value)}
                  className="px-2.5 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] text-[var(--color-text-primary)] rounded-lg text-xs font-semibold focus:outline-hidden focus:border-[var(--color-accent)]"
                />
                <span className="text-[var(--color-text-tertiary)]">to</span>
                <input
                  type="date"
                  value={salesEndDate}
                  onChange={(e) => setSalesEndDate(e.target.value)}
                  className="px-2.5 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] text-[var(--color-text-primary)] rounded-lg text-xs font-semibold focus:outline-hidden focus:border-[var(--color-accent)]"
                />
              </div>

              <div className="flex items-center bg-[var(--color-surface-tertiary)] p-0.5 rounded-xl text-xs font-semibold border border-[var(--color-border-secondary)]">
                {['DAILY', 'MONTHLY'].map((g) => (
                  <button
                    key={g}
                    onClick={() => setSalesGroupBy(g)}
                    className={`px-3 py-1 rounded-lg transition-all cursor-pointer ${
                      salesGroupBy === g
                        ? 'bg-[var(--color-card-bg)] text-[var(--color-accent)] font-bold shadow-xs'
                        : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                    }`}
                  >
                    {g}
                  </button>
                ))}
              </div>
            </div>
          </Card>

          {/* Sales KPIs */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <StatsCard
              title="Total Revenue"
              value={formatCurrency(salesReport?.totalRevenue ?? salesReport?.netSales ?? 0)}
              subtitle={`From ${salesBreakdownList.length} reporting periods`}
              change="Verified Sales"
              icon={DollarSign}
              iconBg="bg-purple-500/10 text-purple-600 dark:text-purple-400"
            />
            <StatsCard
              title="Total Bills Issued"
              value={formatNumber(
                salesReport?.totalBills != null
                  ? salesReport.totalBills
                  : salesBreakdownList.reduce((acc, curr) => acc + (curr.billCount || 0), 0)
              )}
              subtitle="Paid and confirmed customer invoices"
              change="Active Sales"
              icon={BarChart3}
              iconBg="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
            />
            <StatsCard
              title="Average Bill Value"
              value={formatCurrency(
                salesReport?.averageBillValue != null
                  ? salesReport.averageBillValue
                  : (salesReport?.totalBills > 0 ? (salesReport.totalRevenue / salesReport.totalBills) : 0)
              )}
              subtitle="Basket size per customer bill"
              change="Key Metric"
              icon={TrendingUp}
              iconBg="bg-sky-500/10 text-sky-600 dark:text-sky-400"
            />
          </div>

          {/* Revenue Chart */}
          <Card className="p-6">
            <AreaChart
              title={`Revenue Trend (${salesGroupBy})`}
              data={chartPoints}
              height={260}
            />
          </Card>

          {/* Data Table */}
          <Card>
            <CardHeader title="Periodic Breakdown" subtitle="Detailed revenue audit per interval" />
            <Table
              columns={salesColumns}
              data={salesBreakdownList}
              isLoading={isLoading}
              emptyMessage="No sales data found for this range"
            />
          </Card>
        </div>
      )}

      {/* TAB 2: INVENTORY VALUATION AUDIT */}
      {activeTab === 'INVENTORY' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
            <StatsCard
              title="Total Stock Units"
              value={formatNumber(inventoryReport?.totalStockUnits ?? inventoryReport?.totalStockQuantity ?? 0)}
              subtitle="Items across all warehouse bins"
              change="Physical Stock"
              icon={Boxes}
              iconBg="bg-purple-500/10 text-purple-600 dark:text-purple-400"
            />
            <StatsCard
              title="Cost Value (Capital)"
              value={formatCurrency(inventoryReport?.totalCostValue || 0)}
              subtitle="Total procurement expenditure"
              change="Asset Basis"
              icon={DollarSign}
              iconBg="bg-slate-500/10 text-slate-700 dark:text-slate-300"
            />
            <StatsCard
              title="Retail Value"
              value={formatCurrency(inventoryReport?.totalRetailValue || 0)}
              subtitle="Potential selling value"
              change="Market Value"
              icon={TrendingUp}
              iconBg="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
            />
            <StatsCard
              title="Potential Gross Margin"
              value={formatCurrency(
                (inventoryReport?.totalRetailValue || 0) - (inventoryReport?.totalCostValue || 0)
              )}
              subtitle="Projected profit upon sell-through"
              change="+31.2%"
              icon={PieChart}
              iconBg="bg-sky-500/10 text-sky-600 dark:text-sky-400"
            />
          </div>

          <Card>
            <CardHeader
              title="Inventory Asset Valuation Breakdown"
              subtitle="Item-by-item capital investment vs retail potential"
            />
            <Table
              columns={inventoryColumns}
              data={inventoryReport?.items || []}
              isLoading={isLoading}
              emptyMessage="No inventory items to evaluate"
            />
          </Card>
        </div>
      )}

      {/* TAB 3: PROCUREMENT EXPENSES */}
      {activeTab === 'PURCHASES' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <Card className="p-4 flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="text-xs text-[var(--color-text-tertiary)] font-medium">Filter Supplier:</span>
              <select
                value={purchaseSupplierId}
                onChange={(e) => setPurchaseSupplierId(e.target.value)}
                className="px-3 py-1.5 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] text-[var(--color-text-primary)] rounded-xl text-xs sm:text-sm font-semibold cursor-pointer"
              >
                <option value="">All Suppliers</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </Card>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <StatsCard
              title="Total Procurement Spend"
              value={formatCurrency(purchaseReport?.totalExpense || 0)}
              subtitle="Fulfilled purchase shipments"
              change="Vendor Invoices"
              icon={Truck}
              iconBg="bg-indigo-50 text-indigo-600"
            />
            <StatsCard
              title="Orders Completed"
              value={formatNumber(purchaseReport?.totalOrders || 0)}
              subtitle="Delivered purchase requisitions"
              change="Procurement"
              icon={BarChart3}
              iconBg="bg-emerald-50 text-emerald-600"
            />
            <StatsCard
              title="Average Order Size"
              value={formatCurrency(
                purchaseReport?.totalOrders
                  ? (purchaseReport.totalExpense || 0) / purchaseReport.totalOrders
                  : 0
              )}
              subtitle="Average purchase order spend"
              change="PO Size"
              icon={DollarSign}
              iconBg="bg-sky-50 text-sky-600"
            />
          </div>

          <Card>
            <CardHeader title="Procurement by Supplier" subtitle="Vendor spend distribution and volume" />
            <Table
              columns={purchaseColumns}
              data={purchaseReport?.breakdown || purchaseReport?.supplierBreakdowns || []}
              isLoading={isLoading}
              emptyMessage="No procurement records found"
            />
          </Card>
        </div>
      )}
    </AnimatedPage>
  )
}

export default Reports
