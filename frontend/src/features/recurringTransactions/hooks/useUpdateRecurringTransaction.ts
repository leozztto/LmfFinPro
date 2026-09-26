import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringTransactionsApi } from '../api/recurringTransactionsApi'
import { invalidateRecurringTransactionQueries } from './useRecurringTransactions'
import type { RecurringTransactionUpdateInput } from '../types'

export function useUpdateRecurringTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: RecurringTransactionUpdateInput }) =>
      recurringTransactionsApi.update(id, input),
    onSuccess: () => invalidateRecurringTransactionQueries(queryClient),
  })
}
