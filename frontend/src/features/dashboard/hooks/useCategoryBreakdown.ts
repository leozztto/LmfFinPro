import { useQuery } from '@tanstack/react-query'
import { getCurrentYearMonth } from '@/shared/format/date'
import type { CategoryType } from '@/features/categories/types'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useCategoryBreakdown(type: CategoryType, month = getCurrentYearMonth(), scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'category-breakdown', type, month, scope ?? null],
    queryFn: () => dashboardApi.categoryBreakdown(type, month, scope),
  })
}
