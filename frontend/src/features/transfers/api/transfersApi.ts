import { httpClient } from '@/shared/api/httpClient'
import type { LinkableAccount, Transfer, TransferInput } from '../types'

export const transfersApi = {
  list: () => httpClient.get<Transfer[]>('/transfers'),
  linkableAccounts: () => httpClient.get<LinkableAccount[]>('/transfers/linkable-accounts'),
  create: (input: TransferInput) => httpClient.post<Transfer, TransferInput>('/transfers', input),
  remove: (id: number) => httpClient.delete(`/transfers/${id}`),
}
