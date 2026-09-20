import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { taxEstimatesApi } from '../api/taxEstimatesApi'
import type { TaxRegime } from '../types'

const DEBOUNCE_MS = 400

/** Sugestão de alíquota por regime, recalculada ao digitar a receita bruta (debounced). */
export function useSuggestedRate(regime: TaxRegime | '', grossRevenue: number) {
  const [debouncedGrossRevenue, setDebouncedGrossRevenue] = useState(grossRevenue)

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedGrossRevenue(grossRevenue), DEBOUNCE_MS)
    return () => clearTimeout(timeout)
  }, [grossRevenue])

  return useQuery({
    queryKey: ['tax-estimates', 'suggested-rate', regime, debouncedGrossRevenue],
    queryFn: () => taxEstimatesApi.suggestedRate(regime as TaxRegime, debouncedGrossRevenue),
    enabled: regime !== '' && debouncedGrossRevenue > 0,
  })
}
