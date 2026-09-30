import { useState } from 'react'
import { ExpandableText, IconButton } from '@/shared/ui'
import { CheckCircleIcon, ChevronDownIcon, ClockIcon, PaperclipIcon, TagIcon, TrashIcon } from '@/shared/ui/icons'
import { TagBadge } from '@/features/tags/components/TagBadge'
import { formatCurrency, type Currency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import type { Transaction } from '../types'

interface TransactionCardProps {
  transaction: Transaction
  accountName: string
  /** Moeda da conta, em que está o `amount`. */
  accountCurrency?: Currency
  categoryName?: string
  clientName?: string
  onDelete: () => void
  isDeleting: boolean
  onMarkAsPaid: () => void
  isMarkingAsPaid: boolean
  onOpenAttachments: () => void
  onEditTags: () => void
}

const NEUTRAL_BADGE_CLASS =
  'rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700 dark:bg-zinc-700 dark:text-zinc-200'
const PENDING_BADGE_CLASS =
  'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-500/15 dark:text-amber-300'

export function TransactionCard({
  transaction,
  accountName,
  accountCurrency = 'BRL',
  categoryName,
  clientName,
  onDelete,
  isDeleting,
  onMarkAsPaid,
  isMarkingAsPaid,
  onOpenAttachments,
  onEditTags,
}: TransactionCardProps) {
  const [open, setOpen] = useState(false)
  const isTransfer = transaction.transferId != null
  const isPending = transaction.status === 'PENDING'
  const isIncome = transaction.type === 'INCOME'
  const amountClassName = isIncome ? 'font-semibold text-[#5ab482]' : 'font-semibold text-[#f06464]'
  const amountLabel = `${isIncome ? '+' : '-'} ${formatCurrency(transaction.amount, accountCurrency)}`
  // Fora do real: a operação na moeda original e/ou o equivalente em reais que entra nos totais.
  const currencyNotes = [
    transaction.originalCurrency && transaction.originalAmount != null
      ? formatCurrency(transaction.originalAmount, transaction.originalCurrency)
      : null,
    accountCurrency !== 'BRL' ? `≈ ${formatCurrency(transaction.baseAmount)}` : null,
  ].filter(Boolean)
  const timeLabel = transaction.transactionTime ? ` ${transaction.transactionTime.slice(0, 5)}` : ''
  const metaLine = `${formatDateOnlyBr(transaction.transactionDate)}${timeLabel} · ${accountName}${
    categoryName ? ` · ${categoryName}` : ''
  }${clientName ? ` · ${clientName}` : ''}${currencyNotes.length ? ` · ${currencyNotes.join(' · ')}` : ''}`

  const badges = (
    <>
      {isPending && <span className={PENDING_BADGE_CLASS}>{isIncome ? 'A receber' : 'A pagar'}</span>}
      {isTransfer && <span className={NEUTRAL_BADGE_CLASS}>Transferência</span>}
      {transaction.origin === 'RECURRING' && <span className={NEUTRAL_BADGE_CLASS}>Recorrente</span>}
      {(transaction.tags ?? []).map((tag) => (
        <TagBadge key={tag.id} name={tag.name} color={tag.color} />
      ))}
    </>
  )

  // Tag é classificação: dá para mudar em qualquer transação, inclusive paga ou de transferência.
  const tagsButton = <IconButton icon={TagIcon} label="Editar tags" onClick={onEditTags} />

  // Só transação pendente tem o botão: marcar como paga é definitivo (não volta a pendente) e
  // transferência já nasce paga.
  const statusButton = isPending && !isTransfer && (
    <IconButton
      icon={CheckCircleIcon}
      label={`Marcar como ${isIncome ? 'recebida' : 'paga'}`}
      onClick={onMarkAsPaid}
      disabled={isMarkingAsPaid}
    />
  )

  const attachmentCount = transaction.attachmentCount ?? 0
  const attachmentsLabel =
    attachmentCount === 0 ? 'Anexar comprovante' : `Comprovantes (${attachmentCount})`
  // Com anexo, o botão mostra o clipe + a contagem; sem anexo, só o clipe.
  const attachmentsButton = (
    <button
      type="button"
      onClick={onOpenAttachments}
      aria-label={attachmentsLabel}
      title={attachmentsLabel}
      className={`inline-flex items-center gap-1 rounded-lg border px-1.5 py-1.5 text-xs font-medium transition-colors ${
        attachmentCount > 0
          ? 'border-[#2ad6a5]/50 bg-[#2ad6a5]/10 text-[#1ea883] dark:text-[#2ad6a5]'
          : 'border-zinc-300 bg-white text-zinc-700 hover:bg-zinc-100 dark:border-zinc-600 dark:bg-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-600'
      }`}
    >
      <PaperclipIcon className="h-3.5 w-3.5" />
      {attachmentCount > 0 && <span>{attachmentCount}</span>}
    </button>
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
          {attachmentCount > 0 && (
            <PaperclipIcon
              className="h-4 w-4 shrink-0 text-[#1ea883] dark:text-[#2ad6a5]"
              aria-label={`${attachmentCount} comprovante(s)`}
              role="img"
            />
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
                {tagsButton}
                {attachmentsButton}
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
            {tagsButton}
            {attachmentsButton}
            {statusButton}
            {removeButton}
          </div>
        </div>
      </div>
    </div>
  )
}
