import React, { useState, useEffect } from 'react'
import { Layers, Plus, Search, Edit, Trash2, RefreshCw } from 'lucide-react'
import Card from '../components/Card'
import Table from '../components/Table'
import Button from '../components/Button'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import AnimatedPage from '../components/AnimatedPage'
import SearchInput from '../components/SearchInput'
import { categoryService } from '../services/categoryService'
import { useToast } from '../context/ToastContext'

export function Categories() {
  const { success, error } = useToast()

  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  // Modals
  const [showModal, setShowModal] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [categoryToDelete, setCategoryToDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchCategories = async () => {
    setIsLoading(true)
    try {
      const res = await categoryService.getAllCategories()
      setCategories(res || [])
    } catch (err) {
      error(err.message || 'Failed to load categories')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchCategories()
  }, [])

  const filteredCategories = categories.filter(
    (c) =>
      c.name?.toLowerCase().includes(search.toLowerCase()) ||
      c.description?.toLowerCase().includes(search.toLowerCase())
  )

  const handleOpenCreate = () => {
    setEditingCategory(null)
    setName('')
    setDescription('')
    setShowModal(true)
  }

  const handleOpenEdit = (c) => {
    setEditingCategory(c)
    setName(c.name || '')
    setDescription(c.description || '')
    setShowModal(true)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) {
      error('Category name is required')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingCategory) {
        await categoryService.updateCategory(editingCategory.id, {
          name: name.trim(),
          description: description.trim() || null,
        })
        success(`Category '${name}' updated successfully`)
      } else {
        await categoryService.createCategory({
          name: name.trim(),
          description: description.trim() || null,
        })
        success(`Category '${name}' created successfully`)
      }
      setShowModal(false)
      fetchCategories()
    } catch (err) {
      error(err.message || 'Failed to save category')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return
    setIsDeleting(true)
    try {
      await categoryService.deleteCategory(categoryToDelete.id)
      success(`Category '${categoryToDelete.name}' deleted successfully`)
      setCategoryToDelete(null)
      fetchCategories()
    } catch (err) {
      error(err.message || 'Cannot delete category that contains products')
    } finally {
      setIsDeleting(false)
    }
  }

  const columns = [
    {
      header: 'Category Name',
      accessor: 'name',
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <Layers className="w-4 h-4" />
          </div>
          <span className="font-bold text-slate-900 text-sm">{row.name}</span>
        </div>
      ),
    },
    {
      header: 'Description',
      accessor: 'description',
      render: (row) => (
        <span className="text-xs text-slate-500">{row.description || 'No description provided'}</span>
      ),
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
            title="Edit Category"
          />
          <Button
            variant="ghost"
            size="sm"
            icon={Trash2}
            onClick={() => setCategoryToDelete(row)}
            className="text-rose-600 hover:text-rose-800"
            title="Delete Category"
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
            Product Categories
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Organize products into functional departments and merchandise classifications.
          </p>
        </div>

        <Button variant="primary" icon={Plus} onClick={handleOpenCreate}>
          Add Category
        </Button>
      </div>

      {/* Main Table Card */}
      <Card>
        <div className="p-4 sm:p-5 border-b border-[var(--color-border-secondary)] flex flex-wrap items-center justify-between gap-3">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search categories..."
            className="w-full sm:w-72"
          />

          <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchCategories}>
            Refresh
          </Button>
        </div>

        <Table
          columns={columns}
          data={filteredCategories}
          isLoading={isLoading}
          emptyMessage="No categories found"
          emptySubtext="Click 'Add Category' above to create your first product category."
        />
      </Card>

      {/* Create / Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editingCategory ? `Edit Category — ${editingCategory.name}` : 'New Product Category'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Category Name *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Computer Accessories"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Description
            </label>
            <textarea
              rows="3"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Classification details..."
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {editingCategory ? 'Save Changes' : 'Create Category'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={Boolean(categoryToDelete)}
        onClose={() => setCategoryToDelete(null)}
        onConfirm={handleConfirmDelete}
        isLoading={isDeleting}
        title={`Delete Category '${categoryToDelete?.name}'?`}
        message="Are you sure you want to delete this category? If any product is associated with it, deletion will be blocked by foreign key constraints."
      />
    </AnimatedPage>
  )
}

export default Categories
