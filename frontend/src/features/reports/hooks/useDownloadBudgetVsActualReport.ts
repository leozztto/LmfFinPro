import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { BudgetVsActualReportInput } from '../types'

interface DownloadBudgetVsActualReportInput extends BudgetVsActualReportInput {
  fileName: string
}

export function useDownloadBudgetVsActualReport() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadBudgetVsActualReportInput) => {
      const blob = await reportsApi.downloadBudgetVsActualReport(input)
      downloadBlob(blob, fileName)
    },
  })
}
