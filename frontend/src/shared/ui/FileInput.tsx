import { type InputHTMLAttributes, forwardRef } from 'react'

export const FileInput = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function FileInput({ className = '', ...props }, ref) {
    return (
      <input
        ref={ref}
        type="file"
        className={`block w-full text-sm text-zinc-600 file:mr-3 file:rounded-lg file:border-0 file:bg-primary-600 file:px-3 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-primary-700 dark:text-zinc-300 ${className}`}
        {...props}
      />
    )
  },
)
