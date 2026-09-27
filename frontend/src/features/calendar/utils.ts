import type { CalendarEntry } from './types'

export const WEEKDAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'] as const

const MONTH_NAMES = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
] as const

function parseMonth(month: string): [number, number] {
  const [year, monthNumber] = month.split('-').map(Number)
  return [year, monthNumber]
}

function toIsoDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

/** "2026-09" + 1 → "2026-10"; aceita deslocamentos negativos e virada de ano. */
export function shiftMonth(month: string, delta: number): string {
  const [year, monthNumber] = parseMonth(month)
  const index = year * 12 + (monthNumber - 1) + delta
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`
}

/** "2026-09" → "Setembro de 2026". */
export function formatMonthLabel(month: string): string {
  const [year, monthNumber] = parseMonth(month)
  return `${MONTH_NAMES[monthNumber - 1]} de ${year}`
}

/**
 * Semanas do mês (domingo a sábado) com a data ISO de cada dia; `null` nas casas antes do dia 1 e
 * depois do último dia. Usa UTC só para descobrir o dia da semana, sem depender do fuso do aparelho.
 */
export function buildMonthGrid(month: string): (string | null)[][] {
  const [year, monthNumber] = parseMonth(month)
  const firstWeekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay()
  const daysInMonth = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate()

  const cells: (string | null)[] = Array.from({ length: firstWeekday }, () => null)
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push(toIsoDate(year, monthNumber, day))
  }
  while (cells.length % 7 !== 0) cells.push(null)

  const weeks: (string | null)[][] = []
  for (let start = 0; start < cells.length; start += 7) {
    weeks.push(cells.slice(start, start + 7))
  }
  return weeks
}

/** Etiqueta curta da situação, do ponto de vista de quem paga ou recebe. */
export function entryStatusLabel(entry: CalendarEntry): string {
  const isIncome = entry.type === 'INCOME'
  switch (entry.status) {
    case 'PAID':
      return isIncome ? 'Recebida' : 'Paga'
    case 'OVERDUE':
      return 'Atrasada'
    case 'FORECAST':
      return entry.kind === 'DAS' ? 'Lembrete' : 'Prevista'
    default:
      return isIncome ? 'A receber' : 'A pagar'
  }
}

/** "DAS" + competência "2026-08" → "DAS · competência 08/2026". */
export function entryTitle(entry: CalendarEntry): string {
  if (entry.kind === 'DAS' && entry.competence) {
    const [year, monthNumber] = entry.competence.split('-')
    return `${entry.description} · competência ${monthNumber}/${year}`
  }
  return entry.description
}

const WEEKDAY_NAMES = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'] as const

/** "2026-09-20" → "Domingo, 20 de setembro". */
export function formatDayLabel(isoDate: string): string {
  const [year, monthNumber, day] = isoDate.split('-').map(Number)
  const weekday = new Date(Date.UTC(year, monthNumber - 1, day)).getUTCDay()
  return `${WEEKDAY_NAMES[weekday]}, ${day} de ${MONTH_NAMES[monthNumber - 1].toLowerCase()}`
}
