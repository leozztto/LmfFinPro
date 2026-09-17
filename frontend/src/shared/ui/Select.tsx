import { type SelectHTMLAttributes, forwardRef } from 'react'

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  function Select({ className = '', children, ...props }, ref) {
    return (
      <select
        ref={ref}
        className={`w-full rounded-lg border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 focus:border-primary-500 focus:bg-white focus:outline focus:outline-2 focus:outline-primary-100 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-50 dark:focus:bg-zinc-800 ${className}`}
        {...props}
      >
        {children}
      </select>
    )
  },
)
