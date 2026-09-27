import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { ReportFormat, TagTotalsReportFilters } from '../types'

interface DownloadTagTotalsReportInput {
  filters: TagTotalsReportFilters
  tagIds: number[]
  format: ReportFormat
  fileName: string
}

export function useDownloadTagTotalsReport() {
  return useMutation({
    mutationFn: async ({ filters, tagIds, format, fileName }: DownloadTagTotalsReportInput) => {
      const blob = await reportsApi.downloadTagTotalsReport(filters, format, tagIds)
      downloadBlob(blob, fileName)
    },
  })
}
