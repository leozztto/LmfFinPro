import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoriesApi } from '../api/categoriesApi'
import { CATEGORIES_QUERY_KEY } from './useCategories'
import type { CategoryInput } from '../types'

export function useUpdateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: CategoryInput }) => categoriesApi.update(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY }),
  })
}
