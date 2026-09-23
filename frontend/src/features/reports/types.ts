export type ReportType = 'CLIENT_RECEIPT' | 'ACCOUNT_STATEMENT'

export interface ClientReceiptInput {
  clientId: number
  referenceMonth: string
}

export interface AccountStatementInput {
  accountId: number
  referenceMonth: string
}

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  CLIENT_RECEIPT: 'Recibo por cliente',
  ACCOUNT_STATEMENT: 'Extrato de conta',
}
