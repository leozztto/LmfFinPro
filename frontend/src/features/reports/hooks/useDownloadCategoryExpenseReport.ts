import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { CategoryExpenseReportInput } from '../types'

interface DownloadCategoryExpenseReportInput extends CategoryExpenseReportInput {
  fileName: string
}

export function useDownloadCategoryExpenseReport() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadCategoryExpenseReportInput) => {
      const blob = await reportsApi.downloadCategoryExpenseReport(input)
      downloadBlob(blob, fileName)
    },
  })
}
