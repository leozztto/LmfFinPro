import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { netWorthApi } from '../api/netWorthApi'
import type { DebtBalanceInput, DebtCreateInput, DebtUpdateInput } from '../types'

export const NET_WORTH_QUERY_KEY = ['net-worth'] as const

export function useNetWorth(months: number) {
  return useQuery({
    queryKey: [...NET_WORTH_QUERY_KEY, months],
    queryFn: () => netWorthApi.get(months),
  })
}

export function useDebtBalances(debtId: number) {
  return useQuery({
    queryKey: [...NET_WORTH_QUERY_KEY, 'debts', debtId, 'balances'],
    queryFn: () => netWorthApi.listDebtBalances(debtId),
  })
}

/** Toda alteração de dívida muda o patrimônio, que é recalculado no backend. */
function useNetWorthMutation<TVariables>(mutationFn: (variables: TVariables) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: NET_WORTH_QUERY_KEY }),
  })
}

export function useCreateDebt() {
  return useNetWorthMutation((input: DebtCreateInput) => netWorthApi.createDebt(input))
}

export function useUpdateDebt() {
  return useNetWorthMutation(({ id, input }: { id: number; input: DebtUpdateInput }) =>
    netWorthApi.updateDebt(id, input),
  )
}

export function useDeleteDebt() {
  return useNetWorthMutation((id: number) => netWorthApi.removeDebt(id))
}

export function useSaveDebtBalance() {
  return useNetWorthMutation(({ debtId, input }: { debtId: number; input: DebtBalanceInput }) =>
    netWorthApi.saveDebtBalance(debtId, input),
  )
}

export function useDeleteDebtBalance() {
  return useNetWorthMutation(({ debtId, balanceId }: { debtId: number; balanceId: number }) =>
    netWorthApi.removeDebtBalance(debtId, balanceId),
  )
}
