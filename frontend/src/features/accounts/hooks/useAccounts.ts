import { useQuery } from '@tanstack/react-query'
import { accountsApi } from '../api/accountsApi'

export const ACCOUNTS_QUERY_KEY = ['accounts'] as const

export function useAccounts() {
  return useQuery({ queryKey: ACCOUNTS_QUERY_KEY, queryFn: accountsApi.list })
}
