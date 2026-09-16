import { httpClient } from '@/shared/api/httpClient'
import type { Transaction, TransactionInput } from '../types'

export const transactionsApi = {
  list: () => httpClient.get<Transaction[]>('/transactions'),
  create: (input: TransactionInput) => httpClient.post<Transaction, TransactionInput>('/transactions', input),
  remove: (id: number) => httpClient.delete(`/transactions/${id}`),
}
