import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transfersApi } from '../api/transfersApi'
import { TRANSFERS_QUERY_KEY } from './useTransfers'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { ACCOUNTS_QUERY_KEY } from '@/features/accounts/hooks/useAccounts'
import { PRO_LABORE_QUERY_KEY } from '@/features/pro-labore/hooks/useProLabore'

export function useCreateTransfer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: transfersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRANSFERS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: PRO_LABORE_QUERY_KEY })
    },
  })
}
