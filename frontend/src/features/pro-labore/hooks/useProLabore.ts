import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { proLaboreApi } from '../api/proLaboreApi'

export const PRO_LABORE_QUERY_KEY = ['pro-labore'] as const

export function useProLabore() {
  return useQuery({ queryKey: PRO_LABORE_QUERY_KEY, queryFn: proLaboreApi.summary })
}

/** O backend devolve o resumo já recalculado com o novo colchão de caixa. */
export function useUpdateProLaboreSettings() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: proLaboreApi.updateSettings,
    onSuccess: (summary) => queryClient.setQueryData(PRO_LABORE_QUERY_KEY, summary),
  })
}
