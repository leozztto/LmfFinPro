export type ReportType =
  | 'CLIENT_RECEIPT'
  | 'ACCOUNT_STATEMENT'
  | 'CLIENT_ANNUAL_STATEMENT'
  | 'CATEGORY_EXPENSE_REPORT'
  | 'INCOME_STATEMENT'
  | 'BUDGET_VS_ACTUAL'
  | 'TRANSACTION_EXPORT'

export type ReportGranularity = 'MONTHLY' | 'QUARTERLY' | 'YEARLY'

export type ReportFormat = 'PDF' | 'CSV'

export interface ClientReceiptInput {
  clientId: number
  referenceMonth: string
  format: ReportFormat
}

export interface AccountStatementInput {
  accountId: number
  referenceMonth: string
  format: ReportFormat
}

export interface ClientAnnualStatementInput {
  clientId: number
  year: string
  format: ReportFormat
}

export interface CategoryExpenseReportInput {
  referenceMonth: string
  format: ReportFormat
}

export interface IncomeStatementInput {
  year: string
  granularity: ReportGranularity
  format: ReportFormat
}

export interface BudgetVsActualReportInput {
  referenceMonth: string
  format: ReportFormat
}

export interface TransactionExportInput {
  referenceMonth: string
  format: ReportFormat
}

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  CLIENT_RECEIPT: 'Recibo por cliente',
  ACCOUNT_STATEMENT: 'Extrato de conta',
  CLIENT_ANNUAL_STATEMENT: 'Demonstrativo anual por cliente',
  CATEGORY_EXPENSE_REPORT: 'Despesas por categoria',
  INCOME_STATEMENT: 'Resultado do período (DRE)',
  BUDGET_VS_ACTUAL: 'Orçamento vs. realizado',
  TRANSACTION_EXPORT: 'Exportação de transações',
}

export const REPORT_GRANULARITY_LABELS: Record<ReportGranularity, string> = {
  MONTHLY: 'Mensal',
  QUARTERLY: 'Trimestral',
  YEARLY: 'Anual',
}

export const REPORT_FORMAT_LABELS: Record<ReportFormat, string> = {
  PDF: 'PDF',
  CSV: 'CSV (Excel/Sheets)',
}
