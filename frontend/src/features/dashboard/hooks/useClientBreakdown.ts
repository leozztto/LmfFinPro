import { useQuery } from '@tanstack/react-query'
import { getCurrentYearMonth } from '@/shared/format/date'
import { dashboardApi } from '../api/dashboardApi'

export function useClientBreakdown(month = getCurrentYearMonth()) {
  return useQuery({
    queryKey: ['dashboard', 'client-breakdown', month],
    queryFn: () => dashboardApi.clientBreakdown(month),
  })
}
