import { httpClient } from '@/shared/api/httpClient'
import type { TaxEstimate, TaxEstimateInput, TaxRegime } from '../types'

export const taxEstimatesApi = {
  list: () => httpClient.get<TaxEstimate[]>('/tax-estimates'),
  create: (input: TaxEstimateInput) => httpClient.post<TaxEstimate, TaxEstimateInput>('/tax-estimates', input),
  remove: (id: number) => httpClient.delete(`/tax-estimates/${id}`),
  suggestedRevenue: (referenceMonth: string) =>
    httpClient.get<{ grossRevenue: number }>(`/tax-estimates/suggested-revenue?referenceMonth=${referenceMonth}`),
  suggestedRate: (regime: TaxRegime, grossRevenue: number) =>
    httpClient.get<{ rate: number }>(
      `/tax-estimates/suggested-rate?regime=${regime}&grossRevenue=${grossRevenue}`,
    ),
}
