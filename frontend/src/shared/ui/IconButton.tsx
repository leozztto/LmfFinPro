import type { ButtonHTMLAttributes, ComponentType, SVGProps } from 'react'
import { Button } from './Button'

interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: ComponentType<SVGProps<SVGSVGElement>>
  label: string
}

export function IconButton({ icon: Icon, label, className = '', ...props }: IconButtonProps) {
  return (
    <Button variant="secondary" aria-label={label} title={label} className={`px-1 ${className}`} {...props}>
      <Icon className="h-3.5 w-3.5" />
    </Button>
  )
}
