export type TransactionType = 'INCOME' | 'EXPENSE'
export type TransactionOrigin = 'MANUAL' | 'IMPORTED' | 'RECURRING'
export type TransactionStatus = 'PAID' | 'PENDING'

export interface Transaction {
  id: number
  accountId: number
  categoryId: number | null
  clientId: number | null
  description: string
  amount: number
  transactionDate: string
  type: TransactionType
  origin: TransactionOrigin
  createdAt: string
  transferId: number | null
  importBatchId: number | null
  recurringTransactionId: number | null
  status: TransactionStatus
}

export interface TransactionInput {
  accountId: number
  categoryId?: number
  clientId?: number
  description: string
  amount: number
  transactionDate: string
  type: TransactionType
  /** Ausente: o backend decide pela data (futura = pendente). */
  status?: TransactionStatus
}

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
}

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  PAID: 'Paga',
  PENDING: 'Pendente',
}
