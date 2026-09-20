import { useQuery } from '@tanstack/react-query'
import { budgetsApi } from '../api/budgetsApi'

export const BUDGETS_QUERY_KEY = ['budgets'] as const

export function useBudgets() {
  return useQuery({ queryKey: BUDGETS_QUERY_KEY, queryFn: budgetsApi.list })
}
