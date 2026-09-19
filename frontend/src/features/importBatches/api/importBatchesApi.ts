import { httpClient } from '@/shared/api/httpClient'
import type { Transaction } from '@/features/transactions/types'
import type { ImportBatch, ImportBatchUploadInput, TransactionReviewInput } from '../types'

export const importBatchesApi = {
  list: () => httpClient.get<ImportBatch[]>('/import-batches'),

  upload: ({ accountId, file }: ImportBatchUploadInput) => {
    const formData = new FormData()
    formData.append('accountId', String(accountId))
    formData.append('file', file)
    return httpClient.postForm<ImportBatch>('/import-batches', formData)
  },

  listTransactions: (batchId: number) => httpClient.get<Transaction[]>(`/import-batches/${batchId}/transactions`),

  reviewTransaction: (batchId: number, transactionId: number, input: TransactionReviewInput) =>
    httpClient.put<Transaction, TransactionReviewInput>(
      `/import-batches/${batchId}/transactions/${transactionId}`,
      input,
    ),
}
