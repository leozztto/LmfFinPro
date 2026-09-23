import { httpClient } from '@/shared/api/httpClient'
import type { ClientReceiptInput } from '../types'

export const reportsApi = {
  downloadClientReceipt: (input: ClientReceiptInput) =>
    httpClient.getBlob(
      `/reports/client-receipt?clientId=${input.clientId}&referenceMonth=${input.referenceMonth}`,
    ),
}
