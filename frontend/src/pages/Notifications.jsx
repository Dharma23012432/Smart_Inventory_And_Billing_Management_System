import React, { useState, useEffect } from 'react'
import {
  Mail,
  RefreshCw,
  Eye,
  Send,
  RotateCcw,
} from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import AnimatedPage from '../components/AnimatedPage'
import { notificationService } from '../services/notificationService'
import { formatDateTime } from '../utils/formatters'
import { useToast } from '../context/ToastContext'
import { AppEvents, subscribeEvent } from '../utils/eventBus'

const STATUS_FILTERS = [
  { label: 'All Audits', value: '' },
  { label: 'Pending Approval', value: 'PENDING' },
  { label: 'Sent Successfully', value: 'SENT' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Skipped (No Email)', value: 'SKIPPED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

export function Notifications() {
  const { success, error } = useToast()

  const [notifications, setNotifications] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [viewingNotification, setViewingNotification] = useState(null)
  const [approvingItem, setApprovingItem] = useState(null)
  const [isApproving, setIsApproving] = useState(false)
  const [isRetrying, setIsRetrying] = useState(false)

  const fetchNotifications = async () => {
    setIsLoading(true)
    try {
      const res = await notificationService.getNotifications({
        status: statusFilter || undefined,
        type: typeFilter || undefined,
        page,
        size,
        sort: 'createdAt,desc',
      })
      setNotifications(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch notifications')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchNotifications()
  }, [page, size, statusFilter, typeFilter])

  useEffect(() => {
    const unsub1 = subscribeEvent(AppEvents.INVOICE_CREATED, fetchNotifications)
    const unsub2 = subscribeEvent(AppEvents.INVOICE_CONFIRMED, fetchNotifications)
    const unsub3 = subscribeEvent(AppEvents.INVOICE_CANCELLED, fetchNotifications)
    const unsub4 = subscribeEvent(AppEvents.INVOICE_PAID, fetchNotifications)
    const unsub5 = subscribeEvent(AppEvents.EMAIL_SENT, fetchNotifications)
    const unsub6 = subscribeEvent(AppEvents.EMAIL_FAILED, fetchNotifications)
    const unsub7 = subscribeEvent(AppEvents.EMAIL_SKIPPED, fetchNotifications)
    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
      unsub5()
      unsub6()
      unsub7()
    }
  }, [page, size, statusFilter, typeFilter])

  // Approve Low Stock Notification (Dispatches email to Supplier)
  const handleApprove = async () => {
    if (!approvingItem) return
    setIsApproving(true)
    try {
      await notificationService.approveNotification(approvingItem.id)
      success(`Notification #${approvingItem.id} approved! Supplier requisition email dispatched.`)
      setApprovingItem(null)
      fetchNotifications()
      if (viewingNotification?.id === approvingItem.id) {
        setViewingNotification(null)
      }
    } catch (err) {
      error(err.message || 'Failed to approve notification')
    } finally {
      setIsApproving(false)
    }
  }

  // Retry failed notification
  const handleRetry = async (item) => {
    setIsRetrying(true)
    try {
      await notificationService.retryNotification(item.id)
      success(`Notification #${item.id} retry attempted`)
      fetchNotifications()
    } catch (err) {
      error(err.message || 'Retry failed')
    } finally {
      setIsRetrying(false)
    }
  }

  const columns = [
    {
      header: 'ID',
      accessor: 'id',
      className: 'w-[6%] min-w-[55px]',
      render: (row) => <span className="font-mono text-xs text-slate-500 font-bold">#{row.id}</span>,
    },
    {
      header: 'Event / Type',
      accessor: 'type',
      className: 'w-[16%] min-w-[130px]',
      render: (row) => (
        <span
          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold ${
            row.type === 'INVOICE_EMAIL'
              ? 'bg-blue-50 text-blue-700 border border-blue-200'
              : 'bg-amber-50 text-amber-800 border border-amber-200'
          }`}
        >
          <Mail className="w-3.5 h-3.5 shrink-0" />
          <span className="truncate">{row.type === 'INVOICE_EMAIL' ? 'Customer Invoice' : 'Low Stock Reorder'}</span>
        </span>
      ),
    },
    {
      header: 'Recipient Email',
      accessor: 'recipientEmail',
      className: 'w-[23%] min-w-[160px]',
      render: (row) => (
        <span
          className="font-semibold text-slate-800 dark:text-slate-200 text-xs sm:text-sm truncate block"
          title={row.recipientEmail || 'No Email Available'}
        >
          {row.recipientEmail || <span className="text-slate-400 dark:text-slate-500 italic font-normal">No Email Available</span>}
        </span>
      ),
    },
    {
      header: 'Email Subject',
      accessor: 'subject',
      className: 'w-[25%] min-w-[170px]',
      render: (row) => (
        <span className="text-xs text-slate-600 truncate block" title={row.subject}>{row.subject}</span>
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      align: 'center',
      className: 'w-[10%] min-w-[80px]',
      render: (row) => <Badge status={row.status} />,
    },
    {
      header: 'Date Logged',
      accessor: 'createdAt',
      className: 'w-[12%] min-w-[110px]',
      render: (row) => (
        <span className="text-xs text-slate-500 whitespace-nowrap">{formatDateTime(row.sentAt || row.createdAt)}</span>
      ),
    },
    {
      header: 'Actions',
      align: 'right',
      className: 'w-[8%] min-w-[75px]',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="sm"
            icon={Eye}
            onClick={() => setViewingNotification(row)}
            title="Preview Email"
          />

          {row.type === 'LOW_STOCK_ALERT' && row.status === 'PENDING' && (
            <Button
              variant="primary"
              size="sm"
              icon={Send}
              onClick={() => setApprovingItem(row)}
              className="text-xs font-semibold whitespace-nowrap"
            >
              Approve & Send
            </Button>
          )}

          {row.status === 'FAILED' && (
            <Button
              variant="outline"
              size="sm"
              icon={RotateCcw}
              isLoading={isRetrying}
              onClick={() => handleRetry(row)}
              className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50"
            >
              Retry
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
            Notifications & Email Audit
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Real-time audit log of customer invoice emails and owner approval for low stock alerts.
          </p>
        </div>

        <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchNotifications}>
          Refresh Log
        </Button>
      </div>

      {/* Main Table Card */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-3">
          {/* Status Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0 max-w-full scrollbar-none">
            {STATUS_FILTERS.map((tab) => (
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

          {/* Type Filter */}
          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value)
              setPage(0)
            }}
            className="px-3 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] font-semibold cursor-pointer"
          >
            <option value="" className="bg-[var(--color-card-bg)]">All Types</option>
            <option value="INVOICE_EMAIL" className="bg-[var(--color-card-bg)]">Customer Invoices</option>
            <option value="LOW_STOCK_ALERT" className="bg-[var(--color-card-bg)]">Low Stock Alerts</option>
          </select>
        </div>

        {/* Table */}
        <Table
          columns={columns}
          data={notifications}
          isLoading={isLoading}
          emptyMessage="No notification logs found"
          emptySubtext="When bills are paid or inventory drops low, dispatches appear here."
          onRowClick={(row) => setViewingNotification(row)}
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

      {/* HTML Email Preview Modal */}
      <Modal
        isOpen={Boolean(viewingNotification)}
        onClose={() => setViewingNotification(null)}
        size="lg"
        title={viewingNotification?.subject || 'Email Preview'}
        subtitle={`To: ${viewingNotification?.recipientEmail} • Status: ${viewingNotification?.status}`}
      >
        {viewingNotification && (
          <div className="space-y-4">
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between text-xs">
              <div>
                <span className="text-slate-400 block font-medium">Recipient Address:</span>
                <span className="font-bold text-slate-800">{viewingNotification.recipientEmail}</span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Logged At:</span>
                <span className="text-slate-600 font-medium">
                  {formatDateTime(viewingNotification.createdAt)}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Current Status:</span>
                <div className="mt-0.5">
                  <Badge status={viewingNotification.status} />
                </div>
              </div>
            </div>

            {/* Rendered HTML or Text Body */}
            <div className="border border-slate-200 rounded-xl overflow-hidden bg-white max-h-[420px] overflow-y-auto p-4 text-xs">
              {viewingNotification.body?.includes('<html') ||
              viewingNotification.body?.includes('<!DOCTYPE') ? (
                <div
                  dangerouslySetInnerHTML={{ __html: viewingNotification.body }}
                  className="prose prose-xs max-w-none"
                />
              ) : (
                <pre className="whitespace-pre-wrap font-sans text-slate-800 leading-relaxed">
                  {viewingNotification.body}
                </pre>
              )}
            </div>

            {/* Modal actions */}
            <div className="flex justify-between items-center pt-2">
              <Button variant="outline" onClick={() => setViewingNotification(null)}>
                Close Preview
              </Button>

              {viewingNotification.type === 'LOW_STOCK_ALERT' &&
                viewingNotification.status === 'PENDING' && (
                  <Button
                    variant="primary"
                    icon={Send}
                    onClick={() => {
                      setApprovingItem(viewingNotification)
                    }}
                  >
                    Approve & Dispatch Requisition
                  </Button>
                )}
            </div>
          </div>
        )}
      </Modal>

      {/* Approve Requisition Dialog */}
      <ConfirmDialog
        isOpen={Boolean(approvingItem)}
        onClose={() => setApprovingItem(null)}
        onConfirm={handleApprove}
        isLoading={isApproving}
        title="Approve Low Stock Requisition?"
        message={`This will immediately dispatch a formal purchase requisition email to ${approvingItem?.recipientEmail} via Gmail SMTP. Proceed?`}
        confirmText="Approve & Send Email"
        confirmVariant="success"
      />
    </AnimatedPage>
  )
}

export default Notifications
