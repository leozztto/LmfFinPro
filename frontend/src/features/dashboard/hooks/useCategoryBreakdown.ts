import { useQuery } from '@tanstack/react-query'
import { getCurrentYearMonth } from '@/shared/format/date'
import type { CategoryType } from '@/features/categories/types'
import { dashboardApi } from '../api/dashboardApi'

export function useCategoryBreakdown(type: CategoryType, month = getCurrentYearMonth()) {
  return useQuery({
    queryKey: ['dashboard', 'category-breakdown', type, month],
    queryFn: () => dashboardApi.categoryBreakdown(type, month),
  })
}
