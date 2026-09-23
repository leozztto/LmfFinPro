import type { ReactNode } from 'react'
import { ChevronDownIcon } from './icons'

interface AccordionItemProps {
  summary: ReactNode
  children: ReactNode
  open?: boolean
}

export function AccordionItem({ summary, children, open }: AccordionItemProps) {
  return (
    <details className="group border-b border-zinc-200 py-3 last:border-0 dark:border-zinc-700" open={open}>
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-medium text-zinc-800 marker:content-none dark:text-zinc-100">
        {summary}
        <ChevronDownIcon className="h-4 w-4 shrink-0 text-zinc-400 transition-transform group-open:rotate-180" />
      </summary>
      <div className="mt-2 text-sm leading-relaxed text-zinc-600 dark:text-zinc-400">{children}</div>
    </details>
  )
}
