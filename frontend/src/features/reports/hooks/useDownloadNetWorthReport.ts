import { useMutation } from '@tanstack/react-query'
import { downloadBlob } from '@/shared/download/downloadBlob'
import { reportsApi } from '../api/reportsApi'
import type { NetWorthReportInput } from '../types'

interface DownloadNetWorthReportInput extends NetWorthReportInput {
  fileName: string
}

export function useDownloadNetWorthReport() {
  return useMutation({
    mutationFn: async ({ fileName, ...input }: DownloadNetWorthReportInput) => {
      const blob = await reportsApi.downloadNetWorthReport(input)
      downloadBlob(blob, fileName)
    },
  })
}
