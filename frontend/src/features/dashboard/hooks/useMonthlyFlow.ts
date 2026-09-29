import { useQuery } from '@tanstack/react-query'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useMonthlyFlow(months = 6, scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'monthly-flow', months, scope ?? null],
    queryFn: () => dashboardApi.monthlyFlow(months, scope),
  })
}
