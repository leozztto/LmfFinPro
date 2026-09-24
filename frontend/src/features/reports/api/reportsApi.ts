import { httpClient } from '@/shared/api/httpClient'
import type {
  AccountStatementInput,
  BudgetVsActualReportInput,
  CategoryExpenseReportInput,
  ClientAnnualStatementInput,
  ClientReceiptInput,
  IncomeStatementInput,
  TransactionExportInput,
} from '../types'

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
}
