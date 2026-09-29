import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringBudgetsApi } from '../api/recurringBudgetsApi'
import { invalidateRecurringBudgetQueries } from './useRecurringBudgets'

export function useCreateRecurringBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recurringBudgetsApi.create,
    onSuccess: () => invalidateRecurringBudgetQueries(queryClient),
  })
}
