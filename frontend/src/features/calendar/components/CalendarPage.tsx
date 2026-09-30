import { useState } from 'react'
import { Button, Card, Checkbox, StatCard } from '@/shared/ui'
import { AlertTriangleIcon, ChevronDownIcon, ChevronLeftIcon, ChevronRightIcon } from '@/shared/ui/icons'
import { ApiError } from '@/shared/api/httpClient'
import { useToast } from '@/shared/toast/ToastContext'
import { useConfirm } from '@/shared/confirm/ConfirmContext'
import { formatCurrency } from '@/shared/format/currency'
import { getCurrentIsoDate, getCurrentYearMonth } from '@/shared/format/date'
import { useUpdateTransactionStatus } from '@/features/transactions/hooks/useUpdateTransactionStatus'
import { useCalendar } from '../hooks/useCalendar'
import type { Calendar, CalendarEntry } from '../types'
import { formatDayLabel, formatMonthLabel, shiftMonth } from '../utils'
import { CalendarEntryItem } from './CalendarEntryItem'
import { CalendarMonthGrid } from './CalendarMonthGrid'

/** Dia aberto ao chegar num mês: hoje, se for o mês atual; senão o primeiro dia com lançamento. */
function defaultSelectedDate(calendar: Calendar | undefined, month: string): string {
  if (calendar?.today.startsWith(month)) return calendar.today
  return calendar?.days[0]?.date ?? `${month}-01`
}

