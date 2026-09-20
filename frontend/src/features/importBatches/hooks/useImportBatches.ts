import { useQuery } from '@tanstack/react-query'
import { importBatchesApi } from '../api/importBatchesApi'

export const IMPORT_BATCHES_QUERY_KEY = ['import-batches'] as const

export function useImportBatches() {
  return useQuery({ queryKey: IMPORT_BATCHES_QUERY_KEY, queryFn: importBatchesApi.list })
}
