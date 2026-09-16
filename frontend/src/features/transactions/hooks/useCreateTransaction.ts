import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactionsApi'
import { TRANSACTIONS_QUERY_KEY } from './useTransactions'

export function useCreateTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: transactionsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY }),
  })
}
