import React from 'react'
import { AlertCircle, RotateCcw, Home } from 'lucide-react'

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[400px] flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-[var(--color-card-bg)] border border-[var(--color-card-border)] rounded-2xl p-6 shadow-xl text-center">
            <div className="w-12 h-12 rounded-2xl bg-rose-500/10 text-rose-500 flex items-center justify-center mx-auto mb-4">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-[var(--color-text-primary)]">
              Something went wrong
            </h3>
            <p className="text-xs text-[var(--color-text-secondary)] mt-1.5 leading-relaxed">
              An error occurred while loading this view. You can retry or return to the overview.
            </p>

            {this.state.error && (
              <div className="mt-4 p-3 bg-[var(--color-surface-inset)] rounded-xl text-left border border-[var(--color-border-secondary)] overflow-x-auto">
                <p className="text-[11px] font-mono text-rose-500 break-words">
                  {this.state.error.toString()}
                </p>
              </div>
            )}

            <div className="flex items-center justify-center gap-3 mt-6">
              <button
                onClick={this.handleReset}
                className="flex items-center gap-1.5 px-4 py-2 bg-[var(--color-accent)] hover:bg-[var(--color-accent-hover)] text-white text-xs font-bold rounded-xl transition-colors cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Try Again
              </button>
              <a
                href="/"
                className="flex items-center gap-1.5 px-4 py-2 bg-[var(--color-surface-tertiary)] hover:bg-[var(--color-surface-hover)] text-[var(--color-text-primary)] text-xs font-bold rounded-xl transition-colors"
              >
                <Home className="w-3.5 h-3.5" />
                Dashboard
              </a>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
