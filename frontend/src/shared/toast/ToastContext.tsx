import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

type ToastVariant = 'error' | 'success'

interface Toast {
  id: number
  message: string
  variant: ToastVariant
  leaving: boolean
}

interface ToastContextValue {
  showToast: (message: string, variant?: ToastVariant) => void
}

const TOAST_DURATION_MS = 4000
const TOAST_EXIT_MS = 250

const ToastContext = createContext<ToastContextValue | undefined>(undefined)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.map((toast) => (toast.id === id ? { ...toast, leaving: true } : toast)))
    setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, TOAST_EXIT_MS)
  }, [])

  const showToast = useCallback(
    (message: string, variant: ToastVariant = 'error') => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, message, variant, leaving: false }])
      setTimeout(() => dismiss(id), TOAST_DURATION_MS)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-[100] flex flex-col items-center gap-2 px-4">
        {toasts.map((toast) => (
          <button
            key={toast.id}
            type="button"
            onClick={() => dismiss(toast.id)}
            className={`pointer-events-auto w-full max-w-sm rounded-lg border px-4 py-3 text-left text-sm font-medium shadow-lg ${
              toast.leaving ? 'animate-toast-out' : 'animate-toast-in'
            } ${
              toast.variant === 'error'
                ? 'border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200'
                : 'border-primary-200 bg-primary-50 text-primary-700 dark:border-primary-600 dark:bg-zinc-800 dark:text-primary-200'
            }`}
          >
            {toast.message}
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast deve ser usado dentro de um ToastProvider')
  }
  return context
}
