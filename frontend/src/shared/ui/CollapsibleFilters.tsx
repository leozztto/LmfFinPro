import { type ReactNode, useState } from 'react'
import { ChevronDownIcon } from './icons'

interface CollapsibleFiltersProps {
  activeCount: number
  children: ReactNode
}

export function CollapsibleFilters({ activeCount, children }: CollapsibleFiltersProps) {
  const [open, setOpen] = useState(false)

  return (
    <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-3 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        className="flex w-full items-center justify-between text-sm font-medium text-zinc-800 dark:text-zinc-100"
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
      {open && <div className="mt-3">{children}</div>}
    </div>
  )
}
