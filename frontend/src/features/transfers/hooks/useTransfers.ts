import { useQuery } from '@tanstack/react-query'
import { transfersApi } from '../api/transfersApi'

export const TRANSFERS_QUERY_KEY = ['transfers'] as const

export function useTransfers() {
  return useQuery({ queryKey: TRANSFERS_QUERY_KEY, queryFn: transfersApi.list })
}

/** Contas dos outros espaços da pessoa com as quais ela pode transferir a partir do espaço atual. */
export function useLinkableAccounts() {
  return useQuery({ queryKey: [...TRANSFERS_QUERY_KEY, 'linkable-accounts'], queryFn: transfersApi.linkableAccounts })
}
