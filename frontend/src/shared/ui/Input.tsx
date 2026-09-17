import { type InputHTMLAttributes, forwardRef } from 'react'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className = '', ...props }, ref) {
    return (
      <input
        ref={ref}
        className={`w-full rounded-lg border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 placeholder:text-zinc-400 focus:border-primary-500 focus:bg-white focus:outline focus:outline-2 focus:outline-primary-100 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-50 dark:focus:bg-zinc-800 ${className}`}
        {...props}
      />
    )
  },
)
