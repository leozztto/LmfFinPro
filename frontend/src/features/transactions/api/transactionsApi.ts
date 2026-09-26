import { httpClient } from '@/shared/api/httpClient'
import type { Transaction, TransactionInput, TransactionStatus } from '../types'

export const transactionsApi = {
  list: () => httpClient.get<Transaction[]>('/transactions'),
  create: (input: TransactionInput) => httpClient.post<Transaction, TransactionInput>('/transactions', input),
  updateStatus: (id: number, status: TransactionStatus) =>
    httpClient.patch<Transaction, { status: TransactionStatus }>(`/transactions/${id}/status`, { status }),
  remove: (id: number) => httpClient.delete(`/transactions/${id}`),
}
