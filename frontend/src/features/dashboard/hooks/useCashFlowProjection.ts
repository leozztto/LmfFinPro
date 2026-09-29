import { useQuery } from '@tanstack/react-query'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useCashFlowProjection(months = 3, scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'cash-flow-projection', months, scope ?? null],
    queryFn: () => dashboardApi.cashFlowProjection(months, scope),
  })
}
