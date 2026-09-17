import { httpClient } from '@/shared/api/httpClient'
import type { Transfer, TransferInput } from '../types'

export const transfersApi = {
  list: () => httpClient.get<Transfer[]>('/transfers'),
  create: (input: TransferInput) => httpClient.post<Transfer, TransferInput>('/transfers', input),
  remove: (id: number) => httpClient.delete(`/transfers/${id}`),
}
