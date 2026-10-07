import type { TagSummary } from '@/features/tags/types'
import type { Currency } from '@/shared/format/currency'

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
  /** Hora da transação (ex.: "14:32:00") — só presente em transações importadas com coluna de hora. */
  transactionTime: string | null
  type: TransactionType
  origin: TransactionOrigin
  createdAt: string
  transferId: number | null
  importBatchId: number | null
  recurringTransactionId: number | null
  status: TransactionStatus
  /** Moeda e valor da operação feita em outra moeda (ex.: compra em dólar no cartão em reais). */
  originalCurrency: Currency | null
  originalAmount: number | null
  /** Valor em reais — o que entra nos totais; igual a `amount` nas contas em reais. */
  baseAmount: number
  /** Quantidade de comprovantes anexados (vem na listagem). */
  attachmentCount?: number
  tags: TagSummary[]
  /** Quem criou o lançamento; nulo quando não se sabe (criado pelo sistema). Numa conta compartilhada só essa pessoa exclui. */
  createdByUserId?: number | null
  createdByName?: string | null
  /** Nome da conta quando ela está em outro espaço (perna de transferência vinda do grupo ou do pessoal). */
  linkedAccountName?: string | null
}

export interface TransactionInput {
  accountId: number
  categoryId?: number
  clientId?: number
  description: string
  /** Na moeda da conta. */
  amount: number
  originalCurrency?: Currency
  originalAmount?: number
  transactionDate: string
  type: TransactionType
  /** Ausente: o backend decide pela data (futura = pendente). */
  status?: TransactionStatus
  /** Nomes das tags; as que ainda não existem são criadas pelo backend. */
  tagNames?: string[]
}

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
}

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  PAID: 'Paga',
  PENDING: 'Pendente',
}

/** Parâmetros da listagem paginada; o que não vem preenchido não filtra. */
export interface TransactionListParams {
  /** Página atual, a partir de 0. */
  page: number
  size: number
  accountId?: number
  categoryId?: number
  clientId?: number
  type?: TransactionType
  status?: TransactionStatus
  hasAttachment?: boolean
  /** Entra a transação que tiver qualquer uma destas tags. */
  tagNames?: string[]
  startDate?: string
  endDate?: string
}
