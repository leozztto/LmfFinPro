import { useQuery, type QueryClient } from '@tanstack/react-query'
import { recurringBudgetsApi } from '../api/recurringBudgetsApi'
import { BUDGETS_QUERY_KEY } from './useBudgets'

export const RECURRING_BUDGETS_QUERY_KEY = ['recurring-budgets'] as const

export function useRecurringBudgets() {
  return useQuery({ queryKey: RECURRING_BUDGETS_QUERY_KEY, queryFn: recurringBudgetsApi.list })
}

/** Criar ou editar uma recorrência pode lançar orçamentos vencidos na hora, então a lista de orçamentos também precisa ser recarregada. */
export function invalidateRecurringBudgetQueries(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: RECURRING_BUDGETS_QUERY_KEY })
  queryClient.invalidateQueries({ queryKey: BUDGETS_QUERY_KEY })
}
