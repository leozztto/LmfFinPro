import type { ReactNode } from 'react'
import { Label } from './Label'
import { ErrorText } from './ErrorText'

interface FormFieldProps {
  label: string
  htmlFor: string
  error?: string
  children: ReactNode
}

export function FormField({ label, htmlFor, error, children }: FormFieldProps) {
  return (
    <div>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      <ErrorText>{error}</ErrorText>
    </div>
  )
}
