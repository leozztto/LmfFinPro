import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringTransactionsApi } from '../api/recurringTransactionsApi'
import { invalidateRecurringTransactionQueries } from './useRecurringTransactions'

export function useCreateRecurringTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recurringTransactionsApi.create,
    onSuccess: () => invalidateRecurringTransactionQueries(queryClient),
  })
}