export function CalendarPage() {
  const [month, setMonth] = useState(getCurrentYearMonth)
  const [includePaid, setIncludePaid] = useState(false)
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [showOverdue, setShowOverdue] = useState(false)
  const { data: calendar, isError } = useCalendar(month, includePaid)
  // Sem dados e sem erro: primeira carga do mês (ou nova tentativa depois de uma falha).
  const isWaiting = !calendar && !isError
  const updateTransactionStatus = useUpdateTransactionStatus()
  const { showToast } = useToast()
  const confirm = useConfirm()

  const currentMonth = calendar?.today.slice(0, 7) ?? getCurrentYearMonth()
  const today = calendar?.today ?? getCurrentIsoDate()
  const activeDate =
    selectedDate && selectedDate.startsWith(month) ? selectedDate : defaultSelectedDate(calendar, month)
  const selectedDay = calendar?.days.find((day) => day.date === activeDate)

  function goToMonth(nextMonth: string) {
    setMonth(nextMonth)
    setSelectedDate(null)
  }

  /** Marcar como paga (ou recebida) é definitivo — o backend recusa a volta para pendente —, por isso confirma antes. */
  async function handleMarkAsPaid(entry: CalendarEntry) {
    if (entry.transactionId == null) return
    const paidWord = entry.type === 'INCOME' ? 'recebida' : 'paga'
    const confirmed = await confirm({
      title: `Marcar como ${paidWord}`,
      message: `Confirmar que "${entry.description}" foi ${paidWord}? Depois de confirmada, a transação não poderá voltar para pendente.`,
      confirmLabel: `Marcar como ${paidWord}`,
      variant: 'brand',
    })
    if (!confirmed) return

    updateTransactionStatus.mutate(
      { id: entry.transactionId, status: 'PAID' },
      {
        onSuccess: () => showToast(`Transação marcada como ${paidWord}.`, 'success'),
        onError: (error) =>
          showToast(error instanceof ApiError ? error.message : 'Não foi possível alterar a situação da transação.'),
      },
    )
  }

  const overdue = calendar?.overdue ?? []

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Calendário</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Lançamentos futuros, vencimentos e o que está em atraso, dia a dia.
        </p>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="secondary"
            className="px-2"
            onClick={() => goToMonth(shiftMonth(month, -1))}
            aria-label="Mês anterior"
            title="Mês anterior"
          >
            <ChevronLeftIcon />
          </Button>
          <p className="min-w-[8.5rem] text-center sm:min-w-[10.5rem] text-sm font-semibold text-zinc-800 dark:text-zinc-100" aria-live="polite">
            {formatMonthLabel(month)}
          </p>
          <Button
            variant="secondary"
            className="px-2"
            onClick={() => goToMonth(shiftMonth(month, 1))}
            aria-label="Próximo mês"
            title="Próximo mês"
          >
            <ChevronRightIcon />
          </Button>
          {month !== currentMonth && (
            <Button variant="secondary" className="px-3" onClick={() => goToMonth(currentMonth)}>
              Hoje
            </Button>
          )}
        </div>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-zinc-600 dark:text-zinc-300">
          <Checkbox checked={includePaid} onChange={(event) => setIncludePaid(event.target.checked)} />
          Mostrar pagas e recebidas
        </label>
      </div>

      {isError && (
        <Card>
          <p className="text-sm text-red-600 dark:text-red-400">Não foi possível carregar o calendário.</p>
        </Card>
      )}

      {overdue.length > 0 && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-500/30 dark:bg-red-500/10">
          <button
            type="button"
            onClick={() => setShowOverdue((value) => !value)}
            aria-expanded={showOverdue}
            className="flex w-full items-start gap-3 text-left"
          >
            <AlertTriangleIcon className="mt-0.5 h-5 w-5 shrink-0 text-red-600 dark:text-red-400" />
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-red-800 dark:text-red-200">
                {overdue.length === 1 ? '1 lançamento em atraso' : `${overdue.length} lançamentos em atraso`}
              </span>
              <span className="block text-xs text-red-700 dark:text-red-300">
                {[
                  calendar && calendar.overdueExpense > 0 ? `A pagar ${formatCurrency(calendar.overdueExpense)}` : null,
                  calendar && calendar.overdueIncome > 0 ? `A receber ${formatCurrency(calendar.overdueIncome)}` : null,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            </span>
            <ChevronDownIcon
              className={`mt-0.5 h-4 w-4 shrink-0 text-red-600 transition-transform dark:text-red-400 ${showOverdue ? 'rotate-180' : ''}`}
            />
          </button>
          {showOverdue && (
            <ul className="mt-3 divide-y divide-red-200 rounded-lg bg-white/70 px-3 py-3 dark:divide-red-500/20 dark:bg-zinc-900/40">
              {overdue.map((entry) => (
                <CalendarEntryItem
                  key={entry.transactionId}
                  entry={entry}
                  showDate
                  onMarkAsPaid={handleMarkAsPaid}
                  isMarkingAsPaid={updateTransactionStatus.isPending}
                />
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard
          label="A receber no mês"
          value={calendar ? formatCurrency(calendar.expectedIncome) : '—'}
          hint="Pendentes, atrasadas e previstas"
        />
        <StatCard
          label="A pagar no mês"
          value={calendar ? formatCurrency(calendar.expectedExpense) : '—'}
          hint="Inclui recorrências e DAS"
        />
        <StatCard label="Já recebido" value={calendar ? formatCurrency(calendar.paidIncome) : '—'} />
        <StatCard label="Já pago" value={calendar ? formatCurrency(calendar.paidExpense) : '—'} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="min-w-0">
          {isWaiting ? (
            <div className="h-80 animate-pulse rounded-xl bg-zinc-100 dark:bg-zinc-800" />
          ) : (
            <CalendarMonthGrid
              month={month}
              today={today}
              days={calendar?.days ?? []}
              selectedDate={activeDate}
              onSelectDate={setSelectedDate}
            />
          )}
          <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400">
            <LegendItem className="bg-[#5ab482]" label="A receber" />
            <LegendItem className="bg-[#f06464]" label="A pagar" />
            <LegendItem className="bg-sky-500" label="Prevista / DAS" />
            <LegendItem className="bg-red-500" label="Atrasada" />
            {includePaid && <LegendItem className="bg-zinc-400" label="Paga / recebida" />}
          </div>
        </div>

        <Card className="h-fit">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h3 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">{formatDayLabel(activeDate)}</h3>
            {selectedDay && (
              <p className="text-xs font-medium">
                {selectedDay.income > 0 && <span className="text-[#5ab482]">+ {formatCurrency(selectedDay.income)}</span>}
                {selectedDay.income > 0 && selectedDay.expense > 0 && <span className="text-zinc-400"> · </span>}
                {selectedDay.expense > 0 && <span className="text-[#f06464]">- {formatCurrency(selectedDay.expense)}</span>}
              </p>
            )}
          </div>
          {selectedDay ? (
            <ul className="mt-4 divide-y divide-zinc-200 dark:divide-zinc-700">
              {selectedDay.entries.map((entry, index) => (
                <CalendarEntryItem
                  key={`${entry.kind}-${entry.transactionId ?? entry.recurringTransactionId ?? 'das'}-${index}`}
                  entry={entry}
                  onMarkAsPaid={handleMarkAsPaid}
                  isMarkingAsPaid={updateTransactionStatus.isPending}
                />
              ))}
            </ul>
          ) : (
            <p className="mt-4 text-sm text-zinc-500 dark:text-zinc-400">
              {isWaiting ? 'Carregando…' : 'Nada previsto para este dia.'}
            </p>
          )}
        </Card>
      </div>
    </div>
  )
}

function LegendItem({ className, label }: { className: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`h-2 w-2 rounded-full ${className}`} />
      {label}
    </span>
  )
}
