import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ShoppingCart,
  Plus,
  Trash2,
  Search,
  User,
  CheckCircle,
  Package,
} from 'lucide-react'
import Card, { CardHeader, CardBody } from '../components/Card'
import Button from '../components/Button'
import Modal from '../components/Modal'
import AnimatedPage from '../components/AnimatedPage'
import SearchInput from '../components/SearchInput'
import { productService } from '../services/productService'
import { customerService } from '../services/customerService'
import { billService } from '../services/billService'
import { paymentService } from '../services/paymentService'
import { formatCurrency } from '../utils/formatters'
import { useToast } from '../context/ToastContext'
import { AppEvents, publishEvent } from '../utils/eventBus'

export function Sales() {
  const navigate = useNavigate()
  const { success, error } = useToast()

  // State
  const [customers, setCustomers] = useState([])
  const [products, setProducts] = useState([])
  const [selectedCustomerId, setSelectedCustomerId] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [items, setItems] = useState([])
  const [discountAmount, setDiscountAmount] = useState(0)
  const [taxPercent, setTaxPercent] = useState(18.0)
  const [notes, setNotes] = useState('')

  // Modals & Processors
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showNewCustomerModal, setShowNewCustomerModal] = useState(false)
  const [newCustomerForm, setNewCustomerForm] = useState({ name: '', email: '', phone: '', address: '' })
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [pendingBill, setPendingBill] = useState(null)
  const [paymentForm, setPaymentForm] = useState({ method: 'UPI', reference: '' })

  // Load active customers and products
  useEffect(() => {
    const loadMasterData = async () => {
      try {
        const [custRes, prodRes] = await Promise.all([
          customerService.getAllActiveCustomers().catch(() => []),
          productService.getProducts({ status: 'ACTIVE', size: 100 }).catch(() => ({ content: [] })),
        ])
        setCustomers(custRes || [])
        setProducts(prodRes?.content || [])
        if (custRes && custRes.length > 0) {
          // Default to first customer (or walk-in customer)
          setSelectedCustomerId(custRes[0].id)
        }
      } catch (err) {
        error('Failed to load customers and product catalog')
      }
    }
    loadMasterData()
  }, [])

  // Filtered products for quick picker
  const filteredProducts = products.filter(
    (p) =>
      p.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sku?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  // Add product to cart
  const handleAddItem = (product) => {
    setItems((prev) => {
      const existing = prev.find((item) => item.productId === product.id)
      if (existing) {
        return prev.map((item) =>
          item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item
        )
      }
      return [
        ...prev,
        {
          productId: product.id,
          name: product.name,
          sku: product.sku,
          unitPrice: product.unitPrice,
          quantity: 1,
          discountPercent: 0,
        },
      ]
    })
  }

  // Update item quantity
  const handleUpdateQuantity = (productId, delta) => {
    setItems((prev) =>
      prev
        .map((item) => {
          if (item.productId === productId) {
            const newQty = item.quantity + delta
            return newQty > 0 ? { ...item, quantity: newQty } : null
          }
          return item
        })
        .filter(Boolean)
    )
  }

  // Remove item
  const handleRemoveItem = (productId) => {
    setItems((prev) => prev.filter((item) => item.productId !== productId))
  }

  // Live bill calculations
  const subtotal = items.reduce((sum, item) => {
    const itemSub = item.quantity * item.unitPrice
    const itemDisc = (itemSub * (item.discountPercent || 0)) / 100
    return sum + (itemSub - itemDisc)
  }, 0)

  const parsedDiscount = Math.min(Math.max(Number(discountAmount) || 0, 0), subtotal)
  const discountedSubtotal = Math.max(0, subtotal - parsedDiscount)
  const parsedTaxPercent = Math.max(Number(taxPercent) || 0, 0)
  const taxAmount = (discountedSubtotal * parsedTaxPercent) / 100
  const grandTotal = discountedSubtotal + taxAmount

  // Quick Customer Creation
  const handleCreateCustomer = async (e) => {
    e.preventDefault()
    if (!newCustomerForm.name.trim()) {
      error('Customer name is required')
      return
    }
    try {
      const created = await customerService.createCustomer({
        name: newCustomerForm.name.trim(),
        email: newCustomerForm.email.trim() || null,
        phone: newCustomerForm.phone.trim() || null,
        address: newCustomerForm.address.trim() || null,
      })
      success(`Customer '${created.name}' created!`)
      setCustomers((prev) => [...prev, created])
      setSelectedCustomerId(created.id)
      setShowNewCustomerModal(false)
      setNewCustomerForm({ name: '', email: '', phone: '', address: '' })
    } catch (err) {
      error(err.message || 'Failed to create customer')
    }
  }

  // Create Bill handler (Draft or Proceed to Confirm/Payment)
  const handleCreateBill = async (andConfirm = false, andPay = false) => {
    if (items.length === 0) {
      error('Please add at least one product to the bill')
      return
    }
    if (!selectedCustomerId) {
      error('Please select a customer')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = {
        customerId: Number(selectedCustomerId),
        discountAmount: parsedDiscount,
        taxPercent: parsedTaxPercent,
        notes: notes.trim() || null,
        items: items.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
          discountPercent: i.discountPercent || 0,
        })),
      }

      // Step 1: Create draft bill
      const draft = await billService.createBill(payload)
      success(`Bill ${draft.billNumber} created in DRAFT`)
      publishEvent(AppEvents.INVOICE_CREATED, draft)

      if (andConfirm || andPay) {
        // Step 2: Confirm bill and deduct inventory
        const confirmed = await billService.confirmBill(draft.id)
        success(`Bill ${draft.billNumber} confirmed! Stock deducted.`)
        publishEvent(AppEvents.INVOICE_CONFIRMED, confirmed)
        publishEvent(AppEvents.STOCK_UPDATED)

        if (andPay) {
          // Open payment modal
          setPendingBill({ ...draft, ...confirmed })
          setShowPaymentModal(true)
          setIsSubmitting(false)
          return
        }
      }

      // Reset cart and navigate to sales history
      setItems([])
      setNotes('')
      navigate('/sales-history')
    } catch (err) {
      error(err.message || 'Failed to complete transaction')
    } finally {
      setIsSubmitting(false)
    }
  }

  // Process Payment & Dispatch Email
  const handleProcessPayment = async () => {
    if (!pendingBill) return
    setIsSubmitting(true)
    try {
      await paymentService.processPayment({
        billId: pendingBill.id,
        amountPaid: pendingBill.totalAmount,
        paymentMethod: paymentForm.method,
        referenceNumber: paymentForm.reference.trim() || `REF-${Date.now().toString().slice(-6)}`,
      })
      success(`Payment processed! Invoice receipt sent to customer.`)
      publishEvent(AppEvents.INVOICE_PAID, { billId: pendingBill.id })
      publishEvent(AppEvents.EMAIL_SENT)
      setShowPaymentModal(false)
      setPendingBill(null)
      setItems([])
      navigate('/sales-history')
    } catch (err) {
      error(err.message || 'Payment processing failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatedPage className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Point of Sale & New Bill
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Build a customer sales bill, deduct inventory, and process instant payments.
          </p>
        </div>
      </div>

      {/* Main Grid: Catalog on left, Cart on right */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Product Catalog Picker (7 Cols) */}
        <div className="lg:col-span-7 space-y-6">
          {/* Customer Selection Banner */}
          <Card className="p-4 sm:p-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
                  <User className="w-5 h-5" />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[var(--color-text-secondary)] uppercase tracking-wider">
                    Customer Account
                  </label>
                  <select
                    value={selectedCustomerId}
                    onChange={(e) => setSelectedCustomerId(e.target.value)}
                    className="mt-1 font-bold text-[var(--color-text-primary)] text-sm bg-transparent border-0 focus:ring-0 cursor-pointer p-0"
                  >
                    {customers.map((c) => (
                      <option key={c.id} value={c.id} className="bg-[var(--color-card-bg)] text-[var(--color-text-primary)]">
                        {c.name} {c.email ? `(${c.email})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <Button
                variant="outline"
                size="sm"
                icon={Plus}
                onClick={() => setShowNewCustomerModal(true)}
              >
                New Customer
              </Button>
            </div>
          </Card>

          {/* Product Catalog */}
          <Card>
            <CardHeader
              title="Product Catalog"
              subtitle="Select items to add to current bill"
              action={
                <SearchInput
                  value={searchQuery}
                  onChange={setSearchQuery}
                  placeholder="Search name or SKU..."
                  className="w-56 sm:w-64"
                />
              }
            />
            <CardBody className="p-4 sm:p-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-[460px] overflow-y-auto p-1">
                {filteredProducts.length === 0 ? (
                  <div className="col-span-2 py-12 text-center text-[var(--color-text-tertiary)] text-xs">
                    <Package className="w-8 h-8 mx-auto text-slate-300 dark:text-slate-600 mb-2" />
                    <p className="font-semibold text-[var(--color-text-secondary)]">No active products found</p>
                  </div>
                ) : (
                  filteredProducts.map((p) => {
                    const cartItem = items.find((i) => i.productId === p.id)
                    return (
                      <div
                        key={p.id}
                        onClick={() => handleAddItem(p)}
                        className={`p-3.5 rounded-xl border transition-all cursor-pointer select-none flex flex-col justify-between ${
                          cartItem
                            ? 'bg-indigo-500/10 border-indigo-500 shadow-xs ring-1 ring-indigo-500/30'
                            : 'bg-[var(--color-card-bg)] border-[var(--color-card-border)] hover:border-[var(--color-accent)] hover:shadow-xs'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <h4 className="font-bold text-sm text-[var(--color-text-primary)] truncate">{p.name}</h4>
                            <p className="text-[11px] font-mono text-[var(--color-text-tertiary)] mt-0.5">{p.sku}</p>
                          </div>
                          {cartItem && (
                            <span className="shrink-0 px-2 py-0.5 bg-[var(--color-accent)] text-white font-extrabold text-[11px] rounded-full">
                              x{cartItem.quantity}
                            </span>
                          )}
                        </div>

                        <div className="flex items-center justify-between mt-3 pt-2 border-t border-[var(--color-border-secondary)]">
                          <span className="text-xs text-[var(--color-text-secondary)] font-medium">
                            {p.categoryName || 'General'}
                          </span>
                          <span className="font-extrabold text-sm text-[var(--color-text-primary)]">
                            {formatCurrency(p.unitPrice)}
                          </span>
                        </div>
                      </div>
                    )
                  })
                )}
              </div>
            </CardBody>
          </Card>
        </div>

        {/* Invoice / Cart Breakdown (5 Cols) */}
        <div className="lg:col-span-5">
          <Card className="sticky top-24">
            <CardHeader
              title="Cart Items"
              subtitle={`${items.length} unique products`}
              action={
                items.length > 0 && (
                  <button
                    onClick={() => setItems([])}
                    className="text-xs text-rose-600 hover:text-rose-800 font-semibold"
                  >
                    Clear All
                  </button>
                )
              }
            />

            {/* Cart Items List */}
            <div className="divide-y divide-[var(--color-border-secondary)] max-h-[280px] overflow-y-auto px-5">
              {items.length === 0 ? (
                <div className="py-12 text-center text-[var(--color-text-tertiary)] text-xs">
                  <ShoppingCart className="w-8 h-8 mx-auto text-slate-300 dark:text-slate-600 mb-2" />
                  <p className="font-semibold text-[var(--color-text-primary)]">Your billing cart is empty</p>
                  <p className="text-[11px] text-[var(--color-text-tertiary)] mt-1">Select items from the catalog to build bill.</p>
                </div>
              ) : (
                items.map((item) => (
                  <div key={item.productId} className="py-3 flex items-center justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-bold text-[var(--color-text-primary)] truncate">{item.name}</p>
                      <p className="text-[11px] text-[var(--color-text-tertiary)]">{formatCurrency(item.unitPrice)} each</p>
                    </div>

                    {/* Quantity Stepper */}
                    <div className="flex items-center gap-2">
                      <div className="flex items-center border border-[var(--color-border-primary)] rounded-lg bg-[var(--color-surface-tertiary)]">
                        <button
                          type="button"
                          onClick={() => handleUpdateQuantity(item.productId, -1)}
                          className="px-2 py-0.5 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] rounded-l-lg font-bold text-xs"
                        >
                          -
                        </button>
                        <span className="px-2 text-xs font-bold text-[var(--color-text-primary)] min-w-[20px] text-center">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleUpdateQuantity(item.productId, 1)}
                          className="px-2 py-0.5 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)] rounded-r-lg font-bold text-xs"
                        >
                          +
                        </button>
                      </div>

                      <span className="font-extrabold text-xs text-[var(--color-text-primary)] w-16 text-right">
                        {formatCurrency(item.quantity * item.unitPrice)}
                      </span>

                      <button
                        onClick={() => handleRemoveItem(item.productId)}
                        className="text-[var(--color-text-tertiary)] hover:text-rose-500 p-1 transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Calculations & Summary */}
            <div className="p-5 border-t border-[var(--color-border-secondary)] bg-[var(--color-surface-inset)] space-y-3 text-xs sm:text-sm">
              <div className="flex justify-between text-[var(--color-text-secondary)]">
                <span>Subtotal</span>
                <span className="font-semibold text-[var(--color-text-primary)]">{formatCurrency(subtotal)}</span>
              </div>

              {/* Discount Input */}
              <div className="flex items-center justify-between gap-4">
                <span className="text-[var(--color-text-secondary)]">Discount (₹)</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={discountAmount}
                  onChange={(e) => setDiscountAmount(e.target.value)}
                  className="w-24 px-2 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-lg text-right font-semibold text-xs focus:ring-1 focus:ring-[var(--color-accent)]"
                />
              </div>

              {/* Tax % Input */}
              <div className="flex items-center justify-between gap-4">
                <span className="text-[var(--color-text-secondary)]">GST / Tax (%)</span>
                <input
                  type="number"
                  min="0"
                  step="0.1"
                  value={taxPercent}
                  onChange={(e) => setTaxPercent(e.target.value)}
                  className="w-24 px-2 py-1 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-lg text-right font-semibold text-xs focus:ring-1 focus:ring-[var(--color-accent)]"
                />
              </div>

              <div className="flex justify-between text-[var(--color-text-secondary)]">
                <span>Tax Amount</span>
                <span className="font-semibold text-[var(--color-text-primary)]">{formatCurrency(taxAmount)}</span>
              </div>

              {/* Notes */}
              <div>
                <input
                  type="text"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Optional bill notes or remarks..."
                  className="w-full px-3 py-1.5 bg-[var(--color-input-bg)] border border-[var(--color-input-border)] rounded-lg text-xs placeholder-[var(--color-input-placeholder)] text-[var(--color-text-primary)] focus:outline-hidden"
                />
              </div>

              {/* Grand Total */}
              <div className="pt-3 border-t border-[var(--color-border-primary)] flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-[var(--color-text-tertiary)] uppercase tracking-wider block">
                    Grand Total
                  </span>
                  <span className="text-xl sm:text-2xl font-black text-[var(--color-text-primary)]">
                    {formatCurrency(grandTotal)}
                  </span>
                </div>
              </div>

              {/* Actions */}
              <div className="pt-2 flex flex-col gap-2">
                <Button
                  variant="primary"
                  className="w-full justify-center py-3"
                  isLoading={isSubmitting}
                  disabled={items.length === 0}
                  onClick={() => handleCreateBill(true, true)}
                >
                  Confirm & Pay Now
                </Button>

                <div className="grid grid-cols-2 gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="justify-center"
                    disabled={items.length === 0 || isSubmitting}
                    onClick={() => handleCreateBill(false, false)}
                  >
                    Save Draft
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    className="justify-center"
                    disabled={items.length === 0 || isSubmitting}
                    onClick={() => handleCreateBill(true, false)}
                  >
                    Confirm Only
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Quick Add Customer Modal */}
      <Modal
        isOpen={showNewCustomerModal}
        onClose={() => setShowNewCustomerModal(false)}
        title="Add New Customer"
        subtitle="Quick registration for POS billing"
      >
        <form onSubmit={handleCreateCustomer} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Full Name *
            </label>
            <input
              type="text"
              required
              value={newCustomerForm.name}
              onChange={(e) => setNewCustomerForm({ ...newCustomerForm, name: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              placeholder="e.g. Rahul Verma"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
              Email Address (for invoice)
            </label>
            <input
              type="email"
              value={newCustomerForm.email}
              onChange={(e) => setNewCustomerForm({ ...newCustomerForm, email: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              placeholder="e.g. rahul@example.com"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Phone Number</label>
            <input
              type="tel"
              value={newCustomerForm.phone}
              onChange={(e) => setNewCustomerForm({ ...newCustomerForm, phone: e.target.value })}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              placeholder="e.g. 9876543210"
            />
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowNewCustomerModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Save Customer
            </Button>
          </div>
        </form>
      </Modal>

      {/* Collect Payment Modal */}
      <Modal
        isOpen={showPaymentModal}
        onClose={() => setShowPaymentModal(false)}
        title={`Collect Payment — ${pendingBill?.billNumber}`}
        subtitle={`Amount: ${formatCurrency(pendingBill?.totalAmount || 0)}`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-600 uppercase mb-1">Payment Method</label>
            <div className="grid grid-cols-3 gap-2">
              {['UPI', 'CASH', 'CARD', 'BANK_TRANSFER', 'CHEQUE'].map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setPaymentForm({ ...paymentForm, method: m })}
                  className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all ${
                    paymentForm.method === m
                      ? 'bg-slate-900 text-white border-slate-900'
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
              Reference / Transaction ID
            </label>
            <input
              type="text"
              value={paymentForm.reference}
              onChange={(e) => setPaymentForm({ ...paymentForm, reference: e.target.value })}
              placeholder="e.g. UPI-99887766 or Cash Note"
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
            />
          </div>

          <div className="p-3 bg-indigo-50 border border-indigo-100 rounded-xl text-xs text-indigo-900 flex items-start gap-2">
            <CheckCircle className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
            <p>
              Once completed, an automated PDF/HTML invoice email will be immediately dispatched to the
              customer's email address.
            </p>
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <Button variant="outline" onClick={() => setShowPaymentModal(false)}>
              Skip & Pay Later
            </Button>
            <Button variant="primary" isLoading={isSubmitting} onClick={handleProcessPayment}>
              Complete Payment
            </Button>
          </div>
        </div>
      </Modal>
    </AnimatedPage>
  )
}

export default Sales
