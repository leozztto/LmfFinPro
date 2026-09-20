import { useMutation, useQueryClient } from '@tanstack/react-query'
import { budgetsApi } from '../api/budgetsApi'
import { BUDGETS_QUERY_KEY } from './useBudgets'

export function useCreateBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: budgetsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: BUDGETS_QUERY_KEY }),
  })
}
