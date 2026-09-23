import { useMutation } from '@tanstack/react-query'
import { reportsApi } from '../api/reportsApi'
import type { ClientReceiptInput } from '../types'

interface DownloadClientReceiptInput extends ClientReceiptInput {
  fileName: string
}

export function useDownloadClientReceipt() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadClientReceiptInput) => {
      const blob = await reportsApi.downloadClientReceipt(input)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    },
  })
}
