import { httpClient } from '@/shared/api/httpClient'
import type { CategoryType } from '@/features/categories/types'
import type { AccountScope } from '@/features/accounts/types'
import type {
  Insight,
  DashboardOverview,
  RawBalancePoint,
  RawBreakdownPoint,
  RawCashFlowProjectionPoint,
  RawMonthlyFlowPoint,
} from '../types'

function toQuery(params: Record<string, string | number | null | undefined>) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value != null) search.set(key, String(value))
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const dashboardApi = {
  overview: (scope?: AccountScope | null) => httpClient.get<DashboardOverview>(`/dashboard/overview${toQuery({ scope })}`),
  monthlyFlow: (months = 6, scope?: AccountScope | null) =>
    httpClient.get<RawMonthlyFlowPoint[]>(`/dashboard/monthly-flow${toQuery({ months, scope })}`),
  balanceEvolution: (months = 6, scope?: AccountScope | null) =>
    httpClient.get<RawBalancePoint[]>(`/dashboard/balance-evolution${toQuery({ months, scope })}`),
  cashFlowProjection: (months = 3, scope?: AccountScope | null) =>
    httpClient.get<RawCashFlowProjectionPoint[]>(`/dashboard/cash-flow-projection${toQuery({ months, scope })}`),
  categoryBreakdown: (type: CategoryType, month: string, scope?: AccountScope | null) =>
    httpClient.get<RawBreakdownPoint[]>(`/dashboard/category-breakdown${toQuery({ type, month, scope })}`),
  insights: () => httpClient.get<Insight[]>('/insights'),
  clientBreakdown: (month: string, scope?: AccountScope | null) =>
    httpClient.get<RawBreakdownPoint[]>(`/dashboard/client-breakdown${toQuery({ month, scope })}`),
}
