import React, { useState, useEffect } from 'react'
import {
  Plus,
  Search,
  Edit,
  Trash2,
  RefreshCw,
  Power,
  Mail,
  Phone,
} from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Badge from '../components/Badge'
import Button from '../components/Button'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import AnimatedPage from '../components/AnimatedPage'
import SearchInput from '../components/SearchInput'
import { customerService } from '../services/customerService'
import { useToast } from '../context/ToastContext'
import { AppEvents, publishEvent } from '../utils/eventBus'

const INITIAL_FORM = {
  name: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  state: '',
  pincode: '',
  gstin: '',
}

export function Customers() {
  const { success, error } = useToast()

  const [customers, setCustomers] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ACTIVE')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [showModal, setShowModal] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState(null)
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [customerToDelete, setCustomerToDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchCustomers = async () => {
    setIsLoading(true)
    try {
      const res = await customerService.getCustomers({
        search: search.trim() || undefined,
        status: statusFilter,
        page,
        size,
        sortBy: 'name',
        direction: 'ASC',
      })
      setCustomers(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch customers')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchCustomers()
  }, [page, size, search, statusFilter])

  const handleOpenCreate = () => {
    setEditingCustomer(null)
    setFormData(INITIAL_FORM)
    setShowModal(true)
  }

  const handleOpenEdit = (c) => {
    setEditingCustomer(c)
    setFormData({
      name: c.name || '',
      email: c.email || '',
      phone: c.phone || '',
      address: c.address || '',
      city: c.city || '',
      state: c.state || '',
      pincode: c.pincode || '',
      gstin: c.gstin || '',
    })
    setShowModal(true)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!formData.name.trim()) {
      error('Customer name is required')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim(),
        email: formData.email?.trim() || null,
        phone: formData.phone?.trim() || null,
        address: formData.address?.trim() || null,
        city: formData.city?.trim() || null,
        state: formData.state?.trim() || null,
        pincode: formData.pincode?.trim() || null,
        gstin: formData.gstin?.trim() || null,
      }

      if (editingCustomer) {
        await customerService.updateCustomer(editingCustomer.id, payload)
        success(`Customer '${payload.name}' updated successfully`)
        publishEvent(AppEvents.CUSTOMER_UPDATED, { id: editingCustomer.id, ...payload })
      } else {
        await customerService.createCustomer(payload)
        success(`Customer '${payload.name}' registered successfully`)
        publishEvent(AppEvents.CUSTOMER_UPDATED, payload)
      }

      setShowModal(false)
      fetchCustomers()
    } catch (err) {
      error(err.message || 'Failed to save customer')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleToggleStatus = async (customer) => {
    const newStatus = customer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await customerService.updateCustomerStatus(customer.id, newStatus)
      success(`Customer status set to ${newStatus}`)
      fetchCustomers()
    } catch (err) {
      error(err.message || 'Failed to update status')
    }
  }

  const handleConfirmDelete = async () => {
    if (!customerToDelete) return
    setIsDeleting(true)
    try {
      await customerService.deleteCustomer(customerToDelete.id)
      success(`Customer '${customerToDelete.name}' deleted successfully`)
      setCustomerToDelete(null)
      fetchCustomers()
    } catch (err) {
      error(err.message || 'Cannot delete customer with existing bill history')
    } finally {
      setIsDeleting(false)
    }
  }

  const columns = [
    {
      header: 'Customer',
      accessor: 'name',
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-purple-600/20 border border-purple-500/30 text-purple-400 flex items-center justify-center font-bold text-xs shrink-0">
            {row.name.substring(0, 2).toUpperCase()}
          </div>
          <div>
            <p className="font-bold text-[var(--color-text-primary)] text-sm">{row.name}</p>
            {row.gstin && (
              <p className="text-[11px] text-[var(--color-text-tertiary)] font-mono">GSTIN: {row.gstin}</p>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Email Address',
      accessor: 'email',
      render: (row) =>
        row.email ? (
          <span className="text-xs text-[var(--color-text-secondary)] flex items-center gap-1.5">
            <Mail className="w-3.5 h-3.5 text-purple-400 shrink-0" /> {row.email}
          </span>
        ) : (
          <span className="text-xs text-[var(--color-text-tertiary)] italic">No Email Available</span>
        ),
    },
    {
      header: 'Phone',
      accessor: 'phone',
      render: (row) =>
        row.phone ? (
          <span className="text-xs text-slate-700 flex items-center gap-1.5 font-medium">
            <Phone className="w-3.5 h-3.5 text-slate-400 shrink-0" /> {row.phone}
          </span>
        ) : (
          <span className="text-xs text-slate-400">—</span>
        ),
    },
    {
      header: 'Location',
      accessor: 'city',
      render: (row) => (
        <span className="text-xs text-slate-600">
          {[row.city, row.state].filter(Boolean).join(', ') || '—'}
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
        <div className="flex items-center justify-end gap-1">
          <Button
            variant="ghost"
            size="sm"
            icon={Edit}
            onClick={() => handleOpenEdit(row)}
            title="Edit Customer"
          />
          <Button
            variant="ghost"
            size="sm"
            icon={Power}
            onClick={() => handleToggleStatus(row)}
            className={row.status === 'ACTIVE' ? 'text-emerald-600' : 'text-slate-400'}
            title={row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          />
          <Button
            variant="ghost"
            size="sm"
            icon={Trash2}
            onClick={() => setCustomerToDelete(row)}
            className="text-rose-600 hover:text-rose-800"
            title="Delete Customer"
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
            Customer Directory
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Manage customer accounts, contact details, and billing profiles.
          </p>
        </div>

        <Button variant="primary" icon={Plus} onClick={handleOpenCreate}>
          Register Customer
        </Button>
      </div>

      {/* Main Card */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3 flex-1">
            <SearchInput
              value={search}
              onChange={(val) => {
                setSearch(val)
                setPage(0)
              }}
              placeholder="Search name, email, phone..."
              className="w-full sm:w-72"
            />

            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setPage(0)
              }}
              className="px-3 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] font-medium cursor-pointer"
            >
              <option value="ACTIVE" className="bg-[var(--color-card-bg)]">Active Customers</option>
              <option value="INACTIVE" className="bg-[var(--color-card-bg)]">Inactive</option>
              <option value="ALL" className="bg-[var(--color-card-bg)]">All Accounts</option>
            </select>
          </div>

          <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchCustomers}>
            Refresh
          </Button>
        </div>

        <Table
          columns={columns}
          data={customers}
          isLoading={isLoading}
          emptyMessage="No customers found"
          emptySubtext="Register your first customer to begin billing."
        />

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

      {/* Create / Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        size="lg"
        title={editingCustomer ? `Edit Customer — ${editingCustomer.name}` : 'Register New Customer'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Full Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. Priya Sharma"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Email Address (for invoices)
              </label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="e.g. priya@example.com"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Phone Number
              </label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="e.g. 9876543210"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                GSTIN / Tax ID
              </label>
              <input
                type="text"
                value={formData.gstin}
                onChange={(e) => setFormData({ ...formData, gstin: e.target.value })}
                placeholder="e.g. 29ABCDE1234F1Z5"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:ring-1 focus:ring-slate-400"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Address</label>
            <input
              type="text"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              placeholder="Street address..."
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">City</label>
              <input
                type="text"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                placeholder="City"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">State</label>
              <input
                type="text"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                placeholder="State"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Pincode</label>
              <input
                type="text"
                value={formData.pincode}
                onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                placeholder="Pincode"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
              />
            </div>
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingCustomer ? 'Save Changes' : 'Register Customer'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={Boolean(customerToDelete)}
        onClose={() => setCustomerToDelete(null)}
        onConfirm={handleConfirmDelete}
        isLoading={isDeleting}
        title={`Delete '${customerToDelete?.name}'?`}
        message="Are you sure you want to delete this customer? This action cannot be completed if they have past sales invoices."
      />
    </AnimatedPage>
  )
}

export default Customers
