export type ReportType =
  | 'CLIENT_RECEIPT'
  | 'ACCOUNT_STATEMENT'
  | 'CLIENT_ANNUAL_STATEMENT'
  | 'CATEGORY_EXPENSE_REPORT'
  | 'INCOME_STATEMENT'
  | 'BUDGET_VS_ACTUAL'

export type ReportGranularity = 'MONTHLY' | 'QUARTERLY' | 'YEARLY'

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

export interface CategoryExpenseReportInput {
  referenceMonth: string
}

export interface IncomeStatementInput {
  year: string
  granularity: ReportGranularity
}

export interface BudgetVsActualReportInput {
  referenceMonth: string
}

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  CLIENT_RECEIPT: 'Recibo por cliente',
  ACCOUNT_STATEMENT: 'Extrato de conta',
  CLIENT_ANNUAL_STATEMENT: 'Demonstrativo anual por cliente',
  CATEGORY_EXPENSE_REPORT: 'Despesas por categoria',
  INCOME_STATEMENT: 'Resultado do período (DRE)',
  BUDGET_VS_ACTUAL: 'Orçamento vs. realizado',
}

export const REPORT_GRANULARITY_LABELS: Record<ReportGranularity, string> = {
  MONTHLY: 'Mensal',
  QUARTERLY: 'Trimestral',
  YEARLY: 'Anual',
}
