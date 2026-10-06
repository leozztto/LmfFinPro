import { httpClient } from '@/shared/api/httpClient'
import type { Account, AccountInput, AccountValuation, AccountValuationInput } from '../types'

export const accountsApi = {
  list: () => httpClient.get<Account[]>('/accounts'),
  /** As contas de um grupo específico, mesmo que outro esteja ativo (ex.: as do espaço pessoal ao compartilhar). */
  listOfHousehold: (householdId: number) => httpClient.get<Account[]>('/accounts', { householdId }),
  create: (input: AccountInput) => httpClient.post<Account, AccountInput>('/accounts', input),
  update: (id: number, input: AccountInput) => httpClient.put<Account, AccountInput>(`/accounts/${id}`, input),
  remove: (id: number) => httpClient.delete(`/accounts/${id}`),
  listValuations: (accountId: number) => httpClient.get<AccountValuation[]>(`/accounts/${accountId}/valuations`),
  saveValuation: (accountId: number, input: AccountValuationInput) =>
    httpClient.post<AccountValuation, AccountValuationInput>(`/accounts/${accountId}/valuations`, input),
  removeValuation: (accountId: number, valuationId: number) =>
    httpClient.delete(`/accounts/${accountId}/valuations/${valuationId}`),
}
