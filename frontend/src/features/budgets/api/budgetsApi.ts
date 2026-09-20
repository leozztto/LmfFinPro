import { httpClient } from '@/shared/api/httpClient'
import type { Budget, BudgetInput } from '../types'

export const budgetsApi = {
  list: () => httpClient.get<Budget[]>('/budgets'),
  create: (input: BudgetInput) => httpClient.post<Budget, BudgetInput>('/budgets', input),
  remove: (id: number) => httpClient.delete(`/budgets/${id}`),
}
