import { httpClient } from '@/shared/api/httpClient'
import type { CategoryRule, CategoryRuleInput } from '../types'

export const categoryRulesApi = {
  list: () => httpClient.get<CategoryRule[]>('/category-rules'),
  create: (input: CategoryRuleInput) => httpClient.post<CategoryRule, CategoryRuleInput>('/category-rules', input),
  remove: (id: number) => httpClient.delete(`/category-rules/${id}`),
}
