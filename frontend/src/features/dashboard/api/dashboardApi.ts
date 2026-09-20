import { httpClient } from '@/shared/api/httpClient'
import type { CategoryType } from '@/features/categories/types'
import type {
  DashboardOverview,
  RawBalancePoint,
  RawBreakdownPoint,
  RawCashFlowProjectionPoint,
  RawMonthlyFlowPoint,
} from '../types'

export const dashboardApi = {
  overview: () => httpClient.get<DashboardOverview>('/dashboard/overview'),
  monthlyFlow: (months = 6) => httpClient.get<RawMonthlyFlowPoint[]>(`/dashboard/monthly-flow?months=${months}`),
  balanceEvolution: (months = 6) => httpClient.get<RawBalancePoint[]>(`/dashboard/balance-evolution?months=${months}`),
  cashFlowProjection: (months = 3) =>
    httpClient.get<RawCashFlowProjectionPoint[]>(`/dashboard/cash-flow-projection?months=${months}`),
  categoryBreakdown: (type: CategoryType, month: string) =>
    httpClient.get<RawBreakdownPoint[]>(`/dashboard/category-breakdown?type=${type}&month=${month}`),
  clientBreakdown: (month: string) => httpClient.get<RawBreakdownPoint[]>(`/dashboard/client-breakdown?month=${month}`),
}
