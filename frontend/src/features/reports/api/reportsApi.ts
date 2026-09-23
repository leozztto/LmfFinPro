import { httpClient } from '@/shared/api/httpClient'
import type { AccountStatementInput, ClientReceiptInput } from '../types'

export const reportsApi = {
  downloadClientReceipt: (input: ClientReceiptInput) =>
    httpClient.getBlob(
      `/reports/client-receipt?clientId=${input.clientId}&referenceMonth=${input.referenceMonth}`,
    ),
  downloadAccountStatement: (input: AccountStatementInput) =>
    httpClient.getBlob(
      `/reports/account-statement?accountId=${input.accountId}&referenceMonth=${input.referenceMonth}`,
    ),
}
