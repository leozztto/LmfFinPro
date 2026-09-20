import { useQuery } from '@tanstack/react-query'
import { taxEstimatesApi } from '../api/taxEstimatesApi'

export const TAX_ESTIMATES_QUERY_KEY = ['tax-estimates'] as const

export function useTaxEstimates() {
  return useQuery({ queryKey: TAX_ESTIMATES_QUERY_KEY, queryFn: taxEstimatesApi.list })
}
