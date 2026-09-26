import type { TransactionType } from '@/features/transactions/types'

export type RecurrenceFrequency = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface RecurringTransaction {
  id: number
  accountId: number
  categoryId: number | null
  clientId: number | null
  description: string
  amount: number
  type: TransactionType
  frequency: RecurrenceFrequency
  startDate: string
  endDate: string | null
  generatedOccurrences: number
  active: boolean
  /** Calculada no backend; null quando pausada ou encerrada. */
  nextOccurrenceDate: string | null
  createdAt: string
}

export interface RecurringTransactionInput {
  accountId: number
  categoryId?: number
  clientId?: number
  description: string
  amount: number
  type: TransactionType
  frequency: RecurrenceFrequency
  startDate: string
  endDate?: string
}

export interface RecurringTransactionUpdateInput {
  categoryId?: number
  clientId?: number
  description: string
  amount: number
  endDate?: string
  active: boolean
}

export const RECURRENCE_FREQUENCY_LABELS: Record<RecurrenceFrequency, string> = {
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensal',
  YEARLY: 'Anual',
}
