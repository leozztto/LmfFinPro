import { type ReactNode, useState } from 'react'
import { Card } from './Card'
import { ChevronDownIcon } from './icons'

interface CollapsibleFiltersProps {
  activeCount: number
  children: ReactNode
}

export function CollapsibleFilters({ activeCount, children }: CollapsibleFiltersProps) {
  const [open, setOpen] = useState(false)

  return (
    <Card>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        className="flex w-full items-center justify-between text-sm font-medium text-zinc-900 dark:text-zinc-50"
      >
        <span className="flex items-center gap-2">
          Filtros
          {activeCount > 0 && (
            <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200">
              {activeCount}
            </span>
          )}
        </span>
        <ChevronDownIcon className={`h-4 w-4 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && <div className="mt-4">{children}</div>}
    </Card>
  )
}
