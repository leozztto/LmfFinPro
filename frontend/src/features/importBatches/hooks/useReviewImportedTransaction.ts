import { useMutation, useQueryClient } from '@tanstack/react-query'
import { importBatchesApi } from '../api/importBatchesApi'
import { IMPORT_BATCHES_QUERY_KEY } from './useImportBatches'
import { importBatchTransactionsQueryKey } from './useImportBatchTransactions'
import { TRANSACTIONS_QUERY_KEY } from '@/features/transactions/hooks/useTransactions'
import { CATEGORY_RULES_QUERY_KEY } from '@/features/categoryRules/hooks/useCategoryRules'
import type { TransactionReviewInput } from '../types'

interface ReviewVariables {
  batchId: number
  transactionId: number
  input: TransactionReviewInput
}

export function useReviewImportedTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ batchId, transactionId, input }: ReviewVariables) =>
      importBatchesApi.reviewTransaction(batchId, transactionId, input),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: importBatchTransactionsQueryKey(variables.batchId) })
      queryClient.invalidateQueries({ queryKey: IMPORT_BATCHES_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: TRANSACTIONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: CATEGORY_RULES_QUERY_KEY })
    },
  })
}
