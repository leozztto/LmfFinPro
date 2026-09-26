import { useQuery } from '@tanstack/react-query'
import { clientsApi } from '../api/clientsApi'
import { CLIENTS_QUERY_KEY } from './useClients'

export function useClientAnalytics(months: number, onlyReceived: boolean) {
  return useQuery({
    queryKey: [...CLIENTS_QUERY_KEY, 'analytics', months, onlyReceived],
    queryFn: () => clientsApi.analytics(months, onlyReceived),
  })
}
