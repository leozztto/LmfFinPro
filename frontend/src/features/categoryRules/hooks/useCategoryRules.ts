import { useQuery } from '@tanstack/react-query'
import { categoryRulesApi } from '../api/categoryRulesApi'

export const CATEGORY_RULES_QUERY_KEY = ['category-rules'] as const

export function useCategoryRules() {
  return useQuery({ queryKey: CATEGORY_RULES_QUERY_KEY, queryFn: categoryRulesApi.list })
}
