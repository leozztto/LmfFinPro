import { Link } from 'react-router-dom'
import { IconButton } from '@/shared/ui'
import { CheckCircleIcon } from '@/shared/ui/icons'
import { formatCurrency } from '@/shared/format/currency'
import { formatDateOnlyBr } from '@/shared/format/date'
import type { CalendarEntry, CalendarEntryStatus } from '../types'
import { entryStatusLabel, entryTitle } from '../utils'

const STATUS_BADGE_CLASSES: Record<CalendarEntryStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300',
  OVERDUE: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
  FORECAST: 'bg-sky-100 text-sky-800 dark:bg-sky-500/15 dark:text-sky-300',
  PAID: 'bg-zinc-200 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-300',
}

interface CalendarEntryItemProps {
  entry: CalendarEntry
  /** Mostra a data na linha de detalhes (lista de atrasados, que mistura dias). */
  showDate?: boolean
  onMarkAsPaid: (entry: CalendarEntry) => void
  isMarkingAsPaid: boolean
}

export function CalendarEntryItem({ entry, showDate = false, onMarkAsPaid, isMarkingAsPaid }: CalendarEntryItemProps) {
  const isIncome = entry.type === 'INCOME'
  const canMarkAsPaid = entry.kind === 'TRANSACTION' && (entry.status === 'PENDING' || entry.status === 'OVERDUE')
  const details = [
    showDate ? formatDateOnlyBr(entry.date) : null,
    entry.accountName,
    entry.categoryName,
    entry.clientName,
  ].filter(Boolean)

  return (
    <li className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
      <div className="min-w-0 flex-1">
        <p className="break-words text-sm font-medium text-zinc-800 dark:text-zinc-100">{entryTitle(entry)}</p>
        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400">
          <span className={`rounded-full px-2 py-0.5 font-medium ${STATUS_BADGE_CLASSES[entry.status]}`}>
            {entryStatusLabel(entry)}
          </span>
          {details.length > 0 && <span className="min-w-0 break-words">{details.join(' · ')}</span>}
          {entry.kind === 'RECURRING_FORECAST' && (
            <Link to="/recorrencias" className="font-medium text-[#1ea883] hover:underline dark:text-[#2ad6a5]">
              Ver recorrência
            </Link>
          )}
          {entry.kind === 'DAS' && entry.amount == null && (
            <Link to="/impostos" className="font-medium text-[#1ea883] hover:underline dark:text-[#2ad6a5]">
              Estimar imposto
            </Link>
          )}
        </div>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <span
          className={`text-sm font-semibold ${
            entry.amount == null ? 'text-zinc-400' : isIncome ? 'text-[#5ab482]' : 'text-[#f06464]'
          }`}
        >
          {entry.amount == null ? '—' : `${isIncome ? '+' : '-'} ${formatCurrency(entry.amount)}`}
        </span>
        {canMarkAsPaid && (
          <IconButton
            icon={CheckCircleIcon}
            label={`Marcar como ${isIncome ? 'recebida' : 'paga'}`}
            onClick={() => onMarkAsPaid(entry)}
            disabled={isMarkingAsPaid}
          />
        )}
      </div>
    </li>
  )
}
