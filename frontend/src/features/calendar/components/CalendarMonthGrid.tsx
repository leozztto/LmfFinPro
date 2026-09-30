import type { CalendarDay, CalendarEntry } from '../types'
import { WEEKDAY_LABELS, buildMonthGrid } from '../utils'

const MAX_MARKERS = 4

const compactFormatter = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 })

function formatCompact(value: number): string {
  return compactFormatter.format(value)
}

interface CalendarMonthGridProps {
  month: string
  today: string
  days: CalendarDay[]
  selectedDate: string
  onSelectDate: (date: string) => void
}

/** Cor do marcador: atrasada em vermelho, prevista em azul, paga apagada; senão receita/despesa. */
function markerClassName(entry: CalendarEntry): string {
  if (entry.status === 'OVERDUE') return 'bg-red-500'
  if (entry.status === 'FORECAST') return 'bg-sky-500'
  if (entry.status === 'PAID') return 'bg-zinc-400 dark:bg-zinc-500'
  return entry.type === 'INCOME' ? 'bg-[#5ab482]' : 'bg-[#f06464]'
}

export function CalendarMonthGrid({ month, today, days, selectedDate, onSelectDate }: CalendarMonthGridProps) {
  const weeks = buildMonthGrid(month)
  const dayByDate = new Map(days.map((day) => [day.date, day]))

  return (
    <div className="overflow-hidden rounded-xl border border-zinc-200 bg-zinc-50 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <div className="grid grid-cols-7 border-b border-zinc-200 dark:border-zinc-700">
        {WEEKDAY_LABELS.map((label) => (
          <div
            key={label}
            className="py-2 text-center text-[11px] font-semibold uppercase tracking-wide text-zinc-500 dark:text-zinc-400"
          >
            {label}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7">
        {weeks.flat().map((date, index) => {
          if (!date) {
            return (
              <div
                key={`empty-${index}`}
                className="min-h-12 border-b border-r border-zinc-100 bg-zinc-100/50 sm:min-h-20 dark:border-zinc-700/60 dark:bg-zinc-900/30 [&:nth-child(7n)]:border-r-0"
              />
            )
          }

          const day = dayByDate.get(date)
          const entries = day?.entries ?? []
          const isToday = date === today
          const isSelected = date === selectedDate
          const hiddenCount = entries.length - MAX_MARKERS
          const dayNumber = Number(date.slice(8))
          const label = `${dayNumber}${entries.length > 0 ? `, ${entries.length} lançamento(s)` : ''}`

          return (
            <button
              key={date}
              type="button"
              onClick={() => onSelectDate(date)}
              aria-label={label}
              aria-pressed={isSelected}
              className={`flex min-h-12 min-w-0 flex-col items-stretch gap-0.5 border-b border-r border-zinc-100 p-0.5 text-left transition-colors sm:min-h-20 sm:p-1 dark:border-zinc-700/60 [&:nth-child(7n)]:border-r-0 ${
                isSelected
                  ? 'bg-[#2ad6a5]/10 dark:bg-[#2ad6a5]/10'
                  : 'hover:bg-zinc-100 dark:hover:bg-zinc-700/40'
              }`}
            >
              <span
                className={`inline-flex h-6 w-6 items-center justify-center self-center rounded-full text-xs font-medium sm:self-start ${
                  isToday
                    ? 'bg-[#2ad6a5] text-zinc-900'
                    : isSelected
                      ? 'text-[#1ea883] dark:text-[#2ad6a5]'
                      : 'text-zinc-700 dark:text-zinc-200'
                }`}
              >
                {dayNumber}
              </span>

              {/* Um marcador colorido por lançamento (situação ou tipo). */}
              {entries.length > 0 && (
                <span className="flex flex-wrap justify-center gap-0.5 sm:justify-start" aria-hidden="true">
                  {entries.slice(0, MAX_MARKERS).map((entry, entryIndex) => (
                    <span key={entryIndex} className={`h-1.5 w-1.5 rounded-full ${markerClassName(entry)}`} />
                  ))}
                  {hiddenCount > 0 && (
                    <span className="hidden text-[10px] leading-none text-zinc-500 sm:inline dark:text-zinc-400">
                      +{hiddenCount}
                    </span>
                  )}
                </span>
              )}

              {/* Telas maiores: totais do dia, sem centavos, para caber na célula. */}
              {day && (
                <span className="hidden min-w-0 flex-col text-[11px] font-medium leading-tight sm:flex" aria-hidden="true">
                  {day.income > 0 && <span className="truncate text-[#5ab482]">+{formatCompact(day.income)}</span>}
                  {day.expense > 0 && <span className="truncate text-[#f06464]">-{formatCompact(day.expense)}</span>}
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
