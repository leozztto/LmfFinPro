import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { IncomeStatementInput } from '../types'

interface DownloadIncomeStatementInput extends IncomeStatementInput {
  fileName: string
}

export function useDownloadIncomeStatement() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadIncomeStatementInput) => {
      const blob = await reportsApi.downloadIncomeStatement(input)
      downloadBlob(blob, fileName)
    },
  })
}
