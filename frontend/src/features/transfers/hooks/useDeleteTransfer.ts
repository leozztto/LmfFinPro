import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transfersApi } from '../api/transfersApi'
import { TRANSFERS_QUERY_KEY } from './useTransfers'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'

export function useDeleteTransfer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: transfersApi.remove,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRANSFERS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
    },
  })
}
