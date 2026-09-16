import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoriesApi } from '../api/categoriesApi'
import { CATEGORIES_QUERY_KEY } from './useCategories'

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: categoriesApi.remove,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY }),
  })
}
