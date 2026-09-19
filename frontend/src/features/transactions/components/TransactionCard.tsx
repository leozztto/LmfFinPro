import { useState } from 'react'
import { Button, ExpandableText } from '@/shared/ui'
import { ChevronDownIcon, TrashIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import type { Transaction } from '../types'

interface TransactionCardProps {
  transaction: Transaction
  accountName: string
  categoryName?: string
  onDelete: () => void
  isDeleting: boolean
}

export function TransactionCard({ transaction, accountName, categoryName, onDelete, isDeleting }: TransactionCardProps) {
  const [open, setOpen] = useState(false)
  const isTransfer = transaction.transferId != null
  const amountClassName = transaction.type === 'INCOME' ? 'font-semibold text-[#5ab482]' : 'font-semibold text-[#f06464]'
  const amountLabel = `${transaction.type === 'INCOME' ? '+' : '-'} ${formatCurrency(transaction.amount)}`
  const metaLine = `${formatDateOnlyBr(transaction.transactionDate)} · ${accountName}${
    categoryName ? ` · ${categoryName}` : ''
  }`

  const removeButton = (
    <Button
      variant="secondary"
      onClick={onDelete}
      disabled={isDeleting || isTransfer}
      aria-label="Remover"
      title={
        isTransfer
          ? 'Esta transação faz parte de uma transferência. Exclua-a na tela de Transferências.'
          : 'Remover'
      }
      className="px-3"
    >
      <TrashIcon />
    </Button>
  )

  return (
    <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-4 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
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
          <p className="min-w-0 flex-1 truncate font-medium text-zinc-800 dark:text-zinc-100">
            {transaction.description}
          </p>
          <span className={`shrink-0 ${amountClassName}`}>{amountLabel}</span>
        </div>
        {open && (
          <div className="mt-3 space-y-2 border-t border-zinc-200 pt-3 dark:border-zinc-800">
            {isTransfer && (
              <span className="inline-block rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200">
                Transferência
              </span>
            )}
            <p className="break-words text-sm text-zinc-800 dark:text-zinc-100">{transaction.description}</p>
            <div className="flex items-end justify-between gap-2">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">{metaLine}</p>
              {removeButton}
            </div>
          </div>
        )}
      </div>

      <div className="hidden items-end justify-between gap-3 sm:flex">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <ExpandableText text={transaction.description} className="font-medium text-zinc-800 dark:text-zinc-100" />
            {isTransfer && (
              <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200">
                Transferência
              </span>
            )}
          </div>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">{metaLine}</p>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <span className={amountClassName}>{amountLabel}</span>
          {removeButton}
        </div>
      </div>
    </div>
  )
}
