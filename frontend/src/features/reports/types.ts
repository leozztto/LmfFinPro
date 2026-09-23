export type ReportType = 'CLIENT_RECEIPT'

export interface ClientReceiptInput {
  clientId: number
  referenceMonth: string
}

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  CLIENT_RECEIPT: 'Recibo por cliente',
}
