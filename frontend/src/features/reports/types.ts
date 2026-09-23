export type ReportType = 'CLIENT_RECEIPT' | 'ACCOUNT_STATEMENT' | 'CLIENT_ANNUAL_STATEMENT'

export interface ClientReceiptInput {
  clientId: number
  referenceMonth: string
}

export interface AccountStatementInput {
  accountId: number
  referenceMonth: string
}

export interface ClientAnnualStatementInput {
  clientId: number
  year: string
}

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  CLIENT_RECEIPT: 'Recibo por cliente',
  ACCOUNT_STATEMENT: 'Extrato de conta',
  CLIENT_ANNUAL_STATEMENT: 'Demonstrativo anual por cliente',
}
