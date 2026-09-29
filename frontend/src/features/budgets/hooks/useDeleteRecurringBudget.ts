import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringBudgetsApi } from '../api/recurringBudgetsApi'
import { RECURRING_BUDGETS_QUERY_KEY } from './useRecurringBudgets'

export function useDeleteRecurringBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recurringBudgetsApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RECURRING_BUDGETS_QUERY_KEY }),
  })
}
