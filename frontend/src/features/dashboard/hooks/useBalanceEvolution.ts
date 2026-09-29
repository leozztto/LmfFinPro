import { useQuery } from '@tanstack/react-query'
import type { AccountScope } from '@/features/accounts/types'
import { dashboardApi } from '../api/dashboardApi'

export function useBalanceEvolution(months = 6, scope?: AccountScope | null) {
  return useQuery({
    queryKey: ['dashboard', 'balance-evolution', months, scope ?? null],
    queryFn: () => dashboardApi.balanceEvolution(months, scope),
  })
}
