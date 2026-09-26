import { httpClient } from '@/shared/api/httpClient'
import type { GoalContribution, GoalContributionInput, SavingsGoal, SavingsGoalInput } from '../types'

export const savingsGoalsApi = {
  list: () => httpClient.get<SavingsGoal[]>('/savings-goals'),
  create: (input: SavingsGoalInput) => httpClient.post<SavingsGoal, SavingsGoalInput>('/savings-goals', input),
  update: (id: number, input: SavingsGoalInput) => httpClient.put<SavingsGoal>(`/savings-goals/${id}`, input),
  remove: (id: number) => httpClient.delete(`/savings-goals/${id}`),
  suggestedTaxRate: () => httpClient.get<{ incomeRate: number }>('/savings-goals/suggested-tax-rate'),
  listContributions: (goalId: number) => httpClient.get<GoalContribution[]>(`/savings-goals/${goalId}/contributions`),
  addContribution: (goalId: number, input: GoalContributionInput) =>
    httpClient.post<GoalContribution, GoalContributionInput>(`/savings-goals/${goalId}/contributions`, input),
  applySuggestion: (goalId: number) =>
    httpClient.post<GoalContribution, undefined>(`/savings-goals/${goalId}/contributions/suggested`, undefined),
  removeContribution: (goalId: number, contributionId: number) =>
    httpClient.delete(`/savings-goals/${goalId}/contributions/${contributionId}`),
}
