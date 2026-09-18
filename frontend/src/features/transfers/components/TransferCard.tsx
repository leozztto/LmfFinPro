import { useState } from 'react'
import { Button, Card, ExpandableText } from '@/shared/ui'
import { ChevronDownIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import type { Transfer } from '../types'

interface TransferCardProps {
  transfer: Transfer
  fromAccountName: string
  toAccountName: string
  onDelete: () => void
  isDeleting: boolean
}

export function TransferCard({ transfer, fromAccountName, toAccountName, onDelete, isDeleting }: TransferCardProps) {
  const [open, setOpen] = useState(false)
  const title = `${fromAccountName} → ${toAccountName}`
  const amountLabel = formatCurrency(transfer.amount)

  const removeButton = (
    <Button variant="secondary" onClick={onDelete} disabled={isDeleting}>
      Remover
    </Button>
  )

  return (
    <Card>
      <div className="sm:hidden">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            aria-label={open ? 'Ver menos detalhes' : 'Ver detalhes completos'}
            className="shrink-0 rounded-lg p-1 text-zinc-500 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-700"
          >
            <ChevronDownIcon className={`h-4 w-4 transition-transform ${open ? 'rotate-180' : ''}`} />
          </button>
          <p className="min-w-0 flex-1 truncate font-medium text-zinc-900 dark:text-zinc-50">{title}</p>
          <span className="shrink-0 font-semibold text-zinc-900 dark:text-zinc-50">{amountLabel}</span>
        </div>
        {open && (
          <div className="mt-3 space-y-2 border-t border-zinc-200 pt-3 dark:border-zinc-700">
            <p className="break-words text-sm text-zinc-900 dark:text-zinc-50">{title}</p>
            <p className="text-sm text-zinc-500 dark:text-zinc-400">{formatDateOnlyBr(transfer.transferDate)}</p>
            {transfer.description && (
              <p className="break-words text-sm text-zinc-500 dark:text-zinc-400">{transfer.description}</p>
            )}
            {removeButton}
          </div>
        )}
      </div>

      <div className="hidden items-center justify-between gap-3 sm:flex">
        <div className="min-w-0 flex-1">
          <p className="break-words font-medium text-zinc-900 dark:text-zinc-50">{title}</p>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">{formatDateOnlyBr(transfer.transferDate)}</p>
          {transfer.description && (
            <ExpandableText text={transfer.description} className="text-sm text-zinc-500 dark:text-zinc-400" />
          )}
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <span className="font-semibold text-zinc-900 dark:text-zinc-50">{amountLabel}</span>
          {removeButton}
        </div>
      </div>
    </Card>
  )
}
