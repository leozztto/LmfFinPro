import type { TransactionType } from '@/features/transactions/types'

export type CalendarEntryKind = 'TRANSACTION' | 'RECURRING_FORECAST' | 'DAS'
/** OVERDUE: pendente com data anterior a hoje. FORECAST: recorrência ainda não lançada ou DAS. */
export type CalendarEntryStatus = 'PAID' | 'PENDING' | 'OVERDUE' | 'FORECAST'

export interface CalendarEntry {
  kind: CalendarEntryKind
  date: string
  description: string
  /** null só no DAS sem estimativa de imposto da competência. */
  amount: number | null
  type: TransactionType
  status: CalendarEntryStatus
  transactionId: number | null
  recurringTransactionId: number | null
  accountName: string | null
  categoryName: string | null
  clientName: string | null
  /** Só no DAS ("YYYY-MM"). */
  competence: string | null
}

export interface CalendarDay {
  date: string
  income: number
  expense: number
  entries: CalendarEntry[]
}

export interface Calendar {
  /** "YYYY-MM" */
  month: string
  today: string
  /** Em aberto no mês: pendentes, atrasadas e previstas. */
  expectedIncome: number
  expectedExpense: number
  paidIncome: number
  paidExpense: number
  overdueIncome: number
  overdueExpense: number
  /** Só os dias do mês com algum lançamento. */
  days: CalendarDay[]
  /** Pendentes com data anterior a hoje, de qualquer mês. */
  overdue: CalendarEntry[]
}
