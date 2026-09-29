import { useQuery } from '@tanstack/react-query'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useDashboardOverview(scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'overview', scope ?? null],
    queryFn: () => dashboardApi.overview(scope),
  })
}
