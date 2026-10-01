import { useQuery } from '@tanstack/react-query'
import { taxEstimatesApi } from '../api/taxEstimatesApi'

/** Receita do mês (sem transferências), somada pelo backend, para pré-preencher a receita bruta. */
export function useSuggestedRevenue(referenceMonth: string) {
  return useQuery({
    queryKey: ['tax-estimates', 'suggested-revenue', referenceMonth],
    queryFn: () => taxEstimatesApi.suggestedRevenue(referenceMonth),
    enabled: /^\d{4}-\d{2}$/.test(referenceMonth),
  })
}
