import React, { useState, useEffect } from 'react'
import {
  Plus,
  Search,
  Edit,
  Trash2,
  RefreshCw,
  Power,
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
import { productService } from '../services/productService'
import { categoryService } from '../services/categoryService'
import { supplierService } from '../services/supplierService'
import { formatCurrency } from '../utils/formatters'
import { useToast } from '../context/ToastContext'

const INITIAL_FORM = {
  name: '',
  sku: '',
  categoryId: '',
  supplierId: '',
  description: '',
  unitPrice: '',
  purchasePrice: '',
  unitOfMeasure: 'PIECE',
  initialQuantity: 10,
  reorderLevel: 5,
}

export function Products() {
  const { success, error } = useToast()

  // State
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [search, setSearch] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [statusFilter, setStatusFilter] = useState('ACTIVE')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [showModal, setShowModal] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [productToDelete, setProductToDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Load Categories & Suppliers
  useEffect(() => {
    const loadLookups = async () => {
      try {
        const [catRes, supRes] = await Promise.all([
          categoryService.getAllCategories().catch(() => []),
          supplierService.getAllActiveSuppliers().catch(() => []),
        ])
        setCategories(catRes || [])
        setSuppliers(supRes || [])
      } catch (err) {
        // Silently handle
      }
    }
    loadLookups()
  }, [])

  // Load Products
  const fetchProducts = async () => {
    setIsLoading(true)
    try {
      const res = await productService.getProducts({
        search: search.trim() || undefined,
        categoryId: selectedCategory || undefined,
        status: statusFilter,
        page,
        size,
        sortBy: 'name',
        direction: 'ASC',
      })
      setProducts(res?.content || [])
      setTotalPages(res?.totalPages || 1)
      setTotalElements(res?.totalElements || 0)
    } catch (err) {
      error(err.message || 'Failed to fetch products')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [page, size, search, selectedCategory, statusFilter])

  // Open Create Modal
  const handleOpenCreate = () => {
    setEditingProduct(null)
    setFormData({
      ...INITIAL_FORM,
      categoryId: categories.length > 0 ? categories[0].id : '',
      supplierId: suppliers.length > 0 ? suppliers[0].id : '',
    })
    setShowModal(true)
  }

  // Open Edit Modal
  const handleOpenEdit = (p) => {
    setEditingProduct(p)
    setFormData({
      name: p.name || '',
      sku: p.sku || '',
      categoryId: p.categoryId || '',
      supplierId: p.supplierId || '',
      description: p.description || '',
      unitPrice: p.unitPrice || '',
      purchasePrice: p.purchasePrice || '',
      unitOfMeasure: p.unitOfMeasure || 'PIECE',
    })
    setShowModal(true)
  }

  // Submit Product Create / Update
  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!formData.name.trim() || !formData.sku.trim() || !formData.categoryId) {
      error('Please fill all required fields')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim(),
        sku: formData.sku.trim(),
        categoryId: Number(formData.categoryId),
        supplierId: formData.supplierId ? Number(formData.supplierId) : null,
        description: formData.description?.trim() || null,
        unitPrice: Number(formData.unitPrice),
        purchasePrice: Number(formData.purchasePrice),
        unitOfMeasure: formData.unitOfMeasure || 'PIECE',
      }

      if (editingProduct) {
        await productService.updateProduct(editingProduct.id, payload)
        success(`Product '${payload.name}' updated successfully`)
      } else {
        await productService.createProduct({
          ...payload,
          initialQuantity: Number(formData.initialQuantity) || 0,
          reorderLevel: Number(formData.reorderLevel) || 5,
        })
        success(`Product '${payload.name}' created with initial inventory!`)
      }

      setShowModal(false)
      fetchProducts()
    } catch (err) {
      error(err.message || 'Failed to save product')
    } finally {
      setIsSubmitting(false)
    }
  }

  // Toggle Product Status
  const handleToggleStatus = async (product) => {
    const newStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await productService.updateProductStatus(product.id, newStatus)
      success(`Product is now ${newStatus}`)
      fetchProducts()
    } catch (err) {
      error(err.message || 'Failed to update product status')
    }
  }

  // Delete Product
  const handleConfirmDelete = async () => {
    if (!productToDelete) return
    setIsDeleting(true)
    try {
      await productService.deleteProduct(productToDelete.id)
      success(`Product '${productToDelete.name}' deleted successfully`)
      setProductToDelete(null)
      fetchProducts()
    } catch (err) {
      error(err.message || 'Cannot delete product with existing billing or inventory history')
    } finally {
      setIsDeleting(false)
    }
  }

  const columns = [
    {
      header: 'Product Name',
      accessor: 'name',
      className: 'w-[28%] min-w-[170px]',
      render: (row) => (
        <div className="min-w-0 pr-2">
          <span
            className="font-bold text-[var(--color-text-primary)] text-sm truncate block"
            title={row.description ? `${row.name} — ${row.description}` : row.name}
          >
            {row.name}
          </span>
        </div>
      ),
    },
    {
      header: 'SKU',
      accessor: 'sku',
      className: 'w-[12%] min-w-[85px]',
      render: (row) => (
        <span
          className="font-mono text-xs font-semibold text-[var(--color-text-secondary)] truncate block"
          title={row.sku}
        >
          {row.sku}
        </span>
      ),
    },
    {
      header: 'Category',
      accessor: 'categoryName',
      className: 'w-[12%] min-w-[90px]',
      render: (row) => (
        <span
          className="text-xs font-semibold text-[var(--color-text-secondary)] bg-[var(--color-surface-tertiary)] border border-[var(--color-border-secondary)] px-2 py-0.5 rounded-md inline-block max-w-full truncate"
          title={row.categoryName || 'General'}
        >
          {row.categoryName || 'General'}
        </span>
      ),
    },
    {
      header: 'Supplier',
      accessor: 'supplierName',
      className: 'w-[11%] min-w-[85px]',
      render: (row) => (
        <span
          className="text-xs text-[var(--color-text-secondary)] truncate block"
          title={row.supplierName || '—'}
        >
          {row.supplierName || '—'}
        </span>
      ),
    },
    {
      header: 'Selling Price',
      accessor: 'unitPrice',
      align: 'right',
      className: 'w-[11%] min-w-[85px]',
      render: (row) => (
        <span className="font-extrabold text-[var(--color-text-primary)] text-xs sm:text-sm whitespace-nowrap">
          {formatCurrency(row.unitPrice)}
        </span>
      ),
    },
    {
      header: 'Cost Price',
      accessor: 'purchasePrice',
      align: 'right',
      className: 'w-[10%] min-w-[80px]',
      render: (row) => (
        <span className="text-xs text-[var(--color-text-tertiary)] font-medium whitespace-nowrap">
          {formatCurrency(row.purchasePrice)}
        </span>
      ),
    },
    {
      header: 'UOM',
      accessor: 'unitOfMeasure',
      align: 'center',
      className: 'w-[5%] min-w-[45px]',
      render: (row) => (
        <span className="text-[10px] font-bold text-slate-500 uppercase px-1.5 py-0.5 bg-[var(--color-surface-tertiary)] rounded">
          {row.unitOfMeasure}
        </span>
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      align: 'center',
      className: 'w-[5%] min-w-[65px]',
      render: (row) => <Badge status={row.status} />,
    },
    {
      header: 'Actions',
      align: 'right',
      className: 'w-[6%] min-w-[70px]',
      render: (row) => (
        <div className="flex items-center justify-end gap-0.5" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="xs"
            icon={Edit}
            onClick={() => handleOpenEdit(row)}
            title="Edit Product"
            className="p-1.5 hover:bg-purple-500/10 text-slate-400 hover:text-purple-400"
          />
          <Button
            variant="ghost"
            size="xs"
            icon={Power}
            onClick={() => handleToggleStatus(row)}
            className={`p-1.5 ${row.status === 'ACTIVE' ? 'text-emerald-500 hover:bg-emerald-500/10' : 'text-slate-400 hover:bg-slate-500/10'}`}
            title={row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          />
          <Button
            variant="ghost"
            size="xs"
            icon={Trash2}
            onClick={() => setProductToDelete(row)}
            className="p-1.5 text-rose-500 hover:text-rose-400 hover:bg-rose-500/10"
            title="Delete Product"
          />
        </div>
      ),
    },
  ]

  return (
    <AnimatedPage className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Products Catalog
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Manage your store's items, pricing, SKU codes, and suppliers.
          </p>
        </div>

        <Button variant="primary" icon={Plus} onClick={handleOpenCreate}>
          Add Product
        </Button>
      </div>

      {/* Filter Bar & Table Card */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3 flex-1">
            {/* Search Input */}
            <SearchInput
              value={search}
              onChange={(val) => {
                setSearch(val)
                setPage(0)
              }}
              placeholder="Search name or SKU..."
              className="w-full sm:w-64"
            />

            {/* Category Filter */}
            <select
              value={selectedCategory}
              onChange={(e) => {
                setSelectedCategory(e.target.value)
                setPage(0)
              }}
              className="px-3 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] font-medium cursor-pointer"
            >
              <option value="" className="bg-[var(--color-card-bg)]">All Categories</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id} className="bg-[var(--color-card-bg)]">
                  {c.name}
                </option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setPage(0)
              }}
              className="px-3 py-2 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-xl text-xs sm:text-sm text-[var(--color-text-primary)] font-medium cursor-pointer"
            >
              <option value="ACTIVE" className="bg-[var(--color-card-bg)]">Active Only</option>
              <option value="INACTIVE" className="bg-[var(--color-card-bg)]">Inactive Only</option>
              <option value="ALL" className="bg-[var(--color-card-bg)]">All Statuses</option>
            </select>
          </div>

          <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchProducts}>
            Refresh
          </Button>
        </div>

        {/* Table */}
        <Table
          columns={columns}
          data={products}
          isLoading={isLoading}
          emptyMessage="No products found"
          emptySubtext="Try adjusting your filters or click 'Add Product' above."
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

      {/* Create / Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        size="lg"
        title={editingProduct ? `Edit Product — ${editingProduct.name}` : 'Create New Product'}
        subtitle="Configure product details, category, and retail price"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Product Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. Wireless Mouse M220"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Unique SKU *
              </label>
              <input
                type="text"
                required
                value={formData.sku}
                onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                placeholder="e.g. ELEC-LOG-M220"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:ring-1 focus:ring-slate-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Category *
              </label>
              <select
                required
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              >
                <option value="">Select Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Supplier Vendor
              </label>
              <select
                value={formData.supplierId}
                onChange={(e) => setFormData({ ...formData, supplierId: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              >
                <option value="">Select Supplier (Optional)</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Selling Price (₹) *
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                value={formData.unitPrice}
                onChange={(e) => setFormData({ ...formData, unitPrice: e.target.value })}
                placeholder="899.00"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Cost Price (₹) *
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={formData.purchasePrice}
                onChange={(e) => setFormData({ ...formData, purchasePrice: e.target.value })}
                placeholder="650.00"
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold focus:ring-1 focus:ring-slate-400"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Unit of Measure
              </label>
              <select
                value={formData.unitOfMeasure}
                onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              >
                <option value="PIECE">PIECE</option>
                <option value="BOX">BOX</option>
                <option value="KG">KG</option>
                <option value="LITRE">LITRE</option>
                <option value="PACKET">PACKET</option>
                <option value="METER">METER</option>
              </select>
            </div>
          </div>

          {!editingProduct && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-3 bg-slate-50 border border-slate-200/80 rounded-xl">
              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Initial Stock Quantity
                </label>
                <input
                  type="number"
                  min="0"
                  value={formData.initialQuantity}
                  onChange={(e) => setFormData({ ...formData, initialQuantity: e.target.value })}
                  className="w-full px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm font-semibold"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Reorder Threshold Level
                </label>
                <input
                  type="number"
                  min="1"
                  value={formData.reorderLevel}
                  onChange={(e) => setFormData({ ...formData, reorderLevel: e.target.value })}
                  className="w-full px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm font-semibold"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Description</label>
            <textarea
              rows="2"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Detailed product specifications..."
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingProduct ? 'Save Changes' : 'Create Product'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={Boolean(productToDelete)}
        onClose={() => setProductToDelete(null)}
        onConfirm={handleConfirmDelete}
        isLoading={isDeleting}
        title={`Delete Product '${productToDelete?.name}'?`}
        message="Are you sure you want to delete this product? If it has linked purchase orders or historical bills, it cannot be deleted."
      />
    </AnimatedPage>
  )
}

export default Products
