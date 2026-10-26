import React, { useState, useEffect } from 'react'
import {
  Boxes,
  AlertTriangle,
  RefreshCw,
  ArrowUpDown,
  Settings2,
} from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'
import AnimatedPage from '../components/AnimatedPage'
import { inventoryService } from '../services/inventoryService'
import { formatNumber } from '../utils/formatters'
import { useToast } from '../context/ToastContext'

export function Inventory() {
  const { success, error } = useToast()

  // State
  const [inventory, setInventory] = useState([])
  const [lowStockOnly, setLowStockOnly] = useState(false)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [adjustingItem, setAdjustingItem] = useState(null)
  const [adjustForm, setAdjustForm] = useState({
    adjustedQuantity: '',
    adjustmentType: 'PHYSICAL_COUNT',
    reason: '',
  })
  const [isAdjusting, setIsAdjusting] = useState(false)

  const [settingsItem, setSettingsItem] = useState(null)
  const [settingsForm, setSettingsForm] = useState({
    reorderLevel: '',
    maxStockLevel: '',
  })
  const [isSavingSettings, setIsSavingSettings] = useState(false)

  const fetchInventory = async () => {
    setIsLoading(true)
    try {
      const res = await inventoryService.getInventoryList({
        lowStockOnly,
        page,
        size,
        sortBy: 'id',
        direction: 'ASC',
      })
      setInventory(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch inventory stock')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchInventory()
  }, [page, size, lowStockOnly])

  // Open Adjust Modal
  const handleOpenAdjust = (item) => {
    setAdjustingItem(item)
    setAdjustForm({
      adjustedQuantity: item.quantityOnHand,
      adjustmentType: 'PHYSICAL_COUNT',
      reason: '',
    })
  }

  // Submit Stock Adjustment
  const handleAdjustSubmit = async (e) => {
    e.preventDefault()
    if (!adjustingItem) return
    setIsAdjusting(true)
    try {
      await inventoryService.adjustStock(adjustingItem.productId, {
        adjustedQuantity: Number(adjustForm.adjustedQuantity),
        adjustmentType: adjustForm.adjustmentType,
        reason: adjustForm.reason.trim() || 'Manual stock reconciliation',
      })
      success(`Stock for '${adjustingItem.productName}' adjusted to ${adjustForm.adjustedQuantity}`)
      setAdjustingItem(null)
      fetchInventory()
    } catch (err) {
      error(err.message || 'Failed to adjust inventory stock')
    } finally {
      setIsAdjusting(false)
    }
  }

  // Open Settings Modal
  const handleOpenSettings = (item) => {
    setSettingsItem(item)
    setSettingsForm({
      reorderLevel: item.reorderLevel || 5,
      maxStockLevel: item.maxStockLevel || item.maxLevel || 100,
    })
  }

  // Submit Settings
  const handleSettingsSubmit = async (e) => {
    e.preventDefault()
    if (!settingsItem) return
    setIsSavingSettings(true)
    try {
      await inventoryService.updateReorderSettings(settingsItem.productId, {
        reorderLevel: Number(settingsForm.reorderLevel),
        maxStockLevel: Number(settingsForm.maxStockLevel),
      })
      success(`Inventory thresholds updated for '${settingsItem.productName}'`)
      setSettingsItem(null)
      fetchInventory()
    } catch (err) {
      error(err.message || 'Failed to update reorder settings')
    } finally {
      setIsSavingSettings(false)
    }
  }

  const columns = [
    {
      header: 'Product Details',
      accessor: 'productName',
      render: (row) => (
        <div>
          <p className="font-bold text-[var(--color-text-primary)] text-sm">{row.productName || `Product #${row.productId}`}</p>
          <p className="text-[11px] font-mono text-[var(--color-text-tertiary)]">SKU: {row.sku || '—'}</p>
        </div>
      ),
    },
    {
      header: 'Category',
      accessor: 'categoryName',
      render: (row) => (
        <span className="text-xs font-semibold text-[var(--color-text-secondary)] bg-[var(--color-surface-tertiary)] border border-[var(--color-border-secondary)] px-2 py-0.5 rounded-md">
          {row.categoryName || 'General'}
        </span>
      ),
    },
    {
      header: 'Current Stock',
      accessor: 'quantityOnHand',
      align: 'right',
      render: (row) => (
        <span
          className={`font-black text-sm sm:text-base ${
            row.quantityOnHand <= 0
              ? 'text-rose-500'
              : row.quantityOnHand <= row.reorderLevel
              ? 'text-amber-500'
              : 'text-[var(--color-text-primary)]'
          }`}
        >
          {formatNumber(row.quantityOnHand)} units
        </span>
      ),
    },
    {
      header: 'Reorder Level',
      accessor: 'reorderLevel',
      align: 'right',
      render: (row) => <span className="font-semibold text-[var(--color-text-secondary)] text-xs">{row.reorderLevel} units</span>,
    },
    {
      header: 'Max Capacity',
      accessor: 'maxLevel',
      align: 'right',
      render: (row) => (
        <span className="text-xs text-[var(--color-text-tertiary)]">{row.maxLevel ? `${row.maxLevel} units` : '—'}</span>
      ),
    },
    {
      header: 'Stock Status',
      align: 'center',
      render: (row) => {
        if (row.quantityOnHand <= 0) {
          return <Badge status="OUT_OF_STOCK" variant="danger" />
        }
        if (row.quantityOnHand <= row.reorderLevel) {
          return <Badge status="LOW_STOCK" variant="warning" />
        }
        return <Badge status="IN_STOCK" variant="success" />
      },
    },
    {
      header: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          <Button
            variant="outline"
            size="sm"
            icon={ArrowUpDown}
            onClick={() => handleOpenAdjust(row)}
            className="text-xs font-semibold"
          >
            Adjust
          </Button>
          <Button
            variant="ghost"
            size="sm"
            icon={Settings2}
            onClick={() => handleOpenSettings(row)}
            title="Configure Thresholds"
          />
        </div>
      ),
    },
  ]

  return (
    <AnimatedPage className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Inventory & Stock Control
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Warehouse inventory counts, reorder thresholds, and reconciliation audits.
          </p>
        </div>

        <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchInventory}>
          Refresh
        </Button>
      </div>

      {/* Tabs and Controls */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                setLowStockOnly(false)
                setPage(0)
              }}
              className={`px-4 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
                !lowStockOnly
                  ? 'bg-[var(--color-accent)] text-white shadow-xs'
                  : 'bg-[var(--color-surface-tertiary)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              All Warehouse Stock
            </button>
            <button
              onClick={() => {
                setLowStockOnly(true)
                setPage(0)
              }}
              className={`px-4 py-1.5 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                lowStockOnly
                  ? 'bg-amber-500 text-white shadow-xs'
                  : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 hover:bg-amber-500/20'
              }`}
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              Low Stock Alerts Only
            </button>
          </div>
        </div>

        {/* Table */}
        <Table
          columns={columns}
          data={inventory}
          isLoading={isLoading}
          emptyMessage={lowStockOnly ? 'No low stock alerts!' : 'No inventory items recorded'}
          emptySubtext={
            lowStockOnly
              ? 'All products are currently stocked above their reorder thresholds.'
              : 'Add products to start tracking inventory stock.'
          }
        />

        {/* Pagination */}
        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={size}
          onPageChange={setPage}
          onPageSizeChange={(newSize) => {
            setSize(newSize)
            setPage(0)
          }}
        />
      </Card>

      {/* Adjust Stock Modal */}
      <Modal
        isOpen={Boolean(adjustingItem)}
        onClose={() => setAdjustingItem(null)}
        title={`Adjust Stock — ${adjustingItem?.productName}`}
        subtitle={`Current Quantity: ${adjustingItem?.quantityOnHand} units`}
      >
        <form onSubmit={handleAdjustSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              New Count / Adjusted Quantity *
            </label>
            <input
              type="number"
              min="0"
              required
              value={adjustForm.adjustedQuantity}
              onChange={(e) => setAdjustForm({ ...adjustForm, adjustedQuantity: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-base font-bold focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Adjustment Reason Code *
            </label>
            <select
              value={adjustForm.adjustmentType}
              onChange={(e) => setAdjustForm({ ...adjustForm, adjustmentType: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            >
              <option value="PHYSICAL_COUNT">PHYSICAL_COUNT (Warehouse Audit)</option>
              <option value="DAMAGE">DAMAGE (Broken / Defective Goods)</option>
              <option value="LOSS">LOSS (Shrinkage / Missing)</option>
              <option value="RETURN">RETURN (Customer Restocking)</option>
              <option value="CORRECTION">CORRECTION (Data Entry Fix)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Audit Notes
            </label>
            <textarea
              rows="2"
              value={adjustForm.reason}
              onChange={(e) => setAdjustForm({ ...adjustForm, reason: e.target.value })}
              placeholder="e.g. End of month physical inventory reconciliation..."
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setAdjustingItem(null)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isAdjusting}>
              Save Stock Adjustment
            </Button>
          </div>
        </form>
      </Modal>

      {/* Threshold Settings Modal */}
      <Modal
        isOpen={Boolean(settingsItem)}
        onClose={() => setSettingsItem(null)}
        title={`Reorder Settings — ${settingsItem?.productName}`}
        subtitle="Configure automated low stock trigger levels"
      >
        <form onSubmit={handleSettingsSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Reorder Threshold *
              </label>
              <input
                type="number"
                min="1"
                required
                value={settingsForm.reorderLevel}
                onChange={(e) => setSettingsForm({ ...settingsForm, reorderLevel: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold"
              />
              <p className="text-[11px] text-slate-400 mt-1">Triggers low stock supplier notifications</p>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Max Stock Level
              </label>
              <input
                type="number"
                min="1"
                value={settingsForm.maxStockLevel}
                onChange={(e) => setSettingsForm({ ...settingsForm, maxStockLevel: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold"
              />
              <p className="text-[11px] text-slate-400 mt-1">Maximum warehouse bin capacity</p>
            </div>
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setSettingsItem(null)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSavingSettings}>
              Update Thresholds
            </Button>
          </div>
        </form>
      </Modal>
    </AnimatedPage>
  )
}

export default Inventory
