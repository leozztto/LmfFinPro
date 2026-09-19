import { httpClient } from '@/shared/api/httpClient'
import type { Client, ClientInput } from '../types'

export const clientsApi = {
  list: () => httpClient.get<Client[]>('/clients'),
  create: (input: ClientInput) => httpClient.post<Client, ClientInput>('/clients', input),
  update: (id: number, input: ClientInput) => httpClient.put<Client, ClientInput>(`/clients/${id}`, input),
  remove: (id: number) => httpClient.delete(`/clients/${id}`),
}
