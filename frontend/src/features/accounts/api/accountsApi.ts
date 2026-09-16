import { httpClient } from '@/shared/api/httpClient'
import type { Account, AccountInput } from '../types'

export const accountsApi = {
  list: () => httpClient.get<Account[]>('/accounts'),
  create: (input: AccountInput) => httpClient.post<Account, AccountInput>('/accounts', input),
  remove: (id: number) => httpClient.delete(`/accounts/${id}`),
}
