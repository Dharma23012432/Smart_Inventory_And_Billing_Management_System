import React, { useState, useEffect } from 'react'
import {
  Eye,
  XCircle,
  CreditCard,
  Mail,
  RefreshCw,
} from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import AnimatedPage from '../components/AnimatedPage'
import { billService } from '../services/billService'
import { paymentService } from '../services/paymentService'
import { formatCurrency, formatDate } from '../utils/formatters'
import { useToast } from '../context/ToastContext'
import { AppEvents, publishEvent, subscribeEvent } from '../utils/eventBus'

const STATUS_TABS = [
  { label: 'All Bills', value: '' },
  { label: 'Paid', value: 'PAID' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Drafts', value: 'DRAFT' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

export function SalesHistory() {
  const { success, error } = useToast()

  // State
  const [bills, setBills] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [selectedBill, setSelectedBill] = useState(null)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [billToPay, setBillToPay] = useState(null)
  const [paymentForm, setPaymentForm] = useState({ method: 'UPI', reference: '' })
  const [isProcessingPayment, setIsProcessingPayment] = useState(false)

  // Cancel Confirm Dialog
  const [billToCancel, setBillToCancel] = useState(null)
  const [isCancelling, setIsCancelling] = useState(false)

  const fetchBills = async () => {
    setIsLoading(true)
    try {
      const res = await billService.getBills({
        status: statusFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size,
        sortBy: 'createdAt',
        direction: 'DESC',
      })
      setBills(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch sales bills')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchBills()
  }, [page, size, statusFilter, startDate, endDate])

  useEffect(() => {
    const unsub1 = subscribeEvent(AppEvents.INVOICE_CREATED, fetchBills)
    const unsub2 = subscribeEvent(AppEvents.INVOICE_CONFIRMED, fetchBills)
    const unsub3 = subscribeEvent(AppEvents.INVOICE_CANCELLED, fetchBills)
    const unsub4 = subscribeEvent(AppEvents.INVOICE_PAID, fetchBills)
    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
    }
  }, [page, size, statusFilter, startDate, endDate])

  // Confirm Bill Action
  const handleConfirmBill = async (billId) => {
    try {
      await billService.confirmBill(billId)
      success('Bill confirmed and inventory stock deducted!')
      publishEvent(AppEvents.INVOICE_CONFIRMED, { billId })
      publishEvent(AppEvents.STOCK_UPDATED)
      fetchBills()
      if (selectedBill?.id === billId) {
        setShowDetailModal(false)
      }
    } catch (err) {
      error(err.message || 'Failed to confirm bill')
    }
  }

  // Cancel Bill Action
  const handleConfirmCancel = async () => {
    if (!billToCancel) return
    setIsCancelling(true)
    try {
      await billService.cancelBill(billToCancel.id)
      success(`Bill ${billToCancel.billNumber} cancelled successfully`)
      publishEvent(AppEvents.INVOICE_CANCELLED, { billId: billToCancel.id })
      publishEvent(AppEvents.STOCK_UPDATED)
      setBillToCancel(null)
      fetchBills()
    } catch (err) {
      error(err.message || 'Failed to cancel bill')
    } finally {
      setIsCancelling(false)
    }
  }

  // Payment Execution
  const handleProcessPayment = async () => {
    if (!billToPay) return
    setIsProcessingPayment(true)
    try {
      await paymentService.processPayment({
        billId: billToPay.id,
        amountPaid: billToPay.totalAmount,
        paymentMethod: paymentForm.method,
        referenceNumber: paymentForm.reference.trim() || `REF-${Date.now().toString().slice(-6)}`,
      })
      success(`Payment recorded! Customer receipt processed.`)
      publishEvent(AppEvents.INVOICE_PAID, { billId: billToPay.id })
      setShowPaymentModal(false)
      setBillToPay(null)
      fetchBills()
    } catch (err) {
      error(err.message || 'Payment processing failed')
    } finally {
      setIsProcessingPayment(false)
    }
  }

  // Open full details
  const handleOpenDetails = async (bill) => {
    try {
      const full = await billService.getBillById(bill.id)
      setSelectedBill(full)
      setShowDetailModal(true)
    } catch {
      setSelectedBill(bill)
      setShowDetailModal(true)
    }
  }

  const columns = [
    {
      header: 'Bill #',
      accessor: 'billNumber',
      render: (row) => (
        <span className="font-bold text-[var(--color-text-primary)] font-mono text-xs sm:text-sm">
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
          <p className="text-[11px] text-[var(--color-text-tertiary)]">
            {row.customer?.email || <span className="italic">No Email Available</span>}
          </p>
        </div>
      ),
    },
    {
      header: 'Date',
      accessor: 'billDate',
      render: (row) => <span className="text-[var(--color-text-secondary)]">{formatDate(row.billDate)}</span>,
    },
    {
      header: 'Total Amount',
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
    {
      header: 'Email Receipt',
      accessor: 'invoiceSent',
      align: 'center',
      render: (row) => {
        if (row.status === 'CANCELLED') {
          return <span className="text-xs text-rose-500 font-medium">Cancelled</span>
        }
        if (!row.customer?.email) {
          return <span className="text-xs text-slate-400 dark:text-slate-500 italic">No Email Available</span>
        }
        if (row.invoiceSent) {
          return (
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800">
              <Mail className="w-3 h-3 text-emerald-600 dark:text-emerald-400" /> Sent
            </span>
          )
        }
        if (row.status === 'PAID') {
          return <span className="text-xs text-amber-600 dark:text-amber-400 font-medium">Pending</span>
        }
        return <span className="text-xs text-slate-400">—</span>
      },
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

          {row.status === 'DRAFT' && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleConfirmBill(row.id)}
              className="text-xs font-semibold"
            >
              Confirm
            </Button>
          )}

          {row.status === 'CONFIRMED' && (
            <Button
              variant="primary"
              size="sm"
              icon={CreditCard}
              onClick={() => {
                setBillToPay(row)
                setShowPaymentModal(true)
              }}
              className="text-xs font-semibold"
            >
              Pay
            </Button>
          )}

          {(row.status === 'DRAFT' || row.status === 'CONFIRMED') && (
            <Button
              variant="ghost"
              size="sm"
              className="text-rose-500 hover:text-rose-400 hover:bg-rose-500/10"
              onClick={() => setBillToCancel(row)}
              title="Cancel Bill"
            >
              <XCircle className="w-4 h-4" />
            </Button>
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
            Sales Invoices History
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Search, filter, view breakdown, and manage customer sales bills.
          </p>
        </div>
        <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchBills}>
          Refresh
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

          {/* Date Range Picker */}
          <div className="flex items-center gap-2 text-xs">
            <span className="text-[var(--color-text-secondary)] font-medium">Date:</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="px-2.5 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-lg text-xs font-semibold text-[var(--color-text-primary)]"
            />
            <span className="text-[var(--color-text-tertiary)]">to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="px-2.5 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-lg text-xs font-semibold text-[var(--color-text-primary)]"
            />
            {(startDate || endDate) && (
              <button
                onClick={() => {
                  setStartDate('')
                  setEndDate('')
                }}
                className="text-xs text-rose-500 hover:underline font-semibold ml-1 cursor-pointer"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        <Table
          columns={columns}
          data={bills}
          isLoading={isLoading}
          emptyMessage="No sales bills match your criteria"
          emptySubtext="Try switching tabs or adjusting date filters."
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

      {/* Bill Details Modal */}
      <Modal
        isOpen={showDetailModal}
        onClose={() => setShowDetailModal(false)}
        size="lg"
        title={`Invoice Breakdown — ${selectedBill?.billNumber}`}
        subtitle={`Created on ${formatDate(selectedBill?.billDate)}`}
      >
        {selectedBill && (
          <div className="space-y-6">
            {/* Top metadata */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs">
              <div>
                <span className="text-slate-400 block font-medium">Customer</span>
                <span className="font-bold text-slate-800 text-sm">
                  {selectedBill.customer?.name || 'Walk-in'}
                </span>
                {selectedBill.customer?.email && (
                  <span className="text-slate-500 block truncate">{selectedBill.customer.email}</span>
                )}
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Status</span>
                <div className="mt-1">
                  <Badge status={selectedBill.status} />
                </div>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">GST / Tax</span>
                <span className="font-bold text-slate-800 text-sm">
                  {formatCurrency(selectedBill.taxAmount)}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Total Amount</span>
                <span className="font-black text-slate-900 text-sm sm:text-base">
                  {formatCurrency(selectedBill.totalAmount)}
                </span>
              </div>
            </div>

            {/* Line items table */}
            <div>
              <h4 className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
                Purchased Line Items
              </h4>
              <div className="border border-slate-200 rounded-xl overflow-hidden">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-200">
                    <tr>
                      <th className="py-2.5 px-3">Product</th>
                      <th className="py-2.5 px-3 text-right">Unit Price</th>
                      <th className="py-2.5 px-3 text-center">Qty</th>
                      <th className="py-2.5 px-3 text-right">Disc %</th>
                      <th className="py-2.5 px-3 text-right">Line Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {selectedBill.items?.map((item) => (
                      <tr key={item.id}>
                        <td className="py-2.5 px-3">
                          <p className="font-bold text-slate-800">{item.productName}</p>
                          <p className="text-[10px] text-slate-400 font-mono">{item.sku}</p>
                        </td>
                        <td className="py-2.5 px-3 text-right font-medium">
                          {formatCurrency(item.unitPrice)}
                        </td>
                        <td className="py-2.5 px-3 text-center font-bold">{item.quantity}</td>
                        <td className="py-2.5 px-3 text-right">{item.discountPercent || 0}%</td>
                        <td className="py-2.5 px-3 text-right font-bold text-slate-900">
                          {formatCurrency(item.totalPrice)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Financial summary */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200/80 space-y-1.5 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Subtotal:</span>
                <span>{formatCurrency(selectedBill.subtotal)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Discount Applied:</span>
                <span>-{formatCurrency(selectedBill.discountAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Tax:</span>
                <span>+{formatCurrency(selectedBill.taxAmount)}</span>
              </div>
              <div className="flex justify-between font-extrabold text-sm text-slate-900 pt-2 border-t border-slate-200">
                <span>Grand Total:</span>
                <span>{formatCurrency(selectedBill.totalAmount)}</span>
              </div>
            </div>

            {/* Modal actions */}
            <div className="flex justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setShowDetailModal(false)}>
                Close
              </Button>
              {selectedBill.status === 'DRAFT' && (
                <Button variant="primary" onClick={() => handleConfirmBill(selectedBill.id)}>
                  Confirm Bill
                </Button>
              )}
              {selectedBill.status === 'CONFIRMED' && (
                <Button
                  variant="primary"
                  icon={CreditCard}
                  onClick={() => {
                    setBillToPay(selectedBill)
                    setShowDetailModal(false)
                    setShowPaymentModal(true)
                  }}
                >
                  Pay Now
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Collect Payment Modal */}
      <Modal
        isOpen={showPaymentModal}
        onClose={() => setShowPaymentModal(false)}
        title={`Process Payment for ${billToPay?.billNumber}`}
        subtitle={`Grand Total: ${formatCurrency(billToPay?.totalAmount || 0)}`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Select Payment Method
            </label>
            <div className="grid grid-cols-3 gap-2">
              {['UPI', 'CASH', 'CARD', 'BANK_TRANSFER', 'CHEQUE'].map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setPaymentForm({ ...paymentForm, method: m })}
                  className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all ${
                    paymentForm.method === m
                      ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                      : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Transaction / Reference Note
            </label>
            <input
              type="text"
              value={paymentForm.reference}
              onChange={(e) => setPaymentForm({ ...paymentForm, reference: e.target.value })}
              placeholder="e.g. UPI-TXN-123456"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="p-3 bg-emerald-50 border border-emerald-100 rounded-xl text-xs text-emerald-900">
            Confirming this payment will mark the invoice as PAID and automatically send an HTML/PDF invoice
            to the customer email address.
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowPaymentModal(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              isLoading={isProcessingPayment}
              onClick={handleProcessPayment}
            >
              Confirm & Deliver Invoice
            </Button>
          </div>
        </div>
      </Modal>

      {/* Cancel Bill Confirmation */}
      <ConfirmDialog
        isOpen={Boolean(billToCancel)}
        onClose={() => setBillToCancel(null)}
        onConfirm={handleConfirmCancel}
        isLoading={isCancelling}
        title={`Cancel Bill ${billToCancel?.billNumber}?`}
        message="Cancelling this bill will release reserved quantities and restore stock back to warehouse inventory. Proceed?"
        confirmText="Yes, Cancel Bill"
      />
    </AnimatedPage>
  )
}

export default SalesHistory
