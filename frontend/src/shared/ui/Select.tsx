import { type SelectHTMLAttributes, forwardRef } from 'react'

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  function Select({ className = '', children, ...props }, ref) {
    return (
      <select
        ref={ref}
        className={`w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary-500 focus:outline focus:outline-2 focus:outline-primary-100 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-50 ${className}`}
        {...props}
      >
        {children}
      </select>
    )
  },
)
