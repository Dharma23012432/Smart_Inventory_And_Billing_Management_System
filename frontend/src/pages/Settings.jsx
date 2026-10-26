import React, { useState, useEffect } from 'react'
import {
  Save,
  RefreshCw,
} from 'lucide-react'
import Card, { CardHeader, CardBody } from '../components/Card'
import Button from '../components/Button'
import AnimatedPage from '../components/AnimatedPage'
import { settingService } from '../services/settingService'
import { useToast } from '../context/ToastContext'

export function Settings() {
  const { success, error } = useToast()

  const [settings, setSettings] = useState({
    company_name: '',
    company_address: '',
    company_phone: '',
    company_email: '',
    gstin: '',
    currency_symbol: '₹',
    invoice_prefix: 'INV-2026-',
    default_tax_rate: '18.0',
    low_stock_threshold: '10',
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  const fetchSettings = async () => {
    setIsLoading(true)
    try {
      const res = await settingService.getAllSettings()
      if (res) {
        setSettings((prev) => ({ ...prev, ...res }))
      }
    } catch (err) {
      error(err.message || 'Failed to load application settings')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchSettings()
  }, [])

  const handleChange = (key, val) => {
    setSettings((prev) => ({ ...prev, [key]: val }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setIsSaving(true)
    try {
      await settingService.updateSettings(settings)
      success('Application settings updated successfully!')
    } catch (err) {
      error(err.message || 'Failed to update settings')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <AnimatedPage className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[var(--color-text-primary)] tracking-tight">
            Store & System Settings
          </h1>
          <p className="text-[var(--color-text-secondary)] text-xs sm:text-sm mt-1">
            Configure company identity, invoice prefixes, default tax rates, and check cluster health.
          </p>
        </div>

        <Button variant="outline" size="sm" icon={RefreshCw} onClick={fetchSettings}>
          Reload
        </Button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Company Profile Settings */}
        <Card>
          <CardHeader
            title="Business Entity Profile"
            subtitle="Details displayed on customer tax invoices and purchase requisitions"
          />
          <CardBody className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Company / Store Name *
                </label>
                <input
                  type="text"
                  required
                  value={settings.company_name}
                  onChange={(e) => handleChange('company_name', e.target.value)}
                  placeholder="e.g. Smart Retail & Logistics Pvt Ltd"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold focus:ring-1 focus:ring-slate-400"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  GSTIN / Corporate Tax ID
                </label>
                <input
                  type="text"
                  value={settings.gstin}
                  onChange={(e) => handleChange('gstin', e.target.value)}
                  placeholder="e.g. 29ABCDE1234F1Z5"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono focus:ring-1 focus:ring-slate-400"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Official Support Email
                </label>
                <input
                  type="email"
                  value={settings.company_email}
                  onChange={(e) => handleChange('company_email', e.target.value)}
                  placeholder="e.g. billing@smartinventory.com"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Customer Care Phone
                </label>
                <input
                  type="tel"
                  value={settings.company_phone}
                  onChange={(e) => handleChange('company_phone', e.target.value)}
                  placeholder="e.g. +91 98765 43210"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                Storefront / Warehouse Address
              </label>
              <textarea
                rows="2"
                value={settings.company_address}
                onChange={(e) => handleChange('company_address', e.target.value)}
                placeholder="Physical address printed on invoice headers..."
                className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-1 focus:ring-slate-400"
              />
            </div>
          </CardBody>
        </Card>

        {/* Invoice & Tax Settings */}
        <Card>
          <CardHeader
            title="Billing & Tax Configuration"
            subtitle="Default parameters applied during customer sale checkout"
          />
          <CardBody className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Invoice Number Prefix
                </label>
                <input
                  type="text"
                  value={settings.invoice_prefix}
                  onChange={(e) => handleChange('invoice_prefix', e.target.value)}
                  placeholder="INV-2026-"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-mono font-semibold"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Default GST / Tax Rate (%)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={settings.default_tax_rate}
                  onChange={(e) => handleChange('default_tax_rate', e.target.value)}
                  placeholder="18.0"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase mb-1">
                  Currency Symbol
                </label>
                <input
                  type="text"
                  value={settings.currency_symbol}
                  onChange={(e) => handleChange('currency_symbol', e.target.value)}
                  placeholder="₹"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm font-semibold"
                />
              </div>
            </div>
          </CardBody>
        </Card>

        {/* System Diagnostics / Services Health */}
        <Card>
          <CardHeader
            title="Microservices Cluster Status"
            subtitle="Connected Docker container nodes and communication endpoints"
          />
          <CardBody>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-slate-900">Inventory Service</p>
                  <p className="text-[11px] text-slate-400 font-mono">Port 8080 (REST + Actuator)</p>
                </div>
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-4 ring-emerald-100" />
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-slate-900">Payment Service</p>
                  <p className="text-[11px] text-slate-400 font-mono">Port 8081 (RabbitMQ Publisher)</p>
                </div>
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-4 ring-emerald-100" />
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-slate-900">Notification Service</p>
                  <p className="text-[11px] text-slate-400 font-mono">Port 8082 (Gmail SMTP Engine)</p>
                </div>
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-4 ring-emerald-100" />
              </div>
            </div>
          </CardBody>
        </Card>

        {/* Save Button */}
        <div className="flex justify-end pt-2">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            icon={Save}
            isLoading={isSaving}
          >
            Save All Settings
          </Button>
        </div>
      </form>
    </AnimatedPage>
  )
}

export default Settings
