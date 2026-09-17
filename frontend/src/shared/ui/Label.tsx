import type { LabelHTMLAttributes } from 'react'

export function Label({ className = '', ...props }: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={`mb-1 block text-sm font-medium text-zinc-700 dark:text-zinc-200 ${className}`}
      {...props}
    />
  )
}
