import type { ReactNode } from 'react'
import { Label } from './Label'
import { ErrorText } from './ErrorText'

interface FormFieldProps {
  label: string
  htmlFor: string
  error?: string
  /** Texto de apoio abaixo do campo; some quando há erro, para não empilhar mensagens. */
  hint?: ReactNode
  children: ReactNode
}

export function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
  return (
    <div>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {hint && !error && <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">{hint}</p>}
      <ErrorText>{error}</ErrorText>
    </div>
  )
}
