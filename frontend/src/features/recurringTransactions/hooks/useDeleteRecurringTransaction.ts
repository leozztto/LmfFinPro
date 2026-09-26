import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringTransactionsApi } from '../api/recurringTransactionsApi'
import { invalidateRecurringTransactionQueries } from './useRecurringTransactions'

export function useDeleteRecurringTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recurringTransactionsApi.remove,
    onSuccess: () => invalidateRecurringTransactionQueries(queryClient),
  })
}
