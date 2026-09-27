import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactionsApi'
import { TRANSACTIONS_QUERY_KEY } from './useTransactions'
import { ACCOUNTS_QUERY_KEY } from '@/features/accounts/hooks/useAccounts'
import { TAGS_QUERY_KEY } from '@/features/tags/hooks/useTags'

export function useCreateTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: transactionsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
      // Tags digitadas pela primeira vez passam a existir.
      queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY })
    },
  })
}
