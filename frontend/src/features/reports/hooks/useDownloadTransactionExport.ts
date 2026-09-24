import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { TransactionExportInput } from '../types'

interface DownloadTransactionExportInput extends TransactionExportInput {
  fileName: string
}

export function useDownloadTransactionExport() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadTransactionExportInput) => {
      const blob = await reportsApi.downloadTransactionExport(input)
      downloadBlob(blob, fileName)
    },
  })
}
