import { useMutation, useQueryClient } from '@tanstack/react-query'
import { taxEstimatesApi } from '../api/taxEstimatesApi'
import { TAX_ESTIMATES_QUERY_KEY } from './useTaxEstimates'

export function useCreateTaxEstimate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: taxEstimatesApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TAX_ESTIMATES_QUERY_KEY }),
  })
}
