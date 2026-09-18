import { type InputHTMLAttributes, forwardRef } from 'react'

export const Checkbox = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Checkbox({ className = '', ...props }, ref) {
    return (
      <input
        ref={ref}
        type="checkbox"
        className={`h-4 w-4 rounded border-zinc-300 text-primary-600 focus:ring-2 focus:ring-primary-100 dark:border-zinc-700 dark:bg-zinc-900 ${className}`}
        {...props}
      />
    )
  },
)
