import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactionsApi'
import { TRANSACTIONS_QUERY_KEY } from './useTransactions'

export function useDeleteTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: transactionsApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY }),
  })
}
