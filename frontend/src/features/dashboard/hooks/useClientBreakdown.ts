import { useQuery } from '@tanstack/react-query'
import { getCurrentYearMonth } from '@/shared/format/date'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useClientBreakdown(month = getCurrentYearMonth(), scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'client-breakdown', month, scope ?? null],
    queryFn: () => dashboardApi.clientBreakdown(month, scope),
  })
}
