import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useDashboardOverview() {
  return useQuery({ queryKey: ['dashboard', 'overview'], queryFn: dashboardApi.overview })
}
