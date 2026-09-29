import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recurringBudgetsApi } from '../api/recurringBudgetsApi'
import { invalidateRecurringBudgetQueries } from './useRecurringBudgets'
import type { RecurringBudgetUpdateInput } from '../types'

export function useUpdateRecurringBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: RecurringBudgetUpdateInput }) =>
      recurringBudgetsApi.update(id, input),
    onSuccess: () => invalidateRecurringBudgetQueries(queryClient),
  })
}
