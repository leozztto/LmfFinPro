import { httpClient } from '@/shared/api/httpClient'
import type {
  AccountStatementInput,
  CategoryExpenseReportInput,
  ClientAnnualStatementInput,
  ClientReceiptInput,
  IncomeStatementInput,
} from '../types'

export const reportsApi = {
  downloadClientReceipt: (input: ClientReceiptInput) =>
    httpClient.getBlob(
      `/reports/client-receipt?clientId=${input.clientId}&referenceMonth=${input.referenceMonth}`,
    ),
  downloadAccountStatement: (input: AccountStatementInput) =>
    httpClient.getBlob(
      `/reports/account-statement?accountId=${input.accountId}&referenceMonth=${input.referenceMonth}`,
    ),
  downloadClientAnnualStatement: (input: ClientAnnualStatementInput) =>
    httpClient.getBlob(
      `/reports/client-annual-statement?clientId=${input.clientId}&year=${input.year}`,
    ),
  downloadCategoryExpenseReport: (input: CategoryExpenseReportInput) =>
    httpClient.getBlob(`/reports/category-expenses?referenceMonth=${input.referenceMonth}`),
  downloadIncomeStatement: (input: IncomeStatementInput) =>
    httpClient.getBlob(
      `/reports/income-statement?year=${input.year}&granularity=${input.granularity}`,
    ),
}
