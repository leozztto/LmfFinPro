import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { savingsGoalsApi } from '../api/savingsGoalsApi'
import type { GoalContributionInput, SavingsGoalInput } from '../types'

export const SAVINGS_GOALS_QUERY_KEY = ['savings-goals'] as const

export function useSavingsGoals() {
  return useQuery({ queryKey: SAVINGS_GOALS_QUERY_KEY, queryFn: savingsGoalsApi.list })
}

export function useGoalContributions(goalId: number, enabled = true) {
  return useQuery({
    queryKey: [...SAVINGS_GOALS_QUERY_KEY, goalId, 'contributions'],
    queryFn: () => savingsGoalsApi.listContributions(goalId),
    enabled,
  })
}

export function useSuggestedTaxRate(enabled: boolean) {
  return useQuery({
    queryKey: [...SAVINGS_GOALS_QUERY_KEY, 'suggested-tax-rate'],
    queryFn: savingsGoalsApi.suggestedTaxRate,
    enabled,
  })
}

/**
 * Toda alteração invalida a lista inteira (metas e históricos): o valor guardado, o que falta e a
 * sugestão do mês são recalculados no backend.
 */
function useInvalidatingMutation<TVariables, TResult>(mutationFn: (variables: TVariables) => Promise<TResult>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SAVINGS_GOALS_QUERY_KEY }),
  })
}

export function useCreateSavingsGoal() {
  return useInvalidatingMutation(savingsGoalsApi.create)
}

export function useUpdateSavingsGoal() {
  return useInvalidatingMutation(({ id, input }: { id: number; input: SavingsGoalInput }) =>
    savingsGoalsApi.update(id, input),
  )
}

export function useDeleteSavingsGoal() {
  return useInvalidatingMutation(savingsGoalsApi.remove)
}

export function useAddContribution() {
  return useInvalidatingMutation(({ goalId, input }: { goalId: number; input: GoalContributionInput }) =>
    savingsGoalsApi.addContribution(goalId, input),
  )
}

export function useApplySuggestion() {
  return useInvalidatingMutation(savingsGoalsApi.applySuggestion)
}

export function useDeleteContribution() {
  return useInvalidatingMutation(({ goalId, contributionId }: { goalId: number; contributionId: number }) =>
    savingsGoalsApi.removeContribution(goalId, contributionId),
  )
}
