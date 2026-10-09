import { useQuery } from '@tanstack/react-query'
import { importBatchesApi } from '../api/importBatchesApi'

export function useImportSummary(batchId: number | null) {
  return useQuery({
    queryKey: ['import-batches', batchId, 'summary'],
    queryFn: () => importBatchesApi.summary(batchId as number),
    enabled: batchId != null,
  })
}
