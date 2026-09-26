import { httpClient } from '@/shared/api/httpClient'
import type { RecurringTransaction, RecurringTransactionInput, RecurringTransactionUpdateInput } from '../types'

export const recurringTransactionsApi = {
  list: () => httpClient.get<RecurringTransaction[]>('/recurring-transactions'),
  create: (input: RecurringTransactionInput) =>
    httpClient.post<RecurringTransaction, RecurringTransactionInput>('/recurring-transactions', input),
  update: (id: number, input: RecurringTransactionUpdateInput) =>
    httpClient.put<RecurringTransaction, RecurringTransactionUpdateInput>(`/recurring-transactions/${id}`, input),
  remove: (id: number) => httpClient.delete(`/recurring-transactions/${id}`),
}
