import { httpClient } from '@/shared/api/httpClient'
import type { Category, CategoryInput } from '../types'

export const categoriesApi = {
  list: () => httpClient.get<Category[]>('/categories'),
  create: (input: CategoryInput) => httpClient.post<Category, CategoryInput>('/categories', input),
  remove: (id: number) => httpClient.delete(`/categories/${id}`),
}
