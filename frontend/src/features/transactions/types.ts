export type TransactionType = 'INCOME' | 'EXPENSE'
export type TransactionOrigin = 'MANUAL' | 'IMPORTED' | 'RECURRING'

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
}

export interface TransactionInput {
  accountId: number
  categoryId?: number
  clientId?: number
  description: string
  amount: number
  transactionDate: string
  type: TransactionType
}

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
}
