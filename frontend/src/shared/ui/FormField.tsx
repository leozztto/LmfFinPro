import type { ReactNode } from 'react'
import { Label } from './Label'
import { ErrorText } from './ErrorText'

interface FormFieldProps {
  label: string
  htmlFor: string
  error?: string
  hint?: string
  children: ReactNode
}

export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {!error && hint && <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">{hint}</p>}
      <ErrorText>{error}</ErrorText>
    </div>
  )
}
