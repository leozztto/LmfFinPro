import { httpClient } from '@/shared/api/httpClient'
import type {
  AccountStatementInput,
  BudgetVsActualReportInput,
  CategoryExpenseReportInput,
  ClientAnnualStatementInput,
  ClientReceiptInput,
  IncomeStatementInput,
  NetWorthReportInput,
  ReportFormat,
  TagTotalsReportFilters,
  TransactionExportInput,
  TransactionReportFilters,
  TransactionReportKind,
} from '../types'
import { buildTransactionReportQuery } from '../utils'

export const reportsApi = {
  downloadClientReceipt: (input: ClientReceiptInput) =>
    httpClient.getBlob(
      `/reports/client-receipt?clientId=${input.clientId}&referenceMonth=${input.referenceMonth}&format=${input.format}`,
    ),
  downloadAccountStatement: (input: AccountStatementInput) =>
    httpClient.getBlob(
      `/reports/account-statement?accountId=${input.accountId}&referenceMonth=${input.referenceMonth}&format=${input.format}`,
    ),
  downloadClientAnnualStatement: (input: ClientAnnualStatementInput) =>
    httpClient.getBlob(
      `/reports/client-annual-statement?clientId=${input.clientId}&year=${input.year}&format=${input.format}`,
    ),
  downloadCategoryExpenseReport: (input: CategoryExpenseReportInput) =>
    httpClient.getBlob(
      `/reports/category-expenses?referenceMonth=${input.referenceMonth}&format=${input.format}`,
    ),
  downloadIncomeStatement: (input: IncomeStatementInput) =>
    httpClient.getBlob(
      `/reports/income-statement?year=${input.year}&granularity=${input.granularity}&format=${input.format}`,
    ),
  downloadBudgetVsActualReport: (input: BudgetVsActualReportInput) =>
    httpClient.getBlob(
      `/reports/budget-vs-actual?referenceMonth=${input.referenceMonth}&format=${input.format}`,
    ),
  downloadTransactionExport: (input: TransactionExportInput) =>
    httpClient.getBlob(
      `/reports/transaction-export?referenceMonth=${input.referenceMonth}&format=${input.format}`,
    ),
  downloadNetWorthReport: (input: NetWorthReportInput) =>
    httpClient.getBlob(`/reports/net-worth?months=${input.months}&format=${input.format}`),
  downloadAttachmentsArchive: (year: string) => httpClient.getBlob(`/reports/attachments-archive?year=${year}`),
  downloadTransactionReport: (
    kind: TransactionReportKind,
    filters: TransactionReportFilters,
    format: ReportFormat,
    tagIds: number[] = [],
  ) =>
    httpClient.getBlob(
      `/reports/${kind === 'INCOME' ? 'incomes' : 'expenses'}?${buildTransactionReportQuery(filters, format, tagIds)}`,
    ),
  downloadTagTotalsReport: (filters: TagTotalsReportFilters, format: ReportFormat, tagIds: number[]) =>
    httpClient.getBlob(`/reports/tag-totals?${buildTransactionReportQuery(filters, format, tagIds)}`),
}
