import { useQuery } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactionsApi'

export const TRANSACTIONS_QUERY_KEY = ['transactions'] as const

export function useTransactions() {
  return useQuery({ queryKey: TRANSACTIONS_QUERY_KEY, queryFn: transactionsApi.list })
}
