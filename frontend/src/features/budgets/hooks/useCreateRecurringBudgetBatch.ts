import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringBudgetsApi } from '../api/recurringBudgetsApi'
import { invalidateRecurringBudgetQueries } from './useRecurringBudgets'

export function useCreateRecurringBudgetBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: recurringBudgetsApi.createBatch,
    onSuccess: () => invalidateRecurringBudgetQueries(queryClient),
  })
}
