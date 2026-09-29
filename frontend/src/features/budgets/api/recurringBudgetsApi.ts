import { httpClient } from '@/shared/api/httpClient'
import type { RecurringBudget, RecurringBudgetInput, RecurringBudgetUpdateInput } from '../types'

export const recurringBudgetsApi = {
  list: () => httpClient.get<RecurringBudget[]>('/recurring-budgets'),
  create: (input: RecurringBudgetInput) =>
    httpClient.post<RecurringBudget, RecurringBudgetInput>('/recurring-budgets', input),
  update: (id: number, input: RecurringBudgetUpdateInput) =>
    httpClient.put<RecurringBudget, RecurringBudgetUpdateInput>(`/recurring-budgets/${id}`, input),
  remove: (id: number) => httpClient.delete(`/recurring-budgets/${id}`),
}
