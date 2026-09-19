import { useQuery } from '@tanstack/react-query'
import { importBatchesApi } from '../api/importBatchesApi'

export const importBatchTransactionsQueryKey = (batchId: number) => ['import-batches', batchId, 'transactions'] as const

export function useImportBatchTransactions(batchId: number | null) {
  return useQuery({
    queryKey: importBatchTransactionsQueryKey(batchId ?? -1),
    queryFn: () => importBatchesApi.listTransactions(batchId as number),
    enabled: batchId != null,
  })
}
