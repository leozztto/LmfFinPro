import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { AccountStatementInput } from '../types'

interface DownloadAccountStatementInput extends AccountStatementInput {
  fileName: string
}

export function useDownloadAccountStatement() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadAccountStatementInput) => {
      const blob = await reportsApi.downloadAccountStatement(input)
      downloadBlob(blob, fileName)
    },
  })
}
