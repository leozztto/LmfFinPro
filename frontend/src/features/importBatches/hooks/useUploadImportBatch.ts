import { useMutation, useQueryClient } from '@tanstack/react-query'
import { importBatchesApi } from '../api/importBatchesApi'
import { IMPORT_BATCHES_QUERY_KEY } from './useImportBatches'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { ACCOUNTS_QUERY_KEY } from '@/features/accounts/hooks/useAccounts'

export function useUploadImportBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: importBatchesApi.upload,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: IMPORT_BATCHES_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
    },
  })
}
