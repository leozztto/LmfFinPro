import { useState } from 'react'
import { ExpandableText, IconButton } from '@/shared/ui'
import { CheckCircleIcon, ChevronDownIcon, ClockIcon, TrashIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import type { Transaction } from '../types'

interface TransactionCardProps {
  transaction: Transaction
  accountName: string
  categoryName?: string
  clientName?: string
  onDelete: () => void
  isDeleting: boolean
  onToggleStatus: () => void
  isTogglingStatus: boolean
}

const NEUTRAL_BADGE_CLASS =
  'rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200'
const PENDING_BADGE_CLASS =
  'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-500/15 dark:text-amber-300'

export function TransactionCard({
  transaction,
  accountName,
  categoryName,
  clientName,
  onDelete,
  isDeleting,
  onToggleStatus,
  isTogglingStatus,
}: TransactionCardProps) {
  const [open, setOpen] = useState(false)
  const isTransfer = transaction.transferId != null
  const isPending = transaction.status === 'PENDING'
  const isIncome = transaction.type === 'INCOME'
  const amountClassName = isIncome ? 'font-semibold text-[#5ab482]' : 'font-semibold text-[#f06464]'
  const amountLabel = `${isIncome ? '+' : '-'} ${formatCurrency(transaction.amount)}`
  const metaLine = `${formatDateOnlyBr(transaction.transactionDate)} · ${accountName}${
    categoryName ? ` · ${categoryName}` : ''
  }${clientName ? ` · ${clientName}` : ''}`

  const badges = (
    <>
      {isPending && <span className={PENDING_BADGE_CLASS}>{isIncome ? 'A receber' : 'A pagar'}</span>}
      {isTransfer && <span className={NEUTRAL_BADGE_CLASS}>Transferência</span>}
      {transaction.origin === 'RECURRING' && <span className={NEUTRAL_BADGE_CLASS}>Recorrente</span>}
    </>
  )

  // Transferência é sempre paga: não há o que alternar.
  const statusLabel = isPending ? `Marcar como ${isIncome ? 'recebida' : 'paga'}` : 'Marcar como pendente'
  const statusButton = !isTransfer && (
    <IconButton
      icon={isPending ? CheckCircleIcon : ClockIcon}
      label={statusLabel}
      onClick={onToggleStatus}
      disabled={isTogglingStatus}
    />
  )

  const removeButton = (
    <IconButton
      icon={TrashIcon}
      label="Remover"
      onClick={onDelete}
      disabled={isDeleting || isTransfer}
      title={
        isTransfer
          ? 'Esta transação faz parte de uma transferência. Exclua-a na tela de Transferências.'
          : 'Remover'
      }
    />
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
          {isPending && (
            <ClockIcon className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" aria-label="Pendente" role="img" />
          )}
          <span className={`shrink-0 ${amountClassName}`}>{amountLabel}</span>
        </div>
        {open && (
          <div className="mt-3 space-y-2 border-t border-zinc-200 pt-3 dark:border-zinc-800">
            <div className="flex flex-wrap items-center gap-2 empty:hidden">{badges}</div>
            <p className="break-words text-sm text-zinc-800 dark:text-zinc-100">{transaction.description}</p>
            <div className="flex items-end justify-between gap-2">
              <p className="text-sm text-zinc-500 dark:text-zinc-400">{metaLine}</p>
              <div className="flex shrink-0 items-center gap-1">
                {statusButton}
                {removeButton}
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="hidden items-end justify-between gap-3 sm:flex">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <ExpandableText text={transaction.description} className="font-medium text-zinc-800 dark:text-zinc-100" />
            {badges}
          </div>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">{metaLine}</p>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <span className={amountClassName}>{amountLabel}</span>
          <div className="flex items-center gap-1">
            {statusButton}
            {removeButton}
          </div>
        </div>
      </div>
    </div>
  )
}
