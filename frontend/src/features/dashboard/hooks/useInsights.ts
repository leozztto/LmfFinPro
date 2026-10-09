import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useInsights() {
  return useQuery({
    queryKey: ['dashboard', 'insights'],
    queryFn: dashboardApi.insights,
  })
}
