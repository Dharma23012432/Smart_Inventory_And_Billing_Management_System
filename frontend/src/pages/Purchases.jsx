import React, { useState, useEffect } from 'react'
import {
  Plus,
  Eye,
  XCircle,
  Trash2,
  RefreshCw,
  PackageCheck,
} from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import AnimatedPage from '../components/AnimatedPage'
import { purchaseService } from '../services/purchaseService'
import { supplierService } from '../services/supplierService'
import { productService } from '../services/productService'
import { formatCurrency, formatDate } from '../utils/formatters'
import { useToast } from '../context/ToastContext'

const STATUS_TABS = [
  { label: 'All Orders', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Received', value: 'RECEIVED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

export function Purchases() {
  const { success, error } = useToast()

  // State
  const [purchases, setPurchases] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [products, setProducts] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [selectedSupplierId, setSelectedSupplierId] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  // Details Modal
  const [selectedPO, setSelectedPO] = useState(null)
  const [showDetailModal, setShowDetailModal] = useState(false)

  // Create PO Modal
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createSupplierId, setCreateSupplierId] = useState('')
  const [poNotes, setPoNotes] = useState('')
  const [poItems, setPoItems] = useState([])
  const [isSubmittingPO, setIsSubmittingPO] = useState(false)

  // Confirm Actions
  const [poToReceive, setPoToReceive] = useState(null)
  const [isReceiving, setIsReceiving] = useState(false)
  const [poToCancel, setPoToCancel] = useState(null)
  const [isCancelling, setIsCancelling] = useState(false)

  // Load suppliers and products lookups
  useEffect(() => {
    const loadLookups = async () => {
      try {
        const [supRes, prodRes] = await Promise.all([
          supplierService.getAllActiveSuppliers().catch(() => []),
          productService.getProducts({ size: 100, status: 'ACTIVE' }).catch(() => ({ content: [] })),
        ])
        setSuppliers(supRes || [])
        setProducts(prodRes?.content || [])
      } catch {
        // Silently catch
      }
    }
    loadLookups()
  }, [])

  // Load Purchases
  const fetchPurchases = async () => {
    setIsLoading(true)
    try {
      const res = await purchaseService.getPurchases({
        supplierId: selectedSupplierId || undefined,
        status: statusFilter || undefined,
        page,
        size,
        sortBy: 'createdAt',
        direction: 'DESC',
      })
      setPurchases(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch purchase orders')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchPurchases()
  }, [page, size, statusFilter, selectedSupplierId])

  // Open Create PO Modal
  const handleOpenCreate = () => {
    setCreateSupplierId(suppliers.length > 0 ? suppliers[0].id : '')
    setPoNotes('')
    setPoItems(
      products.length > 0
        ? [
            {
              productId: products[0].id,
              quantity: 10,
              unitCost: products[0].purchasePrice || 100,
            },
          ]
        : []
    )
    setShowCreateModal(true)
  }

  // Add Item to PO
  const handleAddLineItem = () => {
    if (products.length === 0) return
    setPoItems((prev) => [
      ...prev,
      {
        productId: products[0].id,
        quantity: 10,
        unitCost: products[0].purchasePrice || 100,
      },
    ])
  }

  // Remove Item from PO
  const handleRemoveLineItem = (index) => {
    setPoItems((prev) => prev.filter((_, i) => i !== index))
  }

  // Update Item in PO
  const handleUpdateLineItem = (index, field, value) => {
    setPoItems((prev) =>
      prev.map((item, i) => {
        if (i === index) {
          if (field === 'productId') {
            const prod = products.find((p) => p.id === Number(value))
            return {
              ...item,
              productId: Number(value),
              unitCost: prod?.purchasePrice || item.unitCost,
            }
          }
          return { ...item, [field]: value }
        }
        return item
      })
    )
  }

  // Submit New PO
  const handleCreatePOSubmit = async (e) => {
    e.preventDefault()
    if (!createSupplierId) {
      error('Please select a supplier')
      return
    }
    if (poItems.length === 0) {
      error('Please add at least one line item to the order')
      return
    }

    setIsSubmittingPO(true)
    try {
      const payload = {
        supplierId: Number(createSupplierId),
        notes: poNotes.trim() || null,
        items: poItems.map((item) => ({
          productId: Number(item.productId),
          quantity: Number(item.quantity),
          unitCost: Number(item.unitCost),
        })),
      }

      const created = await purchaseService.createPurchase(payload)
      success(`Purchase Order '${created.purchaseNumber}' created!`)
      setShowCreateModal(false)
      fetchPurchases()
    } catch (err) {
      error(err.message || 'Failed to create purchase order')
    } finally {
      setIsSubmittingPO(false)
    }
  }

  // Receive PO
  const handleConfirmReceive = async () => {
    if (!poToReceive) return
    setIsReceiving(true)
    try {
      await purchaseService.receivePurchase(poToReceive.id)
      success(`Order ${poToReceive.purchaseNumber} marked RECEIVED. Warehouse inventory updated!`)
      setPoToReceive(null)
      fetchPurchases()
      if (selectedPO?.id === poToReceive.id) {
        setShowDetailModal(false)
      }
    } catch (err) {
      error(err.message || 'Failed to receive purchase order')
    } finally {
      setIsReceiving(false)
    }
  }

  // Cancel PO
  const handleConfirmCancel = async () => {
    if (!poToCancel) return
    setIsCancelling(true)
    try {
      await purchaseService.cancelPurchase(poToCancel.id)
      success(`Order ${poToCancel.purchaseNumber} cancelled successfully`)
      setPoToCancel(null)
      fetchPurchases()
      if (selectedPO?.id === poToCancel.id) {
        setShowDetailModal(false)
      }
    } catch (err) {
      error(err.message || 'Failed to cancel purchase order')
    } finally {
      setIsCancelling(false)
    }
  }

  // Open Full Details
  const handleOpenDetails = async (po) => {
    try {
      const full = await purchaseService.getPurchaseById(po.id)
      setSelectedPO(full)
      setShowDetailModal(true)
    } catch {
      setSelectedPO(po)
      setShowDetailModal(true)
    }
  }

  const columns = [
    {
      header: 'PO Number',
      accessor: 'purchaseNumber',
      render: (row) => (
        <span className="font-bold text-[var(--color-text-primary)] font-mono text-xs sm:text-sm">
          {row.purchaseNumber || (row.id ? `PO-${String(row.id).padStart(5, '0')}` : '—')}
        </span>
      ),
    },
    {
      header: 'Supplier',
      accessor: (row) => row.supplier?.name || '—',
      render: (row) => (
        <div>
          <p className="font-bold text-[var(--color-text-primary)] text-xs sm:text-sm">
            {row.supplier?.name || '—'}
          </p>
          <p className="text-[11px] text-[var(--color-text-tertiary)]">{row.supplier?.email || row.supplierEmail || 'No email'}</p>
        </div>
      ),
    },
    {
      header: 'Order Date',
      accessor: 'purchaseDate',
      render: (row) => <span className="text-[var(--color-text-secondary)]">{formatDate(row.purchaseDate)}</span>,
    },
    {
      header: 'Total Cost',
      accessor: 'totalAmount',
      align: 'right',
      render: (row) => (
        <span className="font-black text-[var(--color-text-primary)]">
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
    {
      header: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="sm"
            icon={Eye}
            onClick={() => handleOpenDetails(row)}
            title="View Details"
          />

          {row.status === 'PENDING' && (
            <>
              <Button
                variant="primary"
                size="sm"
                icon={PackageCheck}
                onClick={() => setPoToReceive(row)}
                className="text-xs font-semibold"
              >
                Receive
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-rose-600 hover:text-rose-800 hover:bg-rose-50"
                onClick={() => setPoToCancel(row)}
                title="Cancel Order"
              >
                <XCircle className="w-4 h-4" />
              </Button>
            </>
          )}
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
            Purchase Orders
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Procure stock from suppliers, receive warehouse shipments, and replenish inventory.
          </p>
        </div>

        <Button variant="primary" icon={Plus} onClick={handleOpenCreate}>
          New Purchase Order
        </Button>
      </div>

      {/* Tabs and Filters */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-4">
          {/* Status Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
            {STATUS_TABS.map((tab) => (
              <button
                key={tab.value}
                onClick={() => {
                  setStatusFilter(tab.value)
                  setPage(0)
                }}
                className={`px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all whitespace-nowrap cursor-pointer ${
                  statusFilter === tab.value
                    ? 'bg-[var(--color-accent)] text-white shadow-xs'
                    : 'bg-[var(--color-surface-tertiary)] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-surface-hover)]'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            {/* Supplier Filter */}
            <select
              value={selectedSupplierId}
              onChange={(e) => {
                setSelectedSupplierId(e.target.value)
                setPage(0)
              }}
              className="px-3 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] font-medium cursor-pointer"
            >
              <option value="" className="bg-[var(--color-card-bg)]">All Suppliers</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id} className="bg-[var(--color-card-bg)]">
                  {s.name}
                </option>
              ))}
            </select>

            <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchPurchases}>
              Refresh
            </Button>
          </div>
        </div>

        {/* Table */}
        <Table
          columns={columns}
          data={purchases}
          isLoading={isLoading}
          emptyMessage="No purchase orders found"
          emptySubtext="Create a new purchase order to start replenishment."
          onRowClick={handleOpenDetails}
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

      {/* PO Details Modal */}
      <Modal
        isOpen={showDetailModal}
        onClose={() => setShowDetailModal(false)}
        size="lg"
        title={`Purchase Order Details — ${selectedPO?.purchaseNumber}`}
        subtitle={`Issued on ${formatDate(selectedPO?.purchaseDate)}`}
      >
        {selectedPO && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs">
              <div>
                <span className="text-slate-400 block font-medium">Supplier Vendor</span>
                <span className="font-bold text-slate-800 text-sm">
                  {selectedPO.supplier?.name}
                </span>
                <span className="text-slate-500 block truncate">{selectedPO.supplier?.email || selectedPO.supplierEmail || 'No email'}</span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Order Status</span>
                <div className="mt-1">
                  <Badge status={selectedPO.status} />
                </div>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Item Count</span>
                <span className="font-bold text-slate-800 text-sm">
                  {selectedPO.items?.length || 0} line items
                </span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Total Cost</span>
                <span className="font-black text-slate-900 text-sm sm:text-base">
                  {formatCurrency(selectedPO.totalAmount)}
                </span>
              </div>
            </div>

            {/* Line Items */}
            <div>
              <h4 className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
                Ordered Products
              </h4>
              <div className="border border-slate-200 rounded-xl overflow-hidden">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-200">
                    <tr>
                      <th className="py-2.5 px-3">Product Name</th>
                      <th className="py-2.5 px-3 text-right">Unit Cost</th>
                      <th className="py-2.5 px-3 text-center">Quantity</th>
                      <th className="py-2.5 px-3 text-right">Total Price</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {selectedPO.items?.map((item) => (
                      <tr key={item.id}>
                        <td className="py-2.5 px-3">
                          <p className="font-bold text-slate-800">{item.productName}</p>
                          <p className="text-[10px] text-slate-400 font-mono">{item.sku}</p>
                        </td>
                        <td className="py-2.5 px-3 text-right font-medium">
                          {formatCurrency(item.unitPrice || item.unitCost)}
                        </td>
                        <td className="py-2.5 px-3 text-center font-bold">{item.quantity}</td>
                        <td className="py-2.5 px-3 text-right font-bold text-slate-900">
                          {formatCurrency(item.totalPrice)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {selectedPO.notes && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600">
                <span className="font-bold block mb-0.5">Order Remarks:</span>
                {selectedPO.notes}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setShowDetailModal(false)}>
                Close
              </Button>
              {selectedPO.status === 'PENDING' && (
                <Button
                  variant="primary"
                  icon={PackageCheck}
                  onClick={() => {
                    setPoToReceive(selectedPO)
                    setShowDetailModal(false)
                  }}
                >
                  Receive Shipment
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Create Purchase Order Modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        size="lg"
        title="Create Procurement Purchase Order"
        subtitle="Order warehouse supplies from registered vendors"
      >
        <form onSubmit={handleCreatePOSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Supplier Vendor *
            </label>
            <select
              required
              value={createSupplierId}
              onChange={(e) => setCreateSupplierId(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            >
              <option value="">Select Supplier</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.email || 'No email'})
                </option>
              ))}
            </select>
          </div>

          {/* Line items list */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-xs font-bold text-slate-600 uppercase">
                Order Items ({poItems.length})
              </label>
              <button
                type="button"
                onClick={handleAddLineItem}
                className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add Product
              </button>
            </div>

            <div className="space-y-2 max-h-[220px] overflow-y-auto p-1">
              {poItems.map((item, idx) => (
                <div
                  key={idx}
                  className="p-3 bg-slate-50 border border-slate-200 rounded-xl grid grid-cols-12 gap-3 items-center text-xs"
                >
                  <div className="col-span-6">
                    <select
                      value={item.productId}
                      onChange={(e) => handleUpdateLineItem(idx, 'productId', e.target.value)}
                      className="w-full px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-semibold"
                    >
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} ({p.sku})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="col-span-2">
                    <input
                      type="number"
                      min="1"
                      required
                      value={item.quantity}
                      onChange={(e) => handleUpdateLineItem(idx, 'quantity', e.target.value)}
                      placeholder="Qty"
                      className="w-full px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-bold text-center"
                    />
                  </div>

                  <div className="col-span-3">
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      required
                      value={item.unitCost}
                      onChange={(e) => handleUpdateLineItem(idx, 'unitCost', e.target.value)}
                      placeholder="Cost"
                      className="w-full px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs font-semibold text-right"
                    />
                  </div>

                  <div className="col-span-1 text-center">
                    <button
                      type="button"
                      onClick={() => handleRemoveLineItem(idx)}
                      className="text-slate-400 hover:text-rose-600"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Procurement Notes
            </label>
            <input
              type="text"
              value={poNotes}
              onChange={(e) => setPoNotes(e.target.value)}
              placeholder="e.g. Expedited warehouse shipment requested..."
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
            />
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowCreateModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmittingPO}>
              Create Purchase Order
            </Button>
          </div>
        </form>
      </Modal>

      {/* Receive PO Dialog */}
      <ConfirmDialog
        isOpen={Boolean(poToReceive)}
        onClose={() => setPoToReceive(null)}
        onConfirm={handleConfirmReceive}
        isLoading={isReceiving}
        title={`Receive Shipment for ${poToReceive?.purchaseNumber}?`}
        message="Confirming this delivery will mark the order as RECEIVED and automatically ingest product quantities into warehouse inventory."
        confirmText="Yes, Receive & Ingest Stock"
        confirmVariant="success"
      />

      {/* Cancel PO Dialog */}
      <ConfirmDialog
        isOpen={Boolean(poToCancel)}
        onClose={() => setPoToCancel(null)}
        onConfirm={handleConfirmCancel}
        isLoading={isCancelling}
        title={`Cancel ${poToCancel?.purchaseNumber}?`}
        message="Are you sure you want to cancel this pending purchase order? This action cannot be undone."
        confirmText="Yes, Cancel Order"
      />
    </AnimatedPage>
  )
}

export default Purchases
