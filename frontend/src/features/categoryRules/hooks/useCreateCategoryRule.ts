import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryRulesApi } from '../api/categoryRulesApi'
import { CATEGORY_RULES_QUERY_KEY } from './useCategoryRules'

export function useCreateCategoryRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: categoryRulesApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CATEGORY_RULES_QUERY_KEY }),
  })
}
