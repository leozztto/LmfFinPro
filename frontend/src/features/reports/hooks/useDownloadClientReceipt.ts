import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { ClientReceiptInput } from '../types'

interface DownloadClientReceiptInput extends ClientReceiptInput {
  fileName: string
}

export function useDownloadClientReceipt() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadClientReceiptInput) => {
      const blob = await reportsApi.downloadClientReceipt(input)
      downloadBlob(blob, fileName)
    },
  })
}
