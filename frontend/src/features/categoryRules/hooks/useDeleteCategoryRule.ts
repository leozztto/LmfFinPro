import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryRulesApi } from '../api/categoryRulesApi'
import { CATEGORY_RULES_QUERY_KEY } from './useCategoryRules'

export function useDeleteCategoryRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: categoryRulesApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CATEGORY_RULES_QUERY_KEY }),
  })
}
