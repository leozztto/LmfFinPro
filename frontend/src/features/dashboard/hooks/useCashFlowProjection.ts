import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useCashFlowProjection(months = 3) {
  return useQuery({
    queryKey: ['dashboard', 'cash-flow-projection', months],
    queryFn: () => dashboardApi.cashFlowProjection(months),
  })
}
