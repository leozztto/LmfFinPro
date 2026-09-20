import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useBalanceEvolution(months = 6) {
  return useQuery({
    queryKey: ['dashboard', 'balance-evolution', months],
    queryFn: () => dashboardApi.balanceEvolution(months),
  })
}
