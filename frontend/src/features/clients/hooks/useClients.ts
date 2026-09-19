import { useQuery } from '@tanstack/react-query'
import { clientsApi } from '../api/clientsApi'

export const CLIENTS_QUERY_KEY = ['clients'] as const

export function useClients() {
  return useQuery({ queryKey: CLIENTS_QUERY_KEY, queryFn: clientsApi.list })
}
