import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { ReportFormat, TransactionReportFilters, TransactionReportKind } from '../types'

interface DownloadTransactionReportInput {
  kind: TransactionReportKind
  filters: TransactionReportFilters
  format: ReportFormat
  fileName: string
}

export function useDownloadTransactionReport() {
  return useMutation({
    mutationFn: async ({ kind, filters, format, fileName }: DownloadTransactionReportInput) => {
      const blob = await reportsApi.downloadTransactionReport(kind, filters, format)
      downloadBlob(blob, fileName)
    },
  })
}
