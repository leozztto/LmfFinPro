import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { accountsApi } from '../api/accountsApi'
import type { AccountValuationInput } from '../types'
import { ACCOUNTS_QUERY_KEY } from './useAccounts'

export function useAccountValuations(accountId: number) {
  return useQuery({
    queryKey: [...ACCOUNTS_QUERY_KEY, accountId, 'valuations'],
    queryFn: () => accountsApi.listValuations(accountId),
  })
}

/**
 * O valor de mercado muda o saldo da conta, o patrimônio e o saldo consolidado do Dashboard, todos
 * recalculados no backend.
 */
function useValuationMutation<TVariables>(mutationFn: (variables: TVariables) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: ['net-worth'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useSaveAccountValuation() {
  return useValuationMutation(({ accountId, input }: { accountId: number; input: AccountValuationInput }) =>
    accountsApi.saveValuation(accountId, input),
  )
}

export function useDeleteAccountValuation() {
  return useValuationMutation(({ accountId, valuationId }: { accountId: number; valuationId: number }) =>
    accountsApi.removeValuation(accountId, valuationId),
  )
}
