import React, { useState, useEffect } from 'react'
import {
  Building2,
  Plus,
  Search,
  Edit,
  Trash2,
  RefreshCw,
  Power,
  Mail,
  Phone,
  UserCheck,
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
import { supplierService } from '../services/supplierService'
import { useToast } from '../context/ToastContext'
import { AppEvents, publishEvent } from '../utils/eventBus'

const INITIAL_FORM = {
  name: '',
  contactPerson: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  state: '',
  pincode: '',
  gstin: '',
}

export function Suppliers() {
  const { success, error } = useToast()

  const [suppliers, setSuppliers] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ACTIVE')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [showModal, setShowModal] = useState(false)
  const [editingSupplier, setEditingSupplier] = useState(null)
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [supplierToDelete, setSupplierToDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchSuppliers = async () => {
    setIsLoading(true)
    try {
      const res = await supplierService.getSuppliers({
        search: search.trim() || undefined,
        status: statusFilter,
        page,
        size,
        sortBy: 'name',
        direction: 'ASC',
      })
      setSuppliers(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch suppliers')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchSuppliers()
  }, [page, size, search, statusFilter])

  const handleOpenCreate = () => {
    setEditingSupplier(null)
    setFormData(INITIAL_FORM)
    setShowModal(true)
  }

  const handleOpenEdit = (s) => {
    setEditingSupplier(s)
    setFormData({
      name: s.name || '',
      contactPerson: s.contactPerson || '',
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      city: s.city || '',
      state: s.state || '',
      pincode: s.pincode || '',
      gstin: s.gstin || '',
    })
    setShowModal(true)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!formData.name.trim() || !formData.email.trim()) {
      error('Supplier name and email are required')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim(),
        contactPerson: formData.contactPerson?.trim() || null,
        email: formData.email.trim(),
        phone: formData.phone?.trim() || null,
        address: formData.address?.trim() || null,
        city: formData.city?.trim() || null,
        state: formData.state?.trim() || null,
        pincode: formData.pincode?.trim() || null,
        gstin: formData.gstin?.trim() || null,
      }

      if (editingSupplier) {
        await supplierService.updateSupplier(editingSupplier.id, payload)
        success(`Supplier '${payload.name}' updated successfully`)
        publishEvent(AppEvents.SUPPLIER_UPDATED, { id: editingSupplier.id, ...payload })
      } else {
        await supplierService.createSupplier(payload)
        success(`Supplier '${payload.name}' registered successfully`)
        publishEvent(AppEvents.SUPPLIER_UPDATED, payload)
      }

      setShowModal(false)
      fetchSuppliers()
    } catch (err) {
      error(err.message || 'Failed to save supplier')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleToggleStatus = async (supplier) => {
    const newStatus = supplier.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await supplierService.updateSupplierStatus(supplier.id, newStatus)
      success(`Supplier is now ${newStatus}`)
      fetchSuppliers()
    } catch (err) {
      error(err.message || 'Failed to update supplier status')
    }
  }

  const handleConfirmDelete = async () => {
    if (!supplierToDelete) return
    setIsDeleting(true)
    try {
      await supplierService.deleteSupplier(supplierToDelete.id)
      success(`Supplier '${supplierToDelete.name}' deleted successfully`)
      setSupplierToDelete(null)
      fetchSuppliers()
    } catch (err) {
      error(err.message || 'Cannot delete supplier with linked products or purchase orders')
    } finally {
      setIsDeleting(false)
    }
  }

  const columns = [
    {
      header: 'Supplier Vendor',
      accessor: 'name',
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-slate-900 text-white flex items-center justify-center font-bold text-xs">
            <Building2 className="w-4 h-4 text-indigo-300" />
          </div>
          <div>
            <p className="font-bold text-slate-900 text-sm">{row.name}</p>
            {row.contactPerson && (
              <p className="text-[11px] text-slate-500 font-medium">Contact: {row.contactPerson}</p>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Email (Requisition Alerts)',
      accessor: 'email',
      render: (row) => (
        <span className="text-xs text-slate-700 flex items-center gap-1.5 font-medium">
          <Mail className="w-3.5 h-3.5 text-slate-400 shrink-0" /> {row.email}
        </span>
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
      header: 'GSTIN',
      accessor: 'gstin',
      render: (row) => (
        <span className="text-xs font-mono text-slate-600">{row.gstin || '—'}</span>
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
            title="Edit Supplier"
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
            onClick={() => setSupplierToDelete(row)}
            className="text-rose-600 hover:text-rose-800"
            title="Delete Supplier"
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
            Suppliers & Vendors
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Maintain external vendor contacts and dispatch channels for automated low stock procurement.
          </p>
        </div>

        <Button variant="primary" icon={Plus} onClick={handleOpenCreate}>
          Register Supplier
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
              placeholder="Search vendor, contact, email..."
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
              <option value="ACTIVE" className="bg-[var(--color-card-bg)]">Active Suppliers</option>
              <option value="INACTIVE" className="bg-[var(--color-card-bg)]">Inactive</option>
              <option value="ALL" className="bg-[var(--color-card-bg)]">All Vendors</option>
            </select>
          </div>

          <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchSuppliers}>
            Refresh
          </Button>
        </div>

        <Table
          columns={columns}
          data={suppliers}
          isLoading={isLoading}
          emptyMessage="No suppliers found"
          emptySubtext="Register suppliers to begin placing purchase orders and automated stock requisition."
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
        title={editingSupplier ? `Edit Supplier — ${editingSupplier.name}` : 'Register New Vendor'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Company / Vendor Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. TechSource Distribution Ltd"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Contact Person
              </label>
              <input
                type="text"
                value={formData.contactPerson}
                onChange={(e) => setFormData({ ...formData, contactPerson: e.target.value })}
                placeholder="e.g. Anand Kumar"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Supplier Email (Receives POs) *
              </label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="e.g. supply@techsource.com"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>

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
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              GSTIN / Vendor Tax ID
            </label>
            <input
              type="text"
              value={formData.gstin}
              onChange={(e) => setFormData({ ...formData, gstin: e.target.value })}
              placeholder="e.g. 29TECH1234F1Z8"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Address</label>
            <input
              type="text"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              placeholder="Warehouse address..."
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
              {editingSupplier ? 'Save Changes' : 'Register Supplier'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={Boolean(supplierToDelete)}
        onClose={() => setSupplierToDelete(null)}
        onConfirm={handleConfirmDelete}
        isLoading={isDeleting}
        title={`Delete Supplier '${supplierToDelete?.name}'?`}
        message="Are you sure you want to delete this supplier vendor? If they have linked purchase orders or products, deletion cannot be completed."
      />
    </AnimatedPage>
  )
}

export default Suppliers
