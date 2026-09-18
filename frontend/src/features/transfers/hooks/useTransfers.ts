import { useQuery } from '@tanstack/react-query'
import { transfersApi } from '../api/transfersApi'

export const TRANSFERS_QUERY_KEY = ['transfers'] as const

export function useTransfers() {
  return useQuery({ queryKey: TRANSFERS_QUERY_KEY, queryFn: transfersApi.list })
}
