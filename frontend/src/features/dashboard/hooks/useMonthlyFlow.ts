import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useMonthlyFlow(months = 6) {
  return useQuery({ queryKey: ['dashboard', 'monthly-flow', months], queryFn: () => dashboardApi.monthlyFlow(months) })
}
