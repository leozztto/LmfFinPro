import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { ClientAnnualStatementInput } from '../types'

interface DownloadClientAnnualStatementInput extends ClientAnnualStatementInput {
  fileName: string
}

export function useDownloadClientAnnualStatement() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadClientAnnualStatementInput) => {
      const blob = await reportsApi.downloadClientAnnualStatement(input)
      downloadBlob(blob, fileName)
    },
  })
}
